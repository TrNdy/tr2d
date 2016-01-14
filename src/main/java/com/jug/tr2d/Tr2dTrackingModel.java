/**
 *
 */
package com.jug.tr2d;

import java.util.ArrayList;
import java.util.List;

import com.indago.fg.FactorGraph;
import com.indago.segment.LabelingBuilder;
import com.indago.segment.LabelingForest;
import com.indago.segment.LabelingSegment;
import com.indago.segment.MinimalOverlapConflictGraph;
import com.indago.segment.RandomSegmentCosts;
import com.indago.segment.fg.FactorGraphFactory;
import com.indago.segment.filteredcomponents.FilteredComponentTree;
import com.indago.segment.filteredcomponents.FilteredComponentTree.Filter;
import com.indago.segment.filteredcomponents.FilteredComponentTree.MaxGrowthPerStep;
import com.jug.tr2d.fg.Tr2dFactorGraphPlus;

import net.imglib2.Dimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * @author jug
 */
public class Tr2dTrackingModel {

	private final Tr2dModel tr2dModel;
	private final Tr2dWekaSegmentationModel tr2dSegModel;

	// Parameters for FilteredComponentTree of SumImage(s)
	private Dimensions dim;
	private final int minComponentSize = 10;
	private final int maxComponentSize = 10000;
	private final Filter maxGrowthPerStep;
	private final boolean darkToBright = true;

	// factor graph (plus association data structures)
	private Tr2dFactorGraphPlus fgPlus;

	/**
	 * @param model
	 */
	public Tr2dTrackingModel( final Tr2dModel model, final Tr2dWekaSegmentationModel modelSeg ) {
		this.tr2dModel = model;
		this.tr2dSegModel = modelSeg;

		maxGrowthPerStep = new MaxGrowthPerStep( 1 );
	}

	/**
	 * @return
	 * @throws IllegalAccessException
	 */
	public RandomAccessibleInterval< DoubleType > getSegmentHypothesesImage()
			throws IllegalAccessException {
		return tr2dSegModel.getSegmentHypotheses();
	}

	public void run() {
		long t0, t1;

		try {
			dim = getSegmentHypothesesImage();
			System.out.println( "Input image dimensions: " + dim.toString() );
		} catch ( final IllegalAccessException e ) {
			System.err.println( "Segmentation Hypotheses could not be accessed!" );
			e.printStackTrace();
			return;
		}

		final long numFrames = dim.dimension( 2 );
		for ( long frameId = 0; frameId < numFrames; frameId++ ) {
			final List< LabelingForest > frameLabelingForests = new ArrayList< LabelingForest >();

			IntervalView< DoubleType > hsFrame = null;
			try {
				hsFrame = Views.hyperSlice( getSegmentHypothesesImage(), 2, frameId );
			} catch ( final IllegalAccessException e ) {
				System.err.println( "Segmentation Hypotheses could not be accessed!" );
				e.printStackTrace();
				return;
			}

			System.out.print( "Building FilteredComponentTree and LabelingForest... " );
			t0 = System.currentTimeMillis();
			FilteredComponentTree< DoubleType > tree = null;
			tree =
					FilteredComponentTree.buildComponentTree(
							hsFrame,
							new DoubleType(),
							minComponentSize,
							maxComponentSize,
							maxGrowthPerStep,
							darkToBright );
			final LabelingBuilder labelingBuilder = new LabelingBuilder( dim );
			frameLabelingForests.add( labelingBuilder.buildLabelingForest( tree ) );
			t1 = System.currentTimeMillis();
			System.out
					.println( String.format( "completed in %.2f seconds!", ( t1 - t0 ) / 1000. ) );

			System.out.print( "Constructing MinimalOverlapConflictGraph... " );
			t0 = System.currentTimeMillis();
			final MinimalOverlapConflictGraph conflictGraph =
					new MinimalOverlapConflictGraph( labelingBuilder );
			conflictGraph.getConflictGraphCliques();
			t1 = System.currentTimeMillis();
			System.out
					.println( String.format( "completed in %.2f seconds!", ( t1 - t0 ) / 1000. ) );

			System.out.print( "Constructing frameFG from MinimalOverlapConflictGraph... " );
			t0 = System.currentTimeMillis();
			final ArrayList< LabelingSegment > segments = labelingBuilder.getSegments();
			final RandomSegmentCosts costs = new RandomSegmentCosts( segments, 4711 );
			final FactorGraph frameFG = FactorGraphFactory
					.createFromConflictGraph( segments, conflictGraph, costs )
					.getFactorGraph();
			t1 = System.currentTimeMillis();
			System.out
					.println( String.format( "completed in %.2f seconds!", ( t1 - t0 ) / 1000. ) );

			// If frame is NOT the first in line generate also transition FG!
			final FactorGraph transFG = null;
			if ( frameId > 0 ) {
				//TODO add missing code
			}

			// ADD FRAME
			if ( frameId == 0 ) {
				fgPlus = new Tr2dFactorGraphPlus( frameFG, frameLabelingForests );
			} else {
				fgPlus.addFrame( transFG, frameFG, frameLabelingForests );
			}

		}

		System.out.println( "Success!" );
	}

}
