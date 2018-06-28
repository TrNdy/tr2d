/**
 *
 */
package com.indago.tr2d.fg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.indago.data.segmentation.Segment;
import com.indago.data.segmentation.fg.FactorGraphPlus;
import com.indago.data.segmentation.fg.SegmentHypothesisVariable;
import com.indago.fg.BooleanVariable;
import com.indago.fg.Factor;
import com.indago.fg.FactorGraph;
import com.indago.fg.Factors;
import com.indago.fg.Function;
import com.indago.fg.Variable;
import com.indago.tr2d.Tr2dLog;
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

	private final Collection< Variable > variables;
	private final Collection< Factor > factors;
	private final Collection< Function > functions;

	// the sub factor graphs
	private final List< FactorGraphPlus< Segment > > frameFGs;
	private final List< FactorGraphPlus< Segment > > transFGs;

	/**
	 * Creates an empty Tr2d Factor Graph object.
	 *
	 * @param firstFrameFG
	 *            the FG representing the first frame
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

		variables = new ArrayList< Variable >();
		factors = new ArrayList< Factor >();
		functions = new ArrayList< Function >();
	}

	/**
	 * @see com.indago.fg.FactorGraph#getVariables()
	 */
	@Override
	public Collection< ? extends Variable > getVariables() {
		return variables;
	}

	/**
	 * @see com.indago.fg.FactorGraph#getFactors()
	 */
	@Override
	public Collection< ? extends Factor > getFactors() {
		return factors;
	}

	/**
	 * @see com.indago.fg.FactorGraph#getFunctions()
	 */
	@Override
	public Collection< ? extends Function > getFunctions() {
		return functions;
	}

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

		for ( final Factor factor : transFG.getFactorGraph().getFactors() ) {
			final BooleanVariable factorIndicatorVariable = ( BooleanVariable ) factor.getVariables().get( 0 );
			if ( factor instanceof MappingFactor ) {
				( ( SegmentHypothesisVariable< Segment > ) factor.getVariables().get( 1 ) )
						.addRightNeighbor( factorIndicatorVariable );
				( ( SegmentHypothesisVariable< Segment > ) factor.getVariables().get( 2 ) )
						.addLeftNeighbor( factorIndicatorVariable );
			} else if ( factor instanceof DivisionFactor ) {
				( ( SegmentHypothesisVariable< Segment > ) factor.getVariables().get( 1 ) )
						.addRightNeighbor( factorIndicatorVariable );
				( ( SegmentHypothesisVariable< Segment > ) factor.getVariables().get( 2 ) )
						.addLeftNeighbor( factorIndicatorVariable );
				( ( SegmentHypothesisVariable< Segment > ) factor.getVariables().get( 3 ) )
						.addLeftNeighbor( factorIndicatorVariable );
			} else if ( factor instanceof AppearanceFactor ) {
				( ( SegmentHypothesisVariable< Segment > ) factor.getVariables().get( 1 ) )
						.addLeftNeighbor( factorIndicatorVariable );
			} else if ( factor instanceof DisappearanceFactor ) {
				( ( SegmentHypothesisVariable< Segment > ) factor.getVariables().get( 1 ) )
						.addRightNeighbor( factorIndicatorVariable );
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

	private void mergeFG( final FactorGraphPlus< Segment > fg ) {
		this.variables.addAll( fg.getFactorGraph().getVariables() );
		this.factors.addAll( fg.getFactorGraph().getFactors() );
		this.functions.addAll( fg.getFactorGraph().getFunctions() );
	}

	/**
	 * Add constraint requesting that all right adjacent assignment variables
	 * sum to the same value as the segmentation variable.
	 *
	 * @param frameFG
	 *            a frame FG
	 */
	private void addRightContinuationConstraints( final FactorGraphPlus< Segment > frameFG ) {
		int numRightContinuationConstraints = 0;

		for ( final SegmentHypothesisVariable< Segment > segVar : frameFG.getSegmentVariables() ) {
			final Factor factor = Factors.firstEqualsSumOfOthersConstraint( segVar.getRightNeighbors() );
			numRightContinuationConstraints++;
		}
		Tr2dLog.log.trace(
				String.format(
						"\n\t\tRight Continuation Constraints added: %d",
						numRightContinuationConstraints ) );
	}

	/**
	 * Add constraint requesting that all left adjacent assignment variables
	 * sum to the same value as the segmentation variable.
	 *
	 * @param frameFG
	 *            a frame FG
	 */
	private void addLeftContinuationConstraints( final FactorGraphPlus< Segment > frameFG ) {
		int numLeftContinuationConstraints = 0;

		for ( final SegmentHypothesisVariable< Segment > segVar : frameFG.getSegmentVariables() ) {
			final Factor factor = Factors.firstEqualsSumOfOthersConstraint( segVar.getLeftNeighbors() );
			numLeftContinuationConstraints++;
		}
		Tr2dLog.log.trace(
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
