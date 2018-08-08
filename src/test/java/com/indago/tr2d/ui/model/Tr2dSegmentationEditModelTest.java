package com.indago.tr2d.ui.model;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.labkit.labeling.Labeling;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.view.Views;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class Tr2dSegmentationEditModelTest {

	@Test
	public void test() {
		Img< IntType > img = ArrayImgs.ints(new int[] { 0, 2, 4, 2 }, 4);
		int result = Tr2dSegmentationEditorModel.max(img);
		assertEquals(4, result);
	}

	@Test
	public void testToBitmap() {
		String[][] pixels = {{"a"}, {"b"}};
		ImgLabeling<String, ?> labeling = initLabeling(pixels, 2);
		RandomAccessibleInterval< IntType > result =
				Tr2dSegmentationEditorModel.toBitmap(labeling, "a");
		// TODO: images equal ArrayImgs.ints(new int[]{1,0}, 2) and result
		Iterator< IntType > resultIterator = Views.iterable(result).iterator();
		assertEquals(1, resultIterator.next().get());
		assertEquals(0, resultIterator.next().get());
	}

	private ImgLabeling<String,?> initLabeling(String[][] pixels, long dims) {
		ImgLabeling< String, IntType > result =
				new ImgLabeling<>(ArrayImgs.ints(dims));
		int i = 0;
		for(LabelingType< String > pixel : Views.flatIterable(result)) {
			for(String label : pixels[i++])
				pixel.add(label);
		}
		return result;
	}

	@Test
	public void testToListOfBitmaps() {
		String[][] pixels = {{"a"}, {"b"}};
		ImgLabeling<String, ?> labeling = initLabeling(pixels, 2);
		List<RandomAccessibleInterval<IntType>> result =
				Tr2dSegmentationEditorModel.toListOfBitmaps(labeling);
		// TODO: images equal ArrayImgs.ints(new int[]{1,0}, 2) and result.get(0)
		Iterator< IntType > resultIterator0 = Views.iterable(result.get(0)).iterator();
		assertEquals(1, resultIterator0.next().get());
		assertEquals(0, resultIterator0.next().get());
		// TODO: images equal ArrayImgs.ints(new int[]{0,1}, 2) and result.get(1)
		Iterator< IntType > resultIterator1 = Views.iterable(result.get(1)).iterator();
		assertEquals(0, resultIterator1.next().get());
		assertEquals(1, resultIterator1.next().get());
	}

	@Test
	public void testToListOfBitmaps255() {
		Img< IntType > input =
				ArrayImgs.ints(new int[] { 0, 255 }, 2);
		RandomAccessibleInterval< Set< String > > result = Tr2dSegmentationEditorModel.toLabeling("prefix:", input);
		Iterator< Set< String > > pixels =
				Views.flatIterable(result).iterator();
		assertEquals(Collections.emptySet(), pixels.next());
		assertEquals(Collections.singleton("prefix:Level 1"), pixels.next());
	}

	@Test
	public void testToLabeling() {
		Img< IntType > input = ArrayImgs.ints(new int[] { 400, 450, 460 }, 3);
		RandomAccessibleInterval< Set< String > > result = Tr2dSegmentationEditorModel.toLabeling("prefix:", input);
		Iterator< Set< String > > pixels =
				Views.flatIterable(result).iterator();
		assertEquals(Collections.emptySet(), pixels.next());
		assertEquals(Collections.singleton("prefix:Level 1"), pixels.next());
		assertEquals(new HashSet<>(Arrays.asList("prefix:Level 1", "prefix:Level 2")), pixels.next());
	}
}
