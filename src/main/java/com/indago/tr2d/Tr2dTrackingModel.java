/**
 *
 */
package com.indago.tr2d;

import java.util.List;

import com.indago.fg.CostsFactory;
import com.indago.segment.LabelingSegment;
import com.indago.segment.Segment;
import com.indago.segmentation.SumImageMovieSequence;
import com.indago.tr2d.tracking.Tr2dSegmentationProblem;
import com.indago.tr2d.tracking.Tr2dTrackingProblem;
import com.indago.tracking.seg.ConflictGraph;
import com.indago.util.TicToc;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.DoubleType;

/**
 * @author jug
 */
public class Tr2dTrackingModel {

	private final Tr2dModel tr2dModel;
	private final Tr2dWekaSegmentationModel tr2dSegModel;

	private final SumImageMovieSequence sumImgMovie;;

	private final Tr2dTrackingProblem trackingProblem;
	private final CostsFactory< Segment > segmentCosts;

	private final RandomAccessibleInterval< DoubleType > imgSolution = null;



	/**
	 * @param model
	 */
	public Tr2dTrackingModel(
			final Tr2dModel model,
			final Tr2dWekaSegmentationModel modelSeg,
			final CostsFactory< Segment > segmentCosts ) {
		this.tr2dModel = model;

		this.trackingProblem = new Tr2dTrackingProblem();
		this.segmentCosts = segmentCosts;

		this.tr2dSegModel = modelSeg;
		this.sumImgMovie = new SumImageMovieSequence( tr2dSegModel );
	}

	/**
	 * This method creates the tracking FG for the entire given time-series.
	 */
	@SuppressWarnings( "unchecked" )
	public void run() {
		final TicToc tictoc = new TicToc();

		// go over frames and create and add frameFGs
		// + assignmentFGs between adjacent frames
		// ---------------------------------------------------------
		for ( int frameId = 0; frameId < sumImgMovie.getNumFrames(); frameId++ ) {
			System.out.println(
					String.format( "Working on frame %d of %d...", frameId + 1, sumImgMovie.getNumFrames() ) );

			// =============================
			// build Tr2dSegmentationProblem
			// =============================
			tictoc.tic( "Constructing Tr2dSegmentationProblem..." );
			final List< LabelingSegment > segments =
					sumImgMovie.getLabelingSegmentsForFrame( frameId );
			final ConflictGraph conflictGraph =
					sumImgMovie.getConflictGraph( frameId );
			final Tr2dSegmentationProblem segmentationProblem =
					new Tr2dSegmentationProblem( frameId, segmentCosts );
			trackingProblem.addSegmentationProblem( segmentationProblem );
			tictoc.toc( "done!" );

			// =========================
			// build Tr2dTrackingProblem
			// =========================
//			FactorGraphPlus< Segment > transFG = null;
//			if ( frameId > 0 ) {
//				System.out.print( "\tConstructing transFG... " );
//				t0 = System.currentTimeMillis();
//				final CostsFactory< Pair< Segment, Segment > > mappingCosts =
//						new HernanMappingCostFactory( frameId, tr2dModel.getImgOrig() );
//				final CostsFactory< Pair< Segment, Pair< Segment, Segment > > > divisionCosts =
//						new HernanDivisionCostFactory( frameId, tr2dModel.getImgOrig() );
//				final CostsFactory< Segment > appearanceCosts =
//						new HernanAppearanceCostFactory( frameId, tr2dModel.getImgOrig() );
//				final CostsFactory< Segment > disappearanceCosts =
//						new HernanDisappearanceCostFactory( frameId, tr2dModel.getImgOrig() );
//				transFG = new Tr2dFactorGraphFactory()
//						.createTransitionGraph(
//								fgPlus,
//								oldSegVars,
//								segVars,
//								mappingCosts,
//								HernanCostConstants.TRUNCATE_COST_THRESHOLD,
//								divisionCosts,
//								HernanCostConstants.TRUNCATE_COST_THRESHOLD,
//								appearanceCosts,
//								HernanCostConstants.TRUNCATE_COST_THRESHOLD,
//								disappearanceCosts,
//								HernanCostConstants.TRUNCATE_COST_THRESHOLD );
//				t1 = System.currentTimeMillis();
//				System.out.println(
//						String.format( "\n\t...completed in %.2f seconds!", ( t1 - t0 ) / 1000. ) );
//			}

			// ==========================
			// add to Tr2dTrackingProblem
			// ==========================
//			System.out.print( "\tAdding new FGs... " );
//			t0 = System.currentTimeMillis();
//			if ( frameId == 0 ) {
//				fgPlus.addFirstFrame( frameFG );
//			} else {
//				fgPlus.addFrame( transFG, frameFG );
//			}
//			t1 = System.currentTimeMillis();
//			System.out
//					.println(
//							String.format(
//									"\n\t...completed in %.2f seconds!",
//									( t1 - t0 ) / 1000. ) );
		}

//		System.out.println( "FG successfully built!\n" );

		// ===============================================================================================

//		System.out.println( "Constructing and solving ILP... " );
//		t0 = System.currentTimeMillis();
//		GurobiReadouts gurobiStats;
//		try {
//			gurobiStats = buildAndRunILP( fgPlus );
//		} catch ( final GRBException e ) {
//			e.printStackTrace();
//		}
//		t1 = System.currentTimeMillis();
//		System.out
//				.println( String.format( "...completed in %.2f seconds!", ( t1 - t0 ) / 1000. ) );

		// ===============================================================================================

//		System.out.println( "Visualize tracking results... " );
//		t0 = System.currentTimeMillis();
//		try {
//			imgSolution =
//					DataMover.createEmptyArrayImgLike(
//							getSegmentHypothesesImage(),
//							new DoubleType() );
//			int trackletId = 1;
//			final Set< SegmentHypothesisVariable< Segment > > seenSegVars = new HashSet< >();
//			for ( long frameId = 0; frameId < numFrames; frameId++ ) {
//				final FactorGraphPlus< Segment > firstFrameFG = fgPlus.getFrameFGs().get( 0 );
//				for ( final SegmentHypothesisVariable< Segment > segVar : firstFrameFG
//						.getSegmentVariables() ) {
//					if ( assignment.getAssignment( segVar ).get()
//							&& !seenSegVars.contains( segVar ) ) {
//						recursivelyPaintTracklet( segVar, seenSegVars, trackletId, frameId );
//						trackletId++;
//					}
//
//				}
//			}
//			ImageJFunctions.show( imgSolution );
//		} catch ( final IllegalAccessException e ) {
//			e.printStackTrace();
//		}
//
//		t1 = System.currentTimeMillis();
//		System.out
//				.println( String.format( "...completed in %.2f seconds!", ( t1 - t0 ) / 1000. ) );

	}

}
