/**
 *
 */
package com.indago.data.segmentation;

import java.util.ArrayList;
import java.util.List;

import com.indago.data.segmentation.filteredcomponents.FilteredComponentTree;
import com.indago.data.segmentation.filteredcomponents.FilteredComponentTree.Filter;
import com.indago.data.segmentation.filteredcomponents.FilteredComponentTree.MaxGrowthPerStep;
import com.indago.tr2d.ui.model.Tr2dSegmentationCollectionModel;

import net.imglib2.Dimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * @author jug
 */
public class LabelingTimeLapse {

	private final Tr2dSegmentationCollectionModel model;

	// Parameters for FilteredComponentTree of SumImage(s)
	private Dimensions dim;
	private final int minComponentSize = 10;
	private final int maxComponentSize = 10000;
	private final Filter maxGrowthPerStep = new MaxGrowthPerStep( 1 );
	private final boolean darkToBright = false;

	private final List< LabelingForest > frameLabelingForests;
	private final List< List< LabelingSegment > > frameLabelingSegments;
	private final List< ConflictGraph< LabelingSegment > > frameConflictGraphs;

	final LabelingBuilder labelingBuilder;

	/**
	 * @param tr2dSegModel2
	 */
	public LabelingTimeLapse( final Tr2dSegmentationCollectionModel model ) {
		this.model = model;
		this.frameLabelingForests = new ArrayList< >();
		this.frameLabelingSegments = new ArrayList< >();
		this.frameConflictGraphs = new ArrayList< >();

		labelingBuilder = new LabelingBuilder( model.getSumImages().get( 0 ) );
	}

	/**
	 *
	 */
	private void setDim() throws IllegalAccessException {
		dim = getSegmentHypothesesImages().get( 0 );
	}

	/**
	 *
	 */
	public void processFrames() throws IllegalAccessException {
		setDim();

		for ( long frameId = 0; frameId < dim.dimension( 2 ); frameId++ ) {
			// Hyperslize current frame out of complete dataset
			IntervalView< IntType > hsFrame = null;
			try {
				final long[] offset = new long[ getSegmentHypothesesImages().get( 0 ).numDimensions() ];
				offset[ offset.length - 1 ] = frameId;
				hsFrame = Views.offset(
						Views.hyperSlice( getSegmentHypothesesImages().get( 0 ), 2, frameId ),
						offset );
			} catch ( final IllegalAccessException e ) {
				System.err.println( "\tSegmentation Hypotheses could not be accessed!" );
				e.printStackTrace();
				return;
			}

			final FilteredComponentTree< IntType > tree =
					FilteredComponentTree.buildComponentTree(
							hsFrame,
							new IntType(),
							minComponentSize,
							maxComponentSize,
							maxGrowthPerStep,
							darkToBright );
			final LabelingBuilder labelingBuilder = new LabelingBuilder( dim );
			frameLabelingForests.add( labelingBuilder.buildLabelingForest( tree ) );
			frameLabelingSegments.add( labelingBuilder.getSegments() );

			final MinimalOverlapConflictGraph conflictGraph =
					new MinimalOverlapConflictGraph( labelingBuilder );
			frameConflictGraphs.add( conflictGraph );
		}
	}

	/**
	 * @return
	 * @throws IllegalAccessException
	 */
	public List< RandomAccessibleInterval< IntType > > getSegmentHypothesesImages()
			throws IllegalAccessException {
		return model.getSumImages();
	}

	/**
	 * @return
	 */
	public long getNumFrames() {
		return dim.dimension( 2 );
	}

	/**
	 * @param frameId
	 * @return
	 */
	public List< LabelingSegment > getLabelingSegmentsForFrame( final int frameId ) {
		return frameLabelingSegments.get( frameId );
	}

	/**
	 * @param frameId
	 * @return
	 */
	public ConflictGraph< LabelingSegment > getConflictGraph( final int frameId ) {
		return frameConflictGraphs.get( frameId );
	}

}
