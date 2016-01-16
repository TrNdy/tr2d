/**
 *
 */
package com.jug.tr2d;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import com.indago.fg.Assignment;
import com.indago.fg.CostsFactory;
import com.indago.fg.FactorGraph;
import com.indago.fg.factor.BooleanFactor;
import com.indago.fg.factor.Factor;
import com.indago.fg.function.BooleanConflictConstraint;
import com.indago.fg.function.BooleanFunction;
import com.indago.fg.function.BooleanTensorTable;
import com.indago.fg.value.BooleanValue;
import com.indago.fg.variable.BooleanVariable;
import com.indago.fg.variable.Variable;
import com.indago.segment.LabelingBuilder;
import com.indago.segment.LabelingForest;
import com.indago.segment.LabelingSegment;
import com.indago.segment.MinimalOverlapConflictGraph;
import com.indago.segment.Segment;
import com.indago.segment.fg.FactorGraphFactory;
import com.indago.segment.fg.FactorGraphPlus;
import com.indago.segment.fg.SegmentHypothesisVariable;
import com.indago.segment.filteredcomponents.FilteredComponentTree;
import com.indago.segment.filteredcomponents.FilteredComponentTree.Filter;
import com.indago.segment.filteredcomponents.FilteredComponentTree.MaxGrowthPerStep;
import com.jug.tr2d.datasets.hernan.HernanAppearanceCostFactory;
import com.jug.tr2d.datasets.hernan.HernanCostConstants;
import com.jug.tr2d.datasets.hernan.HernanDisappearanceCostFactory;
import com.jug.tr2d.datasets.hernan.HernanDivisionCostFactory;
import com.jug.tr2d.datasets.hernan.HernanMappingCostFactory;
import com.jug.tr2d.datasets.hernan.HernanSegmentCostFactory;
import com.jug.tr2d.fg.Tr2dFactorGraphFactory;
import com.jug.tr2d.fg.Tr2dFactorGraphPlus;

