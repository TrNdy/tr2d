/**
 *
 */
package com.indago.tr2d.ui.model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.indago.app.hernan.costs.HernanCostConstants;
import com.indago.costs.CostsFactory;
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
import com.indago.pg.IndicatorNode;
import com.indago.pg.assignments.AppearanceHypothesis;
import com.indago.pg.assignments.DivisionHypothesis;
import com.indago.pg.assignments.MovementHypothesis;
import com.indago.pg.segments.SegmentNode;
import com.indago.tr2d.io.projectfolder.Tr2dProjectFolder;
import com.indago.tr2d.pg.Tr2dSegmentationProblem;
import com.indago.tr2d.pg.Tr2dTrackingProblem;
import com.indago.tr2d.ui.view.bdv.BdvWithOverlaysOwner;
import com.indago.util.TicToc;

import bdv.util.BdvHandlePanel;
import bdv.util.BdvOverlay;
import bdv.util.BdvSource;
import gurobi.GRBException;
import ij.IJ;
import io.scif.img.ImgIOException;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.roi.IterableRegion;
import net.imglib2.roi.Regions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.util.Pair;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;


/**
 * @author jug
 */
public class Tr2dTrackingModel implements BdvWithOverlaysOwner {

	private final ProjectFolder dataFolder;

	private final String FOLDER_LABELING_FRAMES = "labeling_frames";
	private ProjectFolder hypothesesFolder = null;

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
	private final LabelingTimeLapse labelingFrames;
	private RandomAccessibleInterval< IntType > imgSolution = null;

	private MappedFactorGraph mfg;
	private Assignment< Variable > fgSolution;
	private Assignment< IndicatorNode > pgSolution;

	private BdvHandlePanel bdvHandlePanel;
	private final List< RandomAccessibleInterval< IntType > > imgs;
	private final List< BdvSource > bdvSources = new ArrayList< >();
	private final List< BdvOverlay > overlays = new ArrayList< >();
	private final List< BdvSource > bdvOverlaySources = new ArrayList< >();

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
		imgs = new ArrayList< >();
		imgs.add( imgSolution );

