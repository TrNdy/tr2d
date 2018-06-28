/**
 *
 */
package com.indago.tr2d.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.indago.data.segmentation.ConflictGraph;
import com.indago.data.segmentation.LabelingBuilder;
import com.indago.data.segmentation.LabelingPlus;
import com.indago.data.segmentation.LabelingSegment;
import com.indago.data.segmentation.MinimalOverlapConflictGraph;
import com.indago.data.segmentation.XmlIoLabelingPlus;
import com.indago.data.segmentation.filteredcomponents.FilteredComponentTree;
import com.indago.data.segmentation.filteredcomponents.FilteredComponentTree.Filter;
import com.indago.data.segmentation.filteredcomponents.FilteredComponentTree.MaxGrowthPerStep;
import com.indago.io.ProjectFile;
import com.indago.io.ProjectFolder;
import com.indago.tr2d.Tr2dLog;
import com.indago.tr2d.ui.model.Tr2dSegmentationEditorModel;

import indago.ui.progress.ProgressListener;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import weka.gui.ExtensionFileFilter;

/**
 * @author jug
 */
public class LabelingTimeLapse {

	private final Tr2dSegmentationEditorModel model;

	// Parameters for FilteredComponentTrees
	private int minHypothesisSize;
	private int maxHypothesisSize;
	private final Filter maxGrowthPerStep;
	private final boolean darkToBright = false;

	private List< LabelingBuilder > frameLabelingBuilders;
	private final Map< LabelingBuilder, ConflictGraph > mapToConflictGraphs = new HashMap< >();

	private boolean processedOrLoaded;

	/**
	 *
	 * @param model
	 */
	public LabelingTimeLapse( final Tr2dSegmentationEditorModel model, final int minHypothesisSize, final int maxHypothesisSize ) {
		this.model = model;

		// this should be a parameter
		this.minHypothesisSize = minHypothesisSize;
		this.maxHypothesisSize = maxHypothesisSize;
		maxGrowthPerStep = new MaxGrowthPerStep( maxHypothesisSize );

		frameLabelingBuilders = new ArrayList<>();
		processedOrLoaded = false;
	}

	/**
	 * @param progressListeners
	 *            Progress listener
	 * @return <code>true</code>, if any sum images for processing where found,
	 *         <code>false</code> otherwise
	 */
	public boolean processFrames( final List< ProgressListener > progressListeners ) {
		try {
			final List< RandomAccessibleInterval< IntType > >
					segmentHypothesesImages = getSegmentHypothesesImages();
			if ( segmentHypothesesImages.size() == 0 ) { return false; }
			final RandomAccessibleInterval< IntType > firstSumImg = segmentHypothesesImages
					.get( 0 );

			for ( final ProgressListener progressListener : progressListeners ) {
				progressListener.resetProgress( "Computing segment hypotheses labelings...", ( int ) firstSumImg.dimension( 2 ) );
			}

			frameLabelingBuilders = new ArrayList<>();

			for ( int frameId = 0; frameId < firstSumImg.dimension( 2 ); frameId++ ) {

				final Dimensions d = new FinalDimensions( firstSumImg.dimension( 0 ), firstSumImg.dimension( 1 ) );
				final LabelingBuilder labelingBuilder = new LabelingBuilder( d );
				frameLabelingBuilders.add( labelingBuilder );

				for ( final RandomAccessibleInterval< IntType > sumimg : segmentHypothesesImages) {
					// hyperslize desired frame
					IntervalView< IntType > frame = null;
					final long[] offset = new long[ sumimg.numDimensions() ];
					offset[ offset.length - 1 ] = frameId;
					frame = Views.offset(
							Views.hyperSlice( sumimg, 2, frameId ),
							offset );
					// build component tree on frame
					final FilteredComponentTree< IntType > tree =
							FilteredComponentTree.buildComponentTree(
									frame,
									new IntType(),
									minHypothesisSize,
									maxHypothesisSize,
									maxGrowthPerStep,
									darkToBright );
					labelingBuilder.buildLabelingForest( tree );

				}
				for ( final ProgressListener progressListener : progressListeners ) {
					progressListener.hasProgressed();
				}
			}

			for ( final ProgressListener progressListener : progressListeners ) {
				progressListener.hasCompleted();
			}
			processedOrLoaded = true;

		} catch ( final IllegalAccessException e ) {
			// This happens if getSegmentHypothesesImages() is called but none are there yet...
			processedOrLoaded = false;
		}
		return processedOrLoaded;
	}

