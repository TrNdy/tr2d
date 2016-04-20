/**
 *
 */
package com.indago.app.hernan.models;

import java.util.List;
import java.util.Map;

import com.indago.app.hernan.costs.HernanCostConstants;
import com.indago.data.segmentation.ConflictGraph;
import com.indago.data.segmentation.LabelingSegment;
import com.indago.data.segmentation.SumImageMovieSequence;
import com.indago.fg.Assignment;
import com.indago.fg.AssignmentMapper;
import com.indago.fg.FactorGraphFactory;
import com.indago.fg.MappedFactorGraph;
import com.indago.fg.UnaryCostConstraintGraph;
import com.indago.fg.Variable;
import com.indago.ilp.SolveGurobi;
import com.indago.models.IndicatorNode;
import com.indago.models.assignments.AppearanceHypothesis;
import com.indago.models.assignments.DisappearanceHypothesis;
import com.indago.models.assignments.DivisionHypothesis;
import com.indago.models.assignments.MovementHypothesis;
import com.indago.models.segments.SegmentNode;
import com.indago.old_fg.CostsFactory;
import com.indago.tr2d.models.Tr2dSegmentationProblem;
import com.indago.tr2d.models.Tr2dTrackingProblem;
import com.indago.tr2d.ui.model.Tr2dModel;
import com.indago.tr2d.ui.model.Tr2dWekaSegmentationModel;
import com.indago.util.DataMover;
import com.indago.util.TicToc;

import gurobi.GRBException;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.roi.IterableRegion;
import net.imglib2.roi.Regions;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Pair;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * @author jug
 */
public class Tr2dTrackingModelHernan {

	private final Tr2dModel tr2dModel;
	private final Tr2dWekaSegmentationModel tr2dSegModel;
	private final Tr2dTrackingProblem tr2dTraProblem;

	private final SumImageMovieSequence sumImgMovie;

	private final CostsFactory< LabelingSegment > segmentCosts;
	private final CostsFactory< LabelingSegment > appearanceCosts;
	private final CostsFactory< Pair< LabelingSegment, LabelingSegment > > moveCosts;
	private final CostsFactory< Pair< LabelingSegment, Pair< LabelingSegment, LabelingSegment > > > divisionCosts;
	private final CostsFactory< LabelingSegment > disappearanceCosts;

	private RandomAccessibleInterval< DoubleType > imgSolution = null;

	private MappedFactorGraph mfg;
	private Assignment< Variable > fgSolution;
	private Assignment< IndicatorNode > problemSolution;

	/**
	 * @param model
	 */
	public Tr2dTrackingModelHernan(
			final Tr2dModel model,
			final Tr2dWekaSegmentationModel modelSeg,
			final CostsFactory< LabelingSegment > segmentCosts,
			final CostsFactory< LabelingSegment > appearanceCosts,
			final CostsFactory< Pair< LabelingSegment, LabelingSegment > > movementCosts,
			final CostsFactory< Pair< LabelingSegment, Pair< LabelingSegment, LabelingSegment > > > divisionCosts,
			final CostsFactory< LabelingSegment > disappearanceCosts ) {
		this.tr2dModel = model;

		this.appearanceCosts = appearanceCosts;
		this.moveCosts = movementCosts;
		this.divisionCosts = divisionCosts;
		this.disappearanceCosts = disappearanceCosts;
		this.tr2dTraProblem =
				new Tr2dTrackingProblem( appearanceCosts,
						movementCosts, HernanCostConstants.TRUNCATE_COST_THRESHOLD,
						divisionCosts, HernanCostConstants.TRUNCATE_COST_THRESHOLD,
						disappearanceCosts );
		this.segmentCosts = segmentCosts;

		this.tr2dSegModel = modelSeg;
		this.sumImgMovie = new SumImageMovieSequence( tr2dSegModel );
	}

	/**
	 *
	 */
	@SuppressWarnings( "unchecked" )
	public void run() {
		processSegmentationInputs();
		buildTrackingModel();
		buildFactorGraph();
		solveFactorGraph();
		drawSolution();
	}

	/**
	 *
	 */
	public void processSegmentationInputs() {
		try {
			sumImgMovie.processFrames();
		} catch ( final IllegalAccessException e ) {
			System.err.println(
					"Segmentation Hypotheses could not be accessed!\nYou must create a segmentation prior to starting the tracking!" );
			e.printStackTrace();
			return;
		}
	}

	/**
	 *
	 */
	public void buildTrackingModel() {
		final TicToc tictoc = new TicToc();
		for ( int frameId = 0; frameId < sumImgMovie.getNumFrames(); frameId++ ) {
			System.out.println(
					String.format( "Working on frame %d of %d...", frameId + 1, sumImgMovie.getNumFrames() ) );

			// =============================
			// build Tr2dSegmentationProblem
			// =============================
			tictoc.tic( "Constructing Tr2dSegmentationProblem..." );
			final List< LabelingSegment > segments =
					sumImgMovie.getLabelingSegmentsForFrame( frameId );
			final ConflictGraph< LabelingSegment > conflictGraph =
					sumImgMovie.getConflictGraph( frameId );
			final Tr2dSegmentationProblem segmentationProblem =
					new Tr2dSegmentationProblem( frameId, segments, segmentCosts, conflictGraph );
			tictoc.toc( "done!" );

			// =============================
			// add it to Tr2dTrackingProblem
			// =============================
			tictoc.tic( "Connect it to Tr2dTrackingProblem..." );
			tr2dTraProblem.addSegmentationProblem( segmentationProblem );
			tictoc.toc( "done!" );
		}
		tr2dTraProblem.addDummyDisappearanceToFinishModel();

		System.out.println( "Tracking graph was built sucessfully!" );
	}

