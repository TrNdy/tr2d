/**
 *
 */
package com.jug.tr2d.fg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.indago.fg.FactorGraph;
import com.indago.fg.domain.BooleanFunctionDomain;
import com.indago.fg.factor.BooleanFactor;
import com.indago.fg.factor.Factor;
import com.indago.fg.function.BooleanWeightedIndexSumConstraint;
import com.indago.fg.function.Function;
import com.indago.fg.function.WeightedIndexSumConstraint.Relation;
import com.indago.fg.variable.BooleanVariable;
import com.indago.fg.variable.Variable;
import com.indago.segment.Segment;
import com.indago.segment.fg.FactorGraphPlus;
import com.indago.segment.fg.SegmentHypothesisVariable;
import com.jug.tr2d.fg.factor.AppearanceFactor;
import com.jug.tr2d.fg.factor.DisappearanceFactor;
import com.jug.tr2d.fg.factor.DivisionFactor;
import com.jug.tr2d.fg.factor.MappingFactor;


/**
 * @author jug
 */
public class Tr2dFactorGraphPlus implements FactorGraph {

	private int factorId = 0;
	private int functionId = 0;

	private final Collection< Variable< ? > > variables;
	private final Collection< Factor< ?, ?, ? > > factors;
	private final Collection< Function< ?, ? > > functions;

	// the sub factor graphs
	private final List< FactorGraphPlus< Segment > > frameFGs;
	private final List< FactorGraphPlus< Segment > > transFGs;

	/**
	 * Creates an empty Tr2d Factor Graph object.
	 *
	 * @param perFrameLabelingForests
	 */
	public Tr2dFactorGraphPlus(
			final FactorGraphPlus< Segment > firstFrameFG ) {
		this();
		addFirstFrame( firstFrameFG );
	}

	/**
	 *
	 */
	public Tr2dFactorGraphPlus() {
		frameFGs = new ArrayList< FactorGraphPlus< Segment > >();
		transFGs = new ArrayList< FactorGraphPlus< Segment > >();

		variables = new ArrayList< Variable< ? > >();
		factors = new ArrayList< Factor< ?, ?, ? > >();
		functions = new ArrayList< Function< ?, ? > >();
	}

	/**
	 * @see com.indago.fg.FactorGraph#getVariables()
	 */
	@Override
	public Collection< ? extends Variable< ? > > getVariables() {
		return variables;
	}

	/**
	 * @see com.indago.fg.FactorGraph#getFactors()
	 */
	@Override
	public Collection< ? extends Factor< ?, ?, ? > > getFactors() {
		return factors;
	}

	/**
	 * @see com.indago.fg.FactorGraph#getFunctions()
	 */
	@Override
	public Collection< ? extends Function< ?, ? > > getFunctions() {
		return functions;
	}

	/**
	 * @param frameFG
	 */
	public void addFirstFrame( final FactorGraphPlus< Segment > frameFG ) {
		if ( frameFGs.size() == 0 ) {
			frameFGs.add( frameFG );
			mergeFG( frameFG );
		} else {
			throw new IllegalStateException( "Tr2dFactorGraphPlus::addFirstFrame() called after frames have already been added" );
		}
	}

