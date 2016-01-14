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
import com.indago.segment.LabelingForest;


/**
 * @author jug
 */
public class Tr2dFactorGraphPlus implements FactorGraph {

	private final Collection< ? extends Variable< ? > > variables;
	private final Collection< ? extends Factor< ?, ?, ? > > factors;
	private final Collection< ? extends Function< ?, ? > > functions;

	// the sub factor graphs
	private final List< FactorGraph > frameFGs;
	private final List< FactorGraph > transFGs;

	// associated labeling forests
	private final List< List< LabelingForest > > perFrameLabelingForests;

	/**
	 * Creates an empty Tr2d Factor Graph object.
	 * 
	 * @param perFrameLabelingForests
	 */
	public Tr2dFactorGraphPlus(
			final FactorGraph firstFrameFG,
			final List< LabelingForest > frameLabelingForests ) {
		frameFGs = new ArrayList< FactorGraph >();
		frameFGs.add( firstFrameFG );
		transFGs = new ArrayList< FactorGraph >();

		variables = new ArrayList< BooleanVariable >();
		factors = new ArrayList< Factor< ?, ?, ? > >();
		functions = new ArrayList< Function< ?, ? > >();

		perFrameLabelingForests = new ArrayList< List< LabelingForest > >();
		perFrameLabelingForests.add( frameLabelingForests );
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
			final FactorGraph transFG,
			final FactorGraph frameFG,
			final List< LabelingForest > frameLabelingForests ) {
		transFGs.add( transFG );
		frameFGs.add( frameFG );
		this.perFrameLabelingForests.add( frameLabelingForests );
	}
}