	public List< RandomAccessibleInterval< IntType > > getSegmentHypothesesImages()
			throws IllegalAccessException {
		return model.getSumImages();
	}

	public int getNumFrames() {
		return frameLabelingBuilders.size();
	}

	public List< LabelingSegment > getLabelingSegmentsForFrame( final int frameId ) {
		return frameLabelingBuilders.get( frameId ).getSegments();
	}

	/**
	 * Returns the <code>LabelingPlus</code> for the requested frame.
	 *
	 * @param frameId
	 *            integer pointing out the frame id
	 * @return the <code>LabelingPlus</code> requested, or <code>null</code> if
	 *         it does not exists.
	 */
	public LabelingPlus getLabelingPlusForFrame( final int frameId ) {
		if ( frameId < frameLabelingBuilders.size() )
			return frameLabelingBuilders.get( frameId );
		else
			return null;
	}

	public ConflictGraph< LabelingSegment > getConflictGraph( final int frameId ) {
		final LabelingBuilder key = frameLabelingBuilders.get( frameId );
		if ( !mapToConflictGraphs.containsKey( key ) ) {
			mapToConflictGraphs.put( key, new MinimalOverlapConflictGraph( frameLabelingBuilders.get( frameId ) ) );
		}
		return mapToConflictGraphs.get( key );
	}

	public void loadFromProjectFolder( final ProjectFolder folder ) {
		frameLabelingBuilders.clear();
		processedOrLoaded = false;
		for ( final ProjectFile labelingFrameFile : folder.getFiles( new ExtensionFileFilter( "xml", "XML files" ) ) ) {
			final File fLabeling = labelingFrameFile.getFile();
			if ( fLabeling.canRead() ) {
				try {
					final LabelingPlus labelingPlus = new XmlIoLabelingPlus().load( fLabeling );
					frameLabelingBuilders.add( new LabelingBuilder( labelingPlus ) );
				} catch ( final IOException e ) {
					Tr2dLog.log.error( String.format( "Labeling could not be loaded! (%s)", fLabeling.toString() ) );
//					e.printStackTrace();
				}
			}
			processedOrLoaded = true;
		}
	}

	public boolean needProcessing() {
		return !processedOrLoaded;
	}

	/**
	 * @param folder
	 *            ProjectFolder instance
	 * @param progressListeners
	 *            please do not hand <code>null</code>. Empty lists are fine
	 *            though.
	 */
	public void saveTo( final ProjectFolder folder, final List< ProgressListener > progressListeners ) {
		for ( final ProgressListener progressListener : progressListeners ) {
			progressListener.resetProgress( "Saving segment hypotheses labelings...", frameLabelingBuilders.size() );
		}

		final String fnPrefix = "labeling_frame";
		int i = 0;
		for ( final LabelingBuilder lb : frameLabelingBuilders ) {
			final String fn = String.format( "%s%04d.xml", fnPrefix, i );
			final String abspath = new File( folder.getFolder(), fn ).getAbsolutePath();
			try {
				new XmlIoLabelingPlus().save( lb, abspath );
			} catch ( final IOException e ) {
				Tr2dLog.log.error( String.format( "Could not store labeling_frame%04d.* to project folder!", i ) );
//				e.printStackTrace();
			}

			i++;
			for ( final ProgressListener progressListener : progressListeners ) {
				progressListener.hasProgressed();
			}
		}
	}

	public void setMinSegmentSize( final int minHypothesisSize ) {
		this.minHypothesisSize = minHypothesisSize;
	}

	public void setMaxSegmentSize( final int maxHypothesisSize ) {
		this.maxHypothesisSize = maxHypothesisSize;
	}

}
