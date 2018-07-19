package com.indago.tr2d.ui.model;

import com.indago.io.ProjectFile;
import com.indago.io.ProjectFolder;
import com.indago.tr2d.Tr2dContext;
import com.indago.tr2d.Tr2dLog;
import com.indago.tr2d.io.projectfolder.Tr2dProjectFolder;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.hash.TIntHashSet;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.img.Img;
import net.imglib2.img.ImgView;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.IntArray;
import net.imglib2.labkit.labeling.Labeling;
import net.imglib2.labkit.labeling.LabelingSerializer;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelingMapping;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import net.imglib2.view.composite.GenericComposite;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Tr2dSegmentationEditorModel implements AutoCloseable {

	private final Tr2dModel model;

	private final ProjectFolder projectFolder;

	private final String USE_MANUAL_SEGMENTATION_FILE =
			"use_manual_segmentation_tag";

	private final String LABELING_FILE = "labeling.tif";

	private ImgLabeling< String, ? > manualSegmentation = null;

	private boolean useManualSegmentation = false;

	public Tr2dSegmentationEditorModel(Tr2dModel model) {
		this.model = model;
		try {
			this.projectFolder = model.getProjectFolder()
					.getFolder(Tr2dProjectFolder.SEGMENTATION_FOLDER)
					.addFolder("manual");
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		this.useManualSegmentation = projectFolder.addFile(
				USE_MANUAL_SEGMENTATION_FILE).exists();
		tryOpenLabeling();
	}

	private void tryOpenLabeling() {
		ProjectFile labelingFile = projectFolder.addFile(LABELING_FILE);
		if(!labelingFile.exists())
			return;
		try {
			this.manualSegmentation = new LabelingSerializer(Tr2dContext.ops.context())
					.openImgLabelingFromTiff(
							labelingFile.getAbsolutePath()
			);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public List< RandomAccessibleInterval< IntType > > getSumImages() {
		if( useManualSegmentation() ) {
			ImgLabeling< String, ? > segmentations = asLabeling();
			saveSettings();
			return toListOfBitmaps(segmentations);
		} else
			return model.getSegmentationModel().getSumImages();
	}

	private void saveSettings() {
		if(manualSegmentation != null)
			saveLabeling(manualSegmentation);
		saveUseManualSegmentation();
	}

	private void saveLabeling(ImgLabeling< String, ? > segmentations) {
		try {
			new LabelingSerializer(Tr2dContext.ops.context()).save(
					new Labeling(segmentations),
							projectFolder.addFile(LABELING_FILE).getAbsolutePath()
			);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	static List<RandomAccessibleInterval<IntType>> toListOfBitmaps(ImgLabeling< String, ? > segmentations) {
		List<String> labels = new ArrayList<>( segmentations.getMapping().getLabels() );
		labels.sort(String::compareTo);
		return labels.stream().map(label -> toBitmap(segmentations, label)).collect(
				Collectors.toList());
	}

	static RandomAccessibleInterval<IntType> toBitmap(
			ImgLabeling< String, ? > segmentations, String label)
	{
		return Converters.convert((RandomAccessibleInterval<LabelingType<String> >) segmentations,
				(in, out) -> out.set(in.contains(label) ? 1 : 0), new IntType());
	}

	public void setUseManualSegmentation(boolean value) {
		useManualSegmentation = value;
	}

	private void saveUseManualSegmentation() {
		File file = projectFolder.addFile(USE_MANUAL_SEGMENTATION_FILE).getFile();
		if(useManualSegmentation) {
			if(!file.exists())
				try {
					file.createNewFile();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
		}
		else
			if(file.exists())
				file.delete();
	}

	public boolean useManualSegmentation() {
		return useManualSegmentation;
	}

	public ImgLabeling< String, ? > asLabeling()
	{
		if(manualSegmentation == null)
			return fetch();
		return manualSegmentation;
	}

	public ImgLabeling< String, ? > fetch() {
		List<RandomAccessibleInterval<IntType>>
				images = model.getSegmentationModel().getSumImages();
		List<Labeling > labelings = new ArrayList<>();
		for(int i = 0; i < images.size(); i++)
			labelings.add(toLabeling("Segmentation " + (i + 1) + " ",
					images.get(i)));
		manualSegmentation = joinLabelings(labelings);
		return manualSegmentation;
	}

	private ImgLabeling< String, IntType > joinLabelings(
			List< ? extends RandomAccessibleInterval< Set<String> > > labelings)
	{
		RandomAccessibleInterval< Set< String > > stack =
				Views.stack(labelings);
		RandomAccessibleInterval< ? extends GenericComposite< Set< String > > >
				collapsed = Views.collapse(stack);
		ArrayImg< IntType, IntArray > result =
				ArrayImgs.ints(Intervals.dimensionsAsLongArray(collapsed));
		ImgLabeling<String, IntType> labeling = new ImgLabeling<>(result);
		LoopBuilder.setImages(collapsed, labeling).forEachPixel(
				(c, l) -> {
					for(int i = 0; i < labelings.size(); i++)
						l.addAll(c.get(i));
				}
		);
		return labeling;
	}

	static Labeling toLabeling(String prefix,
			RandomAccessibleInterval< IntType > image) {
		TIntList sortedValues = getSortedValues(image);
		int maxValue = sortedValues.size() - 1;
		List< Set< String > > labelSets = labelSets(prefix, sortedValues);
		Img< IntType > intTypes = ImgView.wrap(image, null);
		ImgLabeling<String, IntType> imgLabeling = new ImgLabeling<>(intTypes);
		new LabelingMapping.SerialisationAccess<String>(imgLabeling.getMapping()) {
			public void run() {
				setLabelSets(labelSets);
			}
		}.run();
		return new Labeling(labels(prefix, maxValue), imgLabeling);
	}

	private static TIntList getSortedValues(
			RandomAccessibleInterval< IntType > image)
	{
		TIntHashSet values = new TIntHashSet();
		for(IntType value : Views.iterable(image))
			values.add(value.getInteger());
		TIntList sortedValues = new TIntArrayList(values);
		sortedValues.sort();
		return sortedValues;
	}

	private static List<Set<String>> labelSets(String prefix, TIntList sortedValues) {
		int maxValue = sortedValues.get(sortedValues.size() - 1);
		List<Set<String>> result = new ArrayList<>(maxValue + 1);
		result.add(Collections.emptySet());
		for (int value = 1; value <= maxValue; value++)
			result.add(Collections.singleton("__DUMMY__:" + value));
		for (int i = 0; i < sortedValues.size(); i++) {
			int value = sortedValues.get(i);
			result.set(value, labelSet(prefix, i));
		}
		return result;
	}

	private static Set<String> labelSet(String prefix, int index) {
		return IntStream.rangeClosed(1, index).mapToObj(i -> labelTitle(prefix, i)).collect(Collectors.toSet());
	}

	private static List<String> labels(String prefix, int maxValue) {
		return IntStream.rangeClosed(1, maxValue).mapToObj(
				x -> labelTitle(prefix, x)).collect(Collectors.toList());
	}

	private static String labelTitle(String prefix, int x) {
		return prefix + "Level " + x;
	}

	static int max(RandomAccessibleInterval<IntType> img) {
		IntType result = Util.getTypeFromInterval(img).copy();
		Views.iterable(img).forEach(pixel -> { if(pixel.getInteger() > result.getInteger()) result.set(pixel); } );
		return result.getInteger();
	}

	public Tr2dModel getModel() {
		return model;
	}

	@Override
	public void close() {
		try {
			saveSettings();
		}
		catch(Exception e) {
			Tr2dLog.log.warn("Exception while saving manual segmentation: ", e);
		}
	}
}