	/**
	 *
	 */
	public void buildFactorGraph() {
		final TicToc tictoc = new TicToc();
		tictoc.tic( "Constructing FactorGraph for created Tr2dTrackingProblem..." );
		mfg = FactorGraphFactory.createFactorGraph( tr2dTraProblem );
		tictoc.toc( "done!" );
	}

	/**
	 *
	 */
	private void solveFactorGraph() {
		final UnaryCostConstraintGraph fg = mfg.getFg();
		final AssignmentMapper< Variable, IndicatorNode > assMapper = mfg.getAssmntMapper();
		final Map< IndicatorNode, Variable > varMapper = mfg.getVarmap();

		fgSolution = null;
		try {
			fgSolution = SolveGurobi.staticSolve( fg );
			problemSolution = assMapper.map( fgSolution );
		} catch ( final GRBException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 *
	 */
	private void drawSolution() {
		final UnaryCostConstraintGraph fg = mfg.getFg();
		final AssignmentMapper< Variable, IndicatorNode > assMapper = mfg.getAssmntMapper();
		final Map< IndicatorNode, Variable > varMapper = mfg.getVarmap();

		try {
			this.imgSolution = DataMover.createEmptyArrayImgLike( tr2dSegModel.getClassification(), new DoubleType() );

//			int time = 0;
//			for ( final Tr2dSegmentationModel segProblem : tr2dTraModel.getTimepoints() ) {
//				final IntervalView< DoubleType > slice = Views.hyperSlice( imgSolution, 2, time );
//
//				for ( final SegmentVar segVar : segProblem.getSegments() ) {
//					if ( problemSolution.getAssignment( segVar ) == 1 ) {
//						final IterableRegion< ? > region = segVar.getSegment().getRegion();
//						Regions.sample( region, slice ).forEach( t -> t.set( t.get() + 1 ) );
//					}
//				}
//				time++;
//			}

			int time = 0;
			int curColorId = 1;
			for ( final Tr2dSegmentationProblem segProblem : tr2dTraProblem.getTimepoints() ) {
				for ( final SegmentNode segVar : segProblem.getSegments() ) {
					System.out.print(
							"time=" + time + " - #app/#disapp = " + segVar.getInAssignments().getAppearances().size() + "/" + segVar
									.getOutAssignments()
									.getDisappearances()
									.size() + "\t" );
					for ( final AppearanceHypothesis app : segVar.getInAssignments().getAppearances() ) {
						System.out.print( "" + problemSolution.getAssignment( app ) );
						if ( problemSolution.getAssignment( app ) == 1 ) { // || time == 0
							drawLineageWithId( time, segVar, 10 + curColorId );
							curColorId++;
						}
					}
					for ( final DisappearanceHypothesis disapp : segVar.getOutAssignments().getDisappearances() ) {
						System.out.println( "/" + problemSolution.getAssignment( disapp ) );
					}
				}
				time++;
			}

			ImageJFunctions.show( imgSolution, "Solution" );

		} catch ( final IllegalAccessException e ) {
			e.printStackTrace();
		}
	}

	/**
	 * @param segVar
	 * @param curColorId
	 */
	private void drawLineageWithId( final int time, final SegmentNode segVar, final int curColorId ) {
		final IntervalView< DoubleType > slice = Views.hyperSlice( imgSolution, 2, time );

		if ( problemSolution.getAssignment( segVar ) == 1 ) {
			final int color = curColorId;

//			for ( final DisappearanceHypothesis disapp : segVar.getOutAssignments().getDisappearances() ) {
//				if ( problemSolution.getAssignment( disapp ) == 1 ) {
//					color = 5;
//				}
//			}

			final IterableRegion< ? > region = segVar.getSegment().getRegion();
			final int c = color;
			Regions.sample( region, slice ).forEach( t -> t.set( c ) );

			for ( final MovementHypothesis move : segVar.getOutAssignments().getMoves() ) {
				if ( problemSolution.getAssignment( move ) == 1 ) {
					drawLineageWithId( time + 1, move.getDest(), curColorId );
				}
			}
			for ( final DivisionHypothesis div : segVar.getOutAssignments().getDivisions() ) {
				if ( problemSolution.getAssignment( div ) == 1 ) {
					drawLineageWithId( time + 1, div.getDest1(), curColorId );
					drawLineageWithId( time + 1, div.getDest2(), curColorId );
				}
			}
		}
	}

	/**
	 * @return the imgSolution
	 */
	public RandomAccessibleInterval< DoubleType > getImgSolution() {
		return imgSolution;
	}
}
