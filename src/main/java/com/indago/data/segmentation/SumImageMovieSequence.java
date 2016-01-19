/**
 *
 */
package com.indago.data.segmentation;

import java.util.ArrayList;
import java.util.List;

import com.indago.data.segmentation.ConflictGraph;
import com.indago.data.segmentation.LabelingBuilder;
import com.indago.data.segmentation.LabelingForest;
import com.indago.data.segmentation.LabelingSegment;
import com.indago.data.segmentation.MinimalOverlapConflictGraph;
import com.indago.data.segmentation.filteredcomponents.FilteredComponentTree;
import com.indago.data.segmentation.filteredcomponents.FilteredComponentTree.Filter;
import com.indago.data.segmentation.filteredcomponents.FilteredComponentTree.MaxGrowthPerStep;
import com.indago.tr2d.ui.model.Tr2dWekaSegmentationModel;

import net.imglib2.Dimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * @author jug
 */
public class SumImageMovieSequence {

	private final Tr2dWekaSegmentationModel tr2dSegModel;

	// Parameters for FilteredComponentTree of SumImage(s)
	private Dimensions dim;
	private final int minComponentSize = 10;
	private final int maxComponentSize = 10000;
	private final Filter maxGrowthPerStep = new MaxGrowthPerStep( 1 );
	private final boolean darkToBright = false;

	private final List< LabelingForest > frameLabelingForests;
	private final List< List< LabelingSegment > > frameLabelingSegments;
	private final List< ConflictGraph< LabelingSegment > > frameConflictGraphs;

	/**
	 * @param tr2dSegModel2
	 */
	public SumImageMovieSequence( final Tr2dWekaSegmentationModel tr2dSegModel ) {
		this.tr2dSegModel = tr2dSegModel;
		this.frameLabelingForests = new ArrayList< >();
		this.frameLabelingSegments = new ArrayList< >();
		this.frameConflictGraphs = new ArrayList< >();
	}

	/**
	 *
	 */
	private void setDim() throws IllegalAccessException {
			dim = getSegmentHypothesesImage();
//			System.out.println( "Input image dimensions: " + dim.toString() );
	}

	/**
	 *
	 */
	public void processFrames() throws IllegalAccessException {
		setDim();

		for ( long frameId = 0; frameId < dim.dimension( 2 ); frameId++ ) {
			// Hyperslize current frame out of complete dataset
			IntervalView< DoubleType > hsFrame = null;
			try {
				final long[] offset = new long[ getSegmentHypothesesImage().numDimensions() ];
				offset[ offset.length - 1 ] = frameId;
				hsFrame = Views.offset(
						Views.hyperSlice( getSegmentHypothesesImage(), 2, frameId ),
						offset );
			} catch ( final IllegalAccessException e ) {
				System.err.println( "\tSegmentation Hypotheses could not be accessed!" );
				e.printStackTrace();
				return;
			}

			final FilteredComponentTree< DoubleType > tree =
					FilteredComponentTree.buildComponentTree(
							hsFrame,
							new DoubleType(),
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
	public RandomAccessibleInterval< DoubleType > getSegmentHypothesesImage()
			throws IllegalAccessException {
		return tr2dSegModel.getSegmentHypotheses();
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
