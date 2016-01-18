/**
 *
 */
package com.indago.segmentation;

import java.util.ArrayList;
import java.util.List;

import com.indago.segment.LabelingBuilder;
import com.indago.segment.LabelingForest;
import com.indago.segment.LabelingSegment;
import com.indago.segment.MinimalOverlapConflictGraph;
import com.indago.segment.filteredcomponents.FilteredComponentTree;
import com.indago.segment.filteredcomponents.FilteredComponentTree.Filter;
import com.indago.segment.filteredcomponents.FilteredComponentTree.MaxGrowthPerStep;
import com.indago.tr2d.Tr2dWekaSegmentationModel;
import com.indago.tracking.seg.ConflictGraph;

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

	private List< LabelingForest > frameLabelingForests;
	private List< List< LabelingSegment > > frameLabelingSegments;
	private List< ConflictGraph > frameConflictGraphs;

	/**
	 * @param tr2dSegModel2
	 */
	public SumImageMovieSequence( final Tr2dWekaSegmentationModel tr2dSegModel ) {
		this.tr2dSegModel = tr2dSegModel;
		this.frameLabelingForests = new ArrayList< >();
		this.frameLabelingSegments = new ArrayList< >();
		this.frameConflictGraphs = new ArrayList< >();

		// set dim
		try {
			dim = getSegmentHypothesesImage();
			System.out.println( "Input image dimensions: " + dim.toString() );
		} catch ( final IllegalAccessException e ) {
			System.err.println( "Segmentation Hypotheses could not be accessed!" );
			e.printStackTrace();
			return;
		}

		processFrames();
	}

	/**
	 *
	 */
	private void processFrames() {

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
	public ConflictGraph getConflictGraph( final int frameId ) {
		return frameConflictGraphs.get( frameId );
	}

}
