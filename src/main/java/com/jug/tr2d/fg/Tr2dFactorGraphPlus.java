/**
 *
 */
package com.jug.tr2d.fg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.indago.fg.FactorGraph;
import com.indago.fg.factor.Factor;
import com.indago.fg.function.Function;
import com.indago.fg.variable.BooleanVariable;
import com.indago.fg.variable.Variable;
import com.indago.segment.fg.FactorGraphPlus;


/**
 * @author jug
 */
public class Tr2dFactorGraphPlus implements FactorGraph {

	private final Collection< ? extends Variable< ? > > variables;
	private final Collection< ? extends Factor< ?, ?, ? > > factors;
	private final Collection< ? extends Function< ?, ? > > functions;

	// the sub factor graphs
	private final List< FactorGraphPlus > frameFGs;
	private final List< FactorGraphPlus > transFGs;

	/**
	 * Creates an empty Tr2d Factor Graph object.
	 *
	 * @param perFrameLabelingForests
	 */
	public Tr2dFactorGraphPlus(
			final FactorGraphPlus firstFrameFG ) {
		frameFGs = new ArrayList< FactorGraphPlus >();
		frameFGs.add( firstFrameFG );
		transFGs = new ArrayList< FactorGraphPlus >();

		variables = new ArrayList< BooleanVariable >();
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

	public void addFrame(
			final FactorGraphPlus transFG,
			final FactorGraphPlus frameFG ) {
		transFGs.add( transFG );
		frameFGs.add( frameFG );
	}
}