import gurobi.GRB;
import gurobi.GRB.DoubleAttr;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;
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

	public static class GurobiReadouts {

		public int numIterations;
		public double runtime;
		public double objval;

		public GurobiReadouts(
				final int numIterations,
				final double runtime,
				final double objval ) {
			this.numIterations = numIterations;
			this.runtime = runtime;
			this.objval = objval;
		}
	}

	private final Tr2dModel tr2dModel;
	private final Tr2dWekaSegmentationModel tr2dSegModel;

	// Parameters for FilteredComponentTree of SumImage(s)
	private Dimensions dim;
	private final int minComponentSize = 10;
	private final int maxComponentSize = 10000;
	private final Filter maxGrowthPerStep;
	private final boolean darkToBright = false;

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
			System.out.println(
					String.format( "Working on frame %d of %d...", frameId + 1, numFrames ) );

			final List< LabelingForest > frameLabelingForests = new ArrayList< LabelingForest >();

			// Hyperslize current frame out of complete dataset
			IntervalView< DoubleType > hsFrame = null;
			try {
				hsFrame = Views.hyperSlice( getSegmentHypothesesImage(), 2, frameId );
//				ImageJFunctions.show( hsFrame );
			} catch ( final IllegalAccessException e ) {
				System.err.println( "\tSegmentation Hypotheses could not be accessed!" );
				e.printStackTrace();
				return;
			}

			System.out.print( "\tBuilding FilteredComponentTree and LabelingForest... " );
			t0 = System.currentTimeMillis();
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
			t1 = System.currentTimeMillis();
			System.out
					.println( String.format( "completed in %.2f seconds!", ( t1 - t0 ) / 1000. ) );

			System.out.print( "\tConstructing MinimalOverlapConflictGraph... " );
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
			System.out.print( "\tConstructing frameFG from MinimalOverlapConflictGraph... " );
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
			FactorGraphPlus< Segment > transFG = null;
			if ( frameId > 0 ) {
				System.out.print( "\tConstructing transFG... " );
				t0 = System.currentTimeMillis();
				final CostsFactory< Pair< Segment, Segment > > mappingCosts =
						new HernanMappingCostFactory( frameId, tr2dModel.getImgOrig() );
				final CostsFactory< Pair< Segment, Pair< Segment, Segment > > > divisionCosts =
						new HernanDivisionCostFactory( frameId, tr2dModel.getImgOrig() );
				final CostsFactory< Segment > appearanceCosts =
						new HernanAppearanceCostFactory( frameId, tr2dModel.getImgOrig() );
				final CostsFactory< Segment > disappearanceCosts =
						new HernanDisappearanceCostFactory( frameId, tr2dModel.getImgOrig() );
				transFG = new Tr2dFactorGraphFactory()
						.createTransitionGraph(
								fgPlus,
								oldSegVars,
								segVars,
								mappingCosts,
								HernanCostConstants.TRUNCATE_COST_THRESHOLD,
								divisionCosts,
								HernanCostConstants.TRUNCATE_COST_THRESHOLD,
								appearanceCosts,
								HernanCostConstants.TRUNCATE_COST_THRESHOLD,
								disappearanceCosts,
								HernanCostConstants.TRUNCATE_COST_THRESHOLD );
				t1 = System.currentTimeMillis();
				System.out.println(
						String.format( "completed in %.2f seconds!", ( t1 - t0 ) / 1000. ) );
			}

			// =============
			//   ADD FRAME
			// =============
			System.out.print( "\tAdding new FGs... " );
			t0 = System.currentTimeMillis();
			if ( frameId == 0 ) {
				fgPlus.addFirstFrame( frameFG );
			} else {
				fgPlus.addFrame( transFG, frameFG );
			}
			t1 = System.currentTimeMillis();
			System.out
					.println( String.format( "completed in %.2f seconds!", ( t1 - t0 ) / 1000. ) );

			oldSegments = segments;
			oldSegVars = segVars;
		}

		System.out.println( "FG successfully built!\n" );

		System.out.println( "Constructing and solving ILP... " );
		t0 = System.currentTimeMillis();
		GurobiReadouts gurobiStats;
		try {
			gurobiStats = buildAndRunILP( fgPlus );
		} catch ( final GRBException e ) {
			e.printStackTrace();
		}
		t1 = System.currentTimeMillis();
		System.out
				.println( String.format( "...completed in %.2f seconds!", ( t1 - t0 ) / 1000. ) );
	}

	private static GurobiReadouts buildAndRunILP( final FactorGraph fg ) throws GRBException {
		for ( final Variable< ? > variable : fg.getVariables() ) {
			if ( !( variable instanceof BooleanVariable ) )
				throw new IllegalArgumentException( "Non boolean variable found (and so far not supported)!" );
		}
		final ArrayList< BooleanFactor > constraints = new ArrayList< BooleanFactor >();
		final ArrayList< BooleanFactor > unaries = new ArrayList< BooleanFactor >();
		for ( final Factor< ?, ?, ? > f : fg.getFactors() ) {
			if ( f instanceof BooleanFactor ) {
				final BooleanFactor factor = ( BooleanFactor ) f;
				final BooleanFunction function = factor.getFunction();
				if ( function instanceof BooleanConflictConstraint )
					constraints.add( factor );
				else if ( function instanceof BooleanTensorTable )
					unaries.add( factor );
				else
					throw new IllegalArgumentException( "Function that fucks it up: " + function
							.getClass()
							.toString() );
			} else
				throw new IllegalArgumentException( "Factor that fucks it up: " + f
						.getClass()
						.toString() );
		}

		final List< BooleanVariable > variables = ( List< BooleanVariable > ) fg.getVariables();
		final HashMap< BooleanVariable, Integer > variableToIndex = new HashMap< >();
		int variableIndex = 0;
		for ( final BooleanVariable v : variables )
			variableToIndex.put( v, variableIndex++ );

		final GRBEnv env = new GRBEnv( "mip1.log" );
		final GRBModel model = new GRBModel( env );

		// Create variables
		System.out.println( String.format( "\tadding %d variables...", variables.size() ) );
		final GRBVar[] vars = model.addVars( variables.size(), GRB.BINARY );

		// Integrate new variables
		model.update();

		// Set objective: minimize costs
		System.out.println( String.format( "\tsetting objective function..." ) );
		final double[] coeffs = new double[ variables.size() ];
		for ( final BooleanFactor factor : unaries ) {
			final int i = variableToIndex.get( factor.getVariable( 0 ) );
			final BooleanTensorTable costs = ( BooleanTensorTable ) factor.getFunction();
			coeffs[ i ] =
					costs.evaluate( BooleanValue.TRUE ) - costs.evaluate( BooleanValue.FALSE );
		}
		final GRBLinExpr expr = new GRBLinExpr();
		expr.addTerms( coeffs, vars );
		model.setObjective( expr, GRB.MINIMIZE );

		// Add constraints.
		System.out.println( String.format( "\tadding %d constraints...", constraints.size() ) );
		for ( int i = 0; i < constraints.size(); i++ ) {
			final BooleanFactor constraint = constraints.get( i );
			final GRBLinExpr lhsExprs = new GRBLinExpr();
			for ( final BooleanVariable variable : constraint.getVariables() ) {
				final int vi = variableToIndex.get( variable );
				lhsExprs.addTerm( 1.0, vars[ vi ] );
			}
			model.addConstr( lhsExprs, GRB.LESS_EQUAL, 1.0, null );
		}

		// Optimize model
		System.out.println( String.format( "\tstarting optimization..." ) );
		model.optimize();
		final int iterCount = ( int ) Math.round( model.get( GRB.DoubleAttr.IterCount ) );
		final double solvingTime = model.get( GRB.DoubleAttr.Runtime );
		final double objval = model.get( GRB.DoubleAttr.ObjVal );
//		System.out.println( "Obj: " + model.get( GRB.DoubleAttr.ObjVal ) );

		// Build assignment
		System.out.println( String.format( "\retrieving (optimal) assignment..." ) );
		final Assignment assignment = new Assignment( variables );
		for ( int i = 0; i < variables.size(); i++ ) {
			final BooleanVariable variable = variables.get( i );
			final BooleanValue value =
					vars[ i ].get( DoubleAttr.X ) > 0.5 ? BooleanValue.TRUE : BooleanValue.FALSE;
			assignment.assign( variable, value );

//			System.out.println( variable + " = " + assignment.getAssignment( variable ) );
		}

		// Dispose of model and environment
		model.dispose();
		env.dispose();

		return new GurobiReadouts( iterCount, solvingTime, objval );
	}

}
