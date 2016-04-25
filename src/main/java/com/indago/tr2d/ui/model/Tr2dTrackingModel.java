/**
 *
 */
package com.indago.tr2d.ui.model;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.indago.app.hernan.costs.HernanCostConstants;
import com.indago.data.segmentation.ConflictGraph;
import com.indago.data.segmentation.LabelingSegment;
import com.indago.data.segmentation.LabelingTimeLapse;
import com.indago.fg.Assignment;
import com.indago.fg.AssignmentMapper;
import com.indago.fg.FactorGraphFactory;
import com.indago.fg.MappedFactorGraph;
import com.indago.fg.UnaryCostConstraintGraph;
import com.indago.fg.Variable;
import com.indago.ilp.SolveGurobi;
import com.indago.io.DataMover;
import com.indago.io.IntTypeImgLoader;
import com.indago.io.projectfolder.ProjectFolder;
import com.indago.old_fg.CostsFactory;
import com.indago.pg.IndicatorNode;
import com.indago.pg.assignments.AppearanceHypothesis;
import com.indago.pg.assignments.DivisionHypothesis;
import com.indago.pg.assignments.MovementHypothesis;
import com.indago.pg.segments.SegmentNode;
import com.indago.tr2d.io.projectfolder.Tr2dProjectFolder;
import com.indago.tr2d.pg.Tr2dSegmentationProblem;
import com.indago.tr2d.pg.Tr2dTrackingProblem;
import com.indago.util.TicToc;

import gurobi.GRBException;
import ij.IJ;
import io.scif.img.ImgIOException;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.roi.IterableRegion;
import net.imglib2.roi.Regions;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.util.Pair;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;


/**
 * @author jug
 */
public class Tr2dTrackingModel {

	private final String FILENAME_PGRAPH = "tracking.pgraph";
	private final String FILENAME_GUROBI_MODEL = "gurobiModel.jug";
	private final String FILENAME_TRACKING = "tracking.tif";

	private final Tr2dModel tr2dModel;
	private final Tr2dSegmentationCollectionModel tr2dSegModel;

	private final CostsFactory< LabelingSegment > segmentCosts;
	private final CostsFactory< LabelingSegment > appearanceCosts;
	private final CostsFactory< Pair< LabelingSegment, LabelingSegment > > moveCosts;
	private final CostsFactory< Pair< LabelingSegment, Pair< LabelingSegment, LabelingSegment > > > divisionCosts;
	private final CostsFactory< LabelingSegment > disappearanceCosts;

	private Tr2dTrackingProblem tr2dTraProblem;
	private LabelingTimeLapse sumImgMovie;
	private RandomAccessibleInterval< IntType > imgSolution = null;

	private MappedFactorGraph mfg;
	private Assignment< Variable > fgSolution;
	private Assignment< IndicatorNode > pgSolution;

	private final ProjectFolder dataFolder;