		// Loading hypotheses labeling frames if exist in project folder
		this.labelingFrames = new LabelingTimeLapse( tr2dSegModel );
		try {
			hypothesesFolder = dataFolder.addFolder( FOLDER_LABELING_FRAMES );
			hypothesesFolder.loadFiles();
			labelingFrames.loadFromProjectFolder( hypothesesFolder );
		} catch ( final IOException ioe ) {
			ioe.printStackTrace();
		}
	}

	/**
	 * Marks this tracking model as 'reset'.
	 */
	public void reset() {
		tr2dTraProblem = null;
	}

	/**
	 * Prepares the tracking model (Step1: builds pg and stores intermediate
	 * data in
	 * project folder).
	 */
	private void preparePG() {
		processSegmentationInputs();
		buildTrackingProblem();
		saveTrackingProblem();
		mfg = null;
	}

	/**
	 * Prepares the tracking model (Step2: builds fg from pg and stores
	 * intermediate data in
	 * project folder).
	 */
	public void prepareFG() {
		buildFactorGraph();
		saveFactorGraph();
	}

	/**
	 * Runs the optimization for the prepared tracking (in <code>prepare</code>
	 * was never called, this function will call it).
	 * Does not take care of the BDV.
	 * For a threaded version us <code>runInThread</code>, which also takes care
	 * of BDV.
	 */
	public void run() {
		if ( tr2dTraProblem == null ) {
			preparePG();
			prepareFG();
		} else if ( mfg == null ) {
			prepareFG();
		}

		solveFactorGraph();

		drawSolution();
		saveSolution();
	}

	/**
	 * (Re)runs the trackins problem in a thread of it's own.
	 * Additionally also takes care of the BDV.
	 */
	public void runInThread() {
		final Tr2dTrackingModel self = this;
		final Runnable runnable = new Runnable() {

			@Override
			public void run() {
				self.run();
				bdvRemoveAll();
				bdvAdd( getTr2dModel().getRawData(), "RAW" );
				bdvAdd( getImgSolution(), "solution" );
			}

		};
		new Thread( runnable ).start();
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
		if ( labelingFrames.needProcessing() ) {
			if ( !labelingFrames.processFrames() ) {
				System.err.println(
						"Segmentation Hypotheses could not be accessed!\nYou must create a segmentation prior to starting the tracking!" );
				return;
			}
			labelingFrames.saveTo( hypothesesFolder );
		}
	}

	/**
	 *
	 */
	public void buildTrackingProblem() {
		final TicToc tictoc = new TicToc();

		this.tr2dTraProblem =
				new Tr2dTrackingProblem( appearanceCosts, moveCosts, HernanCostConstants.TRUNCATE_COST_THRESHOLD, divisionCosts, HernanCostConstants.TRUNCATE_COST_THRESHOLD, disappearanceCosts );

		for ( int frameId = 0; frameId < labelingFrames.getNumFrames(); frameId++ ) {
			System.out.println(
					String.format( "Working on frame %d of %d...", frameId + 1, labelingFrames.getNumFrames() ) );

			// =============================
			// build Tr2dSegmentationProblem
			// =============================
			tictoc.tic( "Constructing Tr2dSegmentationProblem..." );
			final List< LabelingSegment > segments =
					labelingFrames.getLabelingSegmentsForFrame( frameId );
			final ConflictGraph< LabelingSegment > conflictGraph =
					labelingFrames.getConflictGraph( frameId );
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
			SolveGurobi.GRB_PRESOLVE = 0;
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
			if ( tr2dSegModel.getImportedSegmentationModel().getSegmentHypothesesImages().isEmpty() ) {
				this.imgSolution = DataMover.createEmptyArrayImgLike( tr2dSegModel.getWekaModel().getClassifications().get( 0 ), new IntType() );
			} else {
				this.imgSolution = DataMover
						.createEmptyArrayImgLike( tr2dSegModel.getImportedSegmentationModel().getSegmentHypothesesImages().get( 0 ), new IntType() );
			}
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

	/**
	 * @see com.indago.tr2d.ui.view.bdv.BdvOwner#setBdvHandlePanel()
	 */
	@Override
	public void bdvSetHandlePanel( final BdvHandlePanel bdvHandlePanel ) {
		this.bdvHandlePanel = bdvHandlePanel;
	}

	/**
	 * @see com.indago.tr2d.ui.view.bdv.BdvOwner#bdvGetHandlePanel()
	 */
	@Override
	public BdvHandlePanel bdvGetHandlePanel() {
		return bdvHandlePanel;
	}

	/**
	 * @see com.indago.tr2d.ui.view.bdv.BdvOwner#bdvGetSources()
	 */
	@Override
	public List< BdvSource > bdvGetSources() {
		return bdvSources;
	}

	/**
	 * @see com.indago.tr2d.ui.view.bdv.BdvOwner#bdvGetSourceFor(net.imglib2.RandomAccessibleInterval)
	 */
	@Override
	public < T extends RealType< T > & NativeType< T > > BdvSource bdvGetSourceFor( final RandomAccessibleInterval< T > img ) {
		final int idx = imgs.indexOf( img );
		if ( idx == -1 ) return null;
		return bdvGetSources().get( idx );
	}

	/**
	 * @see com.indago.tr2d.ui.view.bdv.BdvWithOverlaysOwner#bdvGetOverlays()
	 */
	@Override
	public List< BdvOverlay > bdvGetOverlays() {
		return overlays;
	}

	/**
	 * @see com.indago.tr2d.ui.view.bdv.BdvWithOverlaysOwner#bdvGetOverlaySources()
	 */
	@Override
	public List< BdvSource > bdvGetOverlaySources() {
		return bdvOverlaySources;
	}

	/**
	 * @return
	 */
	public Tr2dTrackingProblem getTrackingProblem() {
		return this.tr2dTraProblem;
	}

	/**
	 * @return
	 */
	public Assignment< IndicatorNode > getSolution() {
		return this.pgSolution;
	}

	/**
	 * @return the tr2dModel
	 */
	public Tr2dModel getTr2dModel() {
		return tr2dModel;
	}

	/**
	 * @return the labelingFrames
	 */
	public LabelingTimeLapse getLabelingFrames() {
		return labelingFrames;
	}
}
