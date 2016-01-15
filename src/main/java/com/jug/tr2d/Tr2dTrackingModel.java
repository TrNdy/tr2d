/**
 *
 */
package com.jug.tr2d;

import java.util.Collection;
import java.util.List;

import com.indago.fg.CostsFactory;
import com.indago.segment.LabelingBuilder;
import com.indago.segment.LabelingSegment;
import com.indago.segment.MinimalOverlapConflictGraph;
import com.indago.segment.Segment;
import com.indago.segment.fg.FactorGraphFactory;
import com.indago.segment.fg.FactorGraphPlus;
import com.indago.segment.fg.SegmentHypothesisVariable;
import com.indago.segment.filteredcomponents.FilteredComponentTree;
import com.indago.segment.filteredcomponents.FilteredComponentTree.Filter;
import com.indago.segment.filteredcomponents.FilteredComponentTree.MaxGrowthPerStep;
import com.jug.tr2d.datasets.hernan.HernanMappingCostFactory;
import com.jug.tr2d.datasets.hernan.HernanSegmentCostFactory;
import com.jug.tr2d.fg.Tr2dFactorGraphFactory;
import com.jug.tr2d.fg.Tr2dFactorGraphPlus;

import net.imglib2.Dimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Pair;
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
	private final Tr2dFactorGraphPlus fgPlus = new Tr2dFactorGraphPlus();

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

	/**
	 * This method creates the tracking FG for the entire given time-series.
	 */
	@SuppressWarnings( "unchecked" )
	public void run() {
		long t0, t1;
		List< LabelingSegment > segments;
		Collection< SegmentHypothesisVariable< Segment > > segVars;
		List< LabelingSegment > oldSegments = null;
		Collection< SegmentHypothesisVariable< Segment > > oldSegVars = null;

		// set dim
		try {
			dim = getSegmentHypothesesImage();
			System.out.println( "Input image dimensions: " + dim.toString() );
		} catch ( final IllegalAccessException e ) {
			System.err.println( "Segmentation Hypotheses could not be accessed!" );
			e.printStackTrace();
			return;
		}

		// go over frames and create and add frameFGs
		// + assignmentFGs between adjacent frames
		// ---------------------------------------------------------
		final long numFrames = dim.dimension( 2 );
		for ( long frameId = 0; frameId < numFrames; frameId++ ) {
//			final List< LabelingForest > frameLabelingForests = new ArrayList< LabelingForest >();

			// Hyperslize current frame out of complete dataset
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
//			frameLabelingForests.add( labelingBuilder.buildLabelingForest( tree ) );
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

			// =============
			// FRAME FG
			// =============
			System.out.print( "Constructing frameFG from MinimalOverlapConflictGraph... " );
			t0 = System.currentTimeMillis();
			segments = labelingBuilder.getSegments();
			final CostsFactory< Segment > segmentCosts =
					new HernanSegmentCostFactory( frameId, tr2dModel.getImgOrig() );
			final FactorGraphPlus frameFG = FactorGraphFactory
					.createFromConflictGraph( segments, conflictGraph, segmentCosts );
			segVars = ( Collection< SegmentHypothesisVariable< Segment > > ) frameFG
					.getFactorGraph()
					.getVariables();
			t1 = System.currentTimeMillis();
			System.out
					.println( String.format( "completed in %.2f seconds!", ( t1 - t0 ) / 1000. ) );

			// =============
			// TRANSITION FG
			// =============
			FactorGraphPlus transFG = null;
			if ( frameId > 0 ) {
				final CostsFactory< Pair< Segment, Segment > > mappingCosts =
						new HernanMappingCostFactory( frameId, tr2dModel.getImgOrig() );
				final CostsFactory< Pair< Segment, Pair< Segment, Segment > > > divisionCosts =
						null;
				final CostsFactory< Segment > appearanceCosts = null;
				final CostsFactory< Segment > disappearanceCosts = null;
				transFG = new Tr2dFactorGraphFactory()
						.createTransitionGraph(
								fgPlus,
								oldSegVars,
								segVars,
								mappingCosts,
								1.,
								divisionCosts,
								1.,
								appearanceCosts,
								1.,
								disappearanceCosts,
								1. );
			}

			// ADD FRAME
			if ( frameId == 0 ) {
				fgPlus.addFirstFrame( frameFG );
			} else {
				fgPlus.addFrame( transFG, frameFG );
			}

			oldSegments = segments;
			oldSegVars = segVars;
		}

		System.out.println( "Success!" );
	}

}