	/**
	 * @param model
	 */
	public Tr2dTrackingModel(
			final Tr2dModel model,
			final Tr2dSegmentationCollectionModel modelSeg,
			final CostsFactory< LabelingSegment > segmentCosts,
			final CostsFactory< LabelingSegment > appearanceCosts,
			final CostsFactory< Pair< LabelingSegment, LabelingSegment > > movementCosts,
			final CostsFactory< Pair< LabelingSegment, Pair< LabelingSegment, LabelingSegment > > > divisionCosts,
			final CostsFactory< LabelingSegment > disappearanceCosts ) {
		this.tr2dModel = model;

		dataFolder = model.getProjectFolder().getFolder( Tr2dProjectFolder.TRACKING_FOLDER );
		dataFolder.mkdirs();

		this.appearanceCosts = appearanceCosts;
		this.moveCosts = movementCosts;
		this.divisionCosts = divisionCosts;
		this.disappearanceCosts = disappearanceCosts;

		this.segmentCosts = segmentCosts;

		this.tr2dSegModel = modelSeg;

		final File fImgSol = dataFolder.addFile( FILENAME_TRACKING ).getFile();
		if ( fImgSol.canRead() ) {
			try {
				imgSolution = IntTypeImgLoader.loadTiffEnsureType( fImgSol );
			} catch ( final ImgIOException e ) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Prepares a tracking (builds pg and fg and stores intermediate data in
	 * project folder).
	 */
	private void prepare() {
		if ( sumImgMovie == null ) {
			processSegmentationInputs();
			tr2dTraProblem = null;
		}

		if ( tr2dTraProblem == null ) {
			buildTrackingProblem();
			saveTrackingProblem();
			mfg = null;
		}

		if ( mfg == null ) {
			buildFactorGraph();
			saveFactorGraph();
		}
	}

	/**
	 * Runs the optimization for the prepared tracking.
	 */
	public void run() {
		prepare();

		solveFactorGraph();

		drawSolution();
		saveSolution();
	}

	/**
	 *
	 */
	private void saveTrackingProblem() {
		try {
			tr2dTraProblem.saveToFile( dataFolder.getFile( FILENAME_PGRAPH ).getFile() );
		} catch ( final IOException e ) {
			e.printStackTrace();
		} catch ( final NullPointerException npe ) {
			System.err.println( "ERROR: PGraph could not be stored to disk!" );
		}
	}

	/**
	 *
	 */
	private void saveFactorGraph() {
		// TODO Auto-generated method stub

	}

	/**
	 *
	 */
	private void saveSolution() {
		IJ.save(
				ImageJFunctions.wrap( imgSolution, "tracking solution" ).duplicate(),
				dataFolder.getFile( FILENAME_TRACKING ).getAbsolutePath() );
	}

	/**
	 *
	 */
	public void processSegmentationInputs() {
		this.sumImgMovie = new LabelingTimeLapse( tr2dSegModel );
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
	public void buildTrackingProblem() {
		final TicToc tictoc = new TicToc();

		this.tr2dTraProblem =
				new Tr2dTrackingProblem( appearanceCosts, moveCosts, HernanCostConstants.TRUNCATE_COST_THRESHOLD, divisionCosts, HernanCostConstants.TRUNCATE_COST_THRESHOLD, disappearanceCosts );

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
		tr2dTraProblem.addDummyDisappearance();

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
			pgSolution = assMapper.map( fgSolution );
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
			this.imgSolution = DataMover.createEmptyArrayImgLike( tr2dSegModel.getWekaModel().getClassifications().get( 0 ), new IntType() );
		} catch ( final ArrayIndexOutOfBoundsException e ) {
			this.imgSolution =
					DataMover.createEmptyArrayImgLike(
							tr2dSegModel.getImportedSegmentationModel().getSegmentHypothesesImages().get( 0 ),
							new IntType() );
		}

//		int time = 0;
//		for ( final Tr2dSegmentationModel segProblem : tr2dTraModel.getTimepoints() ) {
//			final IntervalView< DoubleType > slice = Views.hyperSlice( imgSolution, 2, time );
//
//			for ( final SegmentVar segVar : segProblem.getSegments() ) {
//				if ( problemSolution.getAssignment( segVar ) == 1 ) {
//					final IterableRegion< ? > region = segVar.getSegment().getRegion();
//					Regions.sample( region, slice ).forEach( t -> t.set( t.get() + 1 ) );
//				}
//			}
//			time++;
//		}

		int time = 0;
		int curColorId = 1;
		for ( final Tr2dSegmentationProblem segProblem : tr2dTraProblem.getTimepoints() ) {
			for ( final SegmentNode segVar : segProblem.getSegments() ) {
				for ( final AppearanceHypothesis app : segVar.getInAssignments().getAppearances() ) {
					if ( pgSolution.getAssignment( app ) == 1 ) { // || time == 0
						drawLineageWithId( time, segVar, 10 + curColorId );
						curColorId++;
					}
				}
			}
			time++;
		}
	}

	/**
	 * Opens the computed tracking solution image in ImageJ (if it was computed
	 * already). Does nothing otherwise.
	 */
	public void showSolutionInImageJ() {
		if ( imgSolution != null ) ImageJFunctions.show( imgSolution, "Solution" );
	}

	/**
	 * @param segVar
	 * @param curColorId
	 */
	private void drawLineageWithId( final int time, final SegmentNode segVar, final int curColorId ) {
		final IntervalView< IntType > slice = Views.hyperSlice( imgSolution, 2, time );

		if ( pgSolution.getAssignment( segVar ) == 1 ) {
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
				if ( pgSolution.getAssignment( move ) == 1 ) {
					drawLineageWithId( time + 1, move.getDest(), curColorId );
				}
			}
			for ( final DivisionHypothesis div : segVar.getOutAssignments().getDivisions() ) {
				if ( pgSolution.getAssignment( div ) == 1 ) {
					drawLineageWithId( time + 1, div.getDest1(), curColorId );
					drawLineageWithId( time + 1, div.getDest2(), curColorId );
				}
			}
		}
	}

	/**
	 * @return the imgSolution
	 */
	public RandomAccessibleInterval< IntType > getImgSolution() {
		return imgSolution;
	}
}