	public void addFrame(
			final FactorGraphPlus< Segment > transFG,
			final FactorGraphPlus< Segment > frameFG ) {
		transFGs.add( transFG );
		frameFGs.add( frameFG );

		mergeFG( frameFG );
		mergeFG( transFG );

		for ( final Factor< ?, ?, ? > factor : transFG.getFactorGraph().getFactors() ) {
			final BooleanVariable factorIndicatorVariable = ( BooleanVariable ) factor.getVariable( 0 );
			if ( factor instanceof MappingFactor ) {
				( ( SegmentHypothesisVariable< Segment > ) factor.getVariable( 1 ) )
						.addRightNeighbor( factorIndicatorVariable );
				( ( SegmentHypothesisVariable< Segment > ) factor.getVariable( 2 ) )
						.addLeftNeighbor( factorIndicatorVariable );
			} else if ( factor instanceof DivisionFactor ) {
				( ( SegmentHypothesisVariable< Segment > ) factor.getVariable( 1 ) )
						.addRightNeighbor( factorIndicatorVariable );
				( ( SegmentHypothesisVariable< Segment > ) factor.getVariable( 2 ) )
						.addLeftNeighbor( factorIndicatorVariable );
				( ( SegmentHypothesisVariable< Segment > ) factor.getVariable( 3 ) )
						.addLeftNeighbor( factorIndicatorVariable );
			} else if ( factor instanceof AppearanceFactor ) {
				( ( SegmentHypothesisVariable< Segment > ) factor.getVariable( 1 ) )
						.addLeftNeighbor( factorIndicatorVariable );
			} else if ( factor instanceof DisappearanceFactor ) {
				( ( SegmentHypothesisVariable< Segment > ) factor.getVariable( 1 ) )
						.addRightNeighbor( factorIndicatorVariable );
			} else if ( factor instanceof BooleanFactor ) {
				// BooleanFactors are NOT used to connect SegmentHypothesisVariables
			} else {
				throw new IllegalArgumentException( "Unknown factor class " + factor
						.getClass()
						.toString() );
			}
		}

		// If frame is >2nd frame (id>=2) --> add continuation constraints
		if ( frameFGs.size() >= 2 ) {
			addContinuationConstraints( frameFGs.get( frameFGs.size() - 2 ) );
		}

	}

	/**
	 * @param frameFG
	 */
	private void mergeFG( final FactorGraphPlus< Segment > fg ) {
		this.variables.addAll( fg.getFactorGraph().getVariables() );
		this.factors.addAll( fg.getFactorGraph().getFactors() );
		this.functions.addAll( fg.getFactorGraph().getFunctions() );
	}

	/**
	 * For each segment variable we add 2 constraints.
	 * One requesting that all left adjacent assignment variables sum to the
	 * same value as the segmentation variable, and a second enforcing the same
	 * towards the right neighbors.
	 *
	 * @param factorGraphPlus
	 */
	private void addContinuationConstraints( final FactorGraphPlus< Segment > frameFG ) {
		BooleanFactor factor;
		int numContinuationConstraintPairs = 0;

		for ( final SegmentHypothesisVariable< Segment > segVar : frameFG.getSegmentVariables() ) {
			final int sizeLN = segVar.getLeftNeighbors().size();
			final int sizeRN = segVar.getRightNeighbors().size();
//			System.out.println(
//					String.format(
//							"\t\tadding Cont.Constr. with %d l.n. and %d r.n. ",
//							sizeLN,
//							sizeRN ) );


			// add first constraint to left neighbors
			final BooleanFunctionDomain bfd1 = new BooleanFunctionDomain( sizeLN + 1 );
			factor = new BooleanFactor( bfd1, consumeNextFactorId() );
			final double[] coeffs1 = new double[ sizeLN + 1 ];
			coeffs1[ sizeLN ] = -1;
			factor.setVariable( sizeLN, segVar );
			for ( int i = 0; i < sizeLN; i++ ) {
				coeffs1[ i ] = 1;
				factor.setVariable( i, segVar.getLeftNeighbors().get( i ) );
			}
			factor.setFunction( new BooleanWeightedIndexSumConstraint( coeffs1, Relation.EQ, 0 ) );
			factors.add( factor );

			// add first constraint to left neighbors
			final BooleanFunctionDomain bfd2 = new BooleanFunctionDomain( sizeRN + 1 );
			factor = new BooleanFactor( bfd2, consumeNextFactorId() );
			final double[] coeffs2 = new double[ sizeRN + 1 ];
			coeffs2[ sizeRN ] = -1;
			factor.setVariable( sizeRN, segVar );
			for ( int i = 0; i < sizeRN; i++ ) {
				coeffs2[ i ] = 1;
				factor.setVariable( i, segVar.getRightNeighbors().get( i ) );
			}
			factor.setFunction( new BooleanWeightedIndexSumConstraint( coeffs2, Relation.EQ, 0 ) );
			factors.add( factor );

			numContinuationConstraintPairs++;
		}
		System.out.println(
				String.format(
						"\n\t\tContinuation Constraint Pairs added: %d",
						numContinuationConstraintPairs ) );
	}

	public int consumeNextFactorId() {
		return factorId++;
	}

	public int consumeNextFunctionId() {
		return functionId++;
	}

}
