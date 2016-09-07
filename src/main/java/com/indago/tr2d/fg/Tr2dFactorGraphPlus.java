/**
 *
 */
package com.indago.tr2d.fg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.indago.app.hernan.Tr2dApplication;
import com.indago.data.segmentation.Segment;
import com.indago.data.segmentation.fg.FactorGraphPlus;
import com.indago.data.segmentation.fg.SegmentHypothesisVariable;
import com.indago.old_fg.FactorGraph;
import com.indago.old_fg.domain.BooleanFunctionDomain;
import com.indago.old_fg.factor.BooleanFactor;
import com.indago.old_fg.factor.Factor;
import com.indago.old_fg.function.BooleanWeightedIndexSumConstraint;
import com.indago.old_fg.function.Function;
import com.indago.old_fg.function.WeightedIndexSumConstraint.Relation;
import com.indago.old_fg.variable.BooleanVariable;
import com.indago.old_fg.variable.Variable;
import com.indago.tr2d.fg.factor.AppearanceFactor;
import com.indago.tr2d.fg.factor.DisappearanceFactor;
import com.indago.tr2d.fg.factor.DivisionFactor;
import com.indago.tr2d.fg.factor.MappingFactor;


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
	 * @see com.indago.old_fg.FactorGraph#getVariables()
	 */
	@Override
	public Collection< ? extends Variable< ? > > getVariables() {
		return variables;
	}

	/**
	 * @see com.indago.old_fg.FactorGraph#getFactors()
	 */
	@Override
	public Collection< ? extends Factor< ?, ?, ? > > getFactors() {
		return factors;
	}

	/**
	 * @see com.indago.old_fg.FactorGraph#getFunctions()
	 */
	@Override
	public Collection< ? extends Function< ?, ? > > getFunctions() {
		return functions;
	}

	/**
	 * @param frameFG
	 */
	public void addFirstFrame( final FactorGraphPlus< Segment > frameFG ) {
		if ( getFrameFGs().size() == 0 ) {
			getFrameFGs().add( frameFG );
			mergeFG( frameFG );
		} else {
			throw new IllegalStateException( "Tr2dFactorGraphPlus::addFirstFrame() called after frames have already been added" );
		}
	}

	public void addFrame(
			final FactorGraphPlus< Segment > transFG,
			final FactorGraphPlus< Segment > frameFG ) {
		getTransFGs().add( transFG );
		getFrameFGs().add( frameFG );

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
		if ( getFrameFGs().size() >= 2 ) {
			addRightContinuationConstraints( getFrameFGs().get( getFrameFGs().size() - 2 ) );
			addLeftContinuationConstraints( getFrameFGs().get( getFrameFGs().size() - 1 ) );
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
	 * Add constraint requesting that all right adjacent assignment variables
	 * sum to the same value as the segmentation variable.
	 *
	 * @param factorGraphPlus
	 */
	private void addRightContinuationConstraints( final FactorGraphPlus< Segment > frameFG ) {
		BooleanFactor factor;
		int numRightContinuationConstraints = 0;

		for ( final SegmentHypothesisVariable< Segment > segVar : frameFG.getSegmentVariables() ) {
			final int sizeRN = segVar.getRightNeighbors().size();

			// add constraint to right neighbors
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

			numRightContinuationConstraints++;
		}
		Tr2dApplication.log.trace(
				String.format(
						"\n\t\tRight Continuation Constraints added: %d",
						numRightContinuationConstraints ) );
	}

	/**
	 * Add constraint requesting that all left adjacent assignment variables
	 * sum to the same value as the segmentation variable.
	 *
	 * @param factorGraphPlus
	 */
	private void addLeftContinuationConstraints( final FactorGraphPlus< Segment > frameFG ) {
		BooleanFactor factor;
		int numLeftContinuationConstraints = 0;

		for ( final SegmentHypothesisVariable< Segment > segVar : frameFG.getSegmentVariables() ) {
			final int sizeLN = segVar.getLeftNeighbors().size();


			// add constraint to left neighbors
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

			numLeftContinuationConstraints++;
		}
		Tr2dApplication.log.trace(
				String.format(
						"\n\t\tLeft Continuation Constraints added: %d",
						numLeftContinuationConstraints ) );
	}

	public int consumeNextFactorId() {
		return factorId++;
	}

	public int consumeNextFunctionId() {
		return functionId++;
	}

	/**
	 * @return the frameFGs
	 */
	public List< FactorGraphPlus< Segment > > getFrameFGs() {
		return frameFGs;
	}

	/**
	 * @return the transFGs
	 */
	public List< FactorGraphPlus< Segment > > getTransFGs() {
		return transFGs;
	}

}
