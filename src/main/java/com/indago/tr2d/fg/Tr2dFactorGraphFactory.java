/**
 *
 */
package com.indago.tr2d.fg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import com.indago.fg.CostsFactory;
import com.indago.fg.DefaultFactorGraph;
import com.indago.fg.domain.BooleanFunctionDomain;
import com.indago.fg.factor.BooleanFactor;
import com.indago.fg.factor.Factor;
import com.indago.fg.function.BooleanTensorTable;
import com.indago.fg.function.Function;
import com.indago.fg.variable.BooleanVariable;
import com.indago.segment.Segment;
import com.indago.segment.fg.FactorGraphPlus;
import com.indago.segment.fg.SegmentHypothesisVariable;
import com.indago.tr2d.fg.constraints.BooleanAssignmentConstraint;
import com.indago.tr2d.fg.factor.AppearanceFactor;
import com.indago.tr2d.fg.factor.DisappearanceFactor;
import com.indago.tr2d.fg.factor.DivisionFactor;
import com.indago.tr2d.fg.factor.MappingFactor;
import com.indago.tr2d.fg.variables.AppearanceHypothesisVariable;
import com.indago.tr2d.fg.variables.DisappearanceHypothesisVariable;
import com.indago.tr2d.fg.variables.DivisionHypothesisVariable;
import com.indago.tr2d.fg.variables.MovementHypothesisVariable;

import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;

/**
 * @author jug
 */
public class Tr2dFactorGraphFactory {

	private Tr2dFactorGraphPlus fgp;

	final BooleanFunctionDomain unaryDomain = new BooleanFunctionDomain( 1 );
	final BooleanFunctionDomain mappingConstraintDomain = new BooleanFunctionDomain( 3 );
	final BooleanFunctionDomain divisionConstraintDomain = new BooleanFunctionDomain( 4 );
	final BooleanFunctionDomain appdisappConstraintDomain = new BooleanFunctionDomain( 2 );

	final BooleanAssignmentConstraint mappingConstraint =
			new BooleanAssignmentConstraint( mappingConstraintDomain );
	final BooleanAssignmentConstraint divisionConstraint =
			new BooleanAssignmentConstraint( divisionConstraintDomain );
	final BooleanAssignmentConstraint appdisappConstraint =
			new BooleanAssignmentConstraint( appdisappConstraintDomain );

	private ArrayList< Function< ?, ? > > functions;
	private ArrayList< Factor< ?, ?, ? > > factors;
	private ArrayList< BooleanVariable > variables;

	private Collection< SegmentHypothesisVariable< Segment > > segVarsSource;
	private Collection< SegmentHypothesisVariable< Segment > > segVarsDest;

	private CostsFactory< Pair< Segment, Segment > > mappingCosts;
	private double maxMappingCost;
	private CostsFactory< Pair< Segment, Pair< Segment, Segment > > > divisionCosts;
	private double maxDivisionCost;
	private CostsFactory< Segment > appearanceCosts;
	private double maxAppearanceCost;
	private CostsFactory< Segment > disappearanceCosts;
	private double maxDisappearanceCost;


	/**
	 *
	 * @param segVarSource
	 * @param segVarDest
	 * @param mappingCosts
	 * @param maxMappingCost
	 * @param divisionCosts
	 * @param maxDivisionCost
	 * @param appearanceCosts
	 * @param maxAppearanceCost
	 * @param disappearanceCosts
	 * @param maxDisappearanceCost
	 * @return
	 */
	public FactorGraphPlus< Segment > createTransitionGraph(
			final Tr2dFactorGraphPlus fgp,
			final Collection< SegmentHypothesisVariable< Segment > > segVarSource,
			final Collection< SegmentHypothesisVariable< Segment > > segVarDest,
			final CostsFactory< Pair< Segment, Segment > > mappingCosts,
			final double maxMappingCost,
			final CostsFactory< Pair< Segment, Pair< Segment, Segment > > > divisionCosts,
			final double maxDivisionCost,
			final CostsFactory< Segment > appearanceCosts,
			final double maxAppearanceCost,
			final CostsFactory< Segment > disappearanceCosts,
			final double maxDisappearanceCost ) {

		this.fgp = fgp;

		functions = new ArrayList< >();
		factors = new ArrayList< >();
		variables = new ArrayList< BooleanVariable >();

		functions.add( mappingConstraint );
		fgp.consumeNextFunctionId();
		functions.add( divisionConstraint );
		fgp.consumeNextFunctionId();
		functions.add( appdisappConstraint );
		fgp.consumeNextFunctionId();

		this.segVarsSource = segVarSource;
		this.segVarsDest = segVarDest;

		this.mappingCosts = mappingCosts;
		this.maxMappingCost = maxMappingCost;
		this.divisionCosts = divisionCosts;
		this.maxDivisionCost = maxDivisionCost;
		this.appearanceCosts = appearanceCosts;
		this.maxAppearanceCost = maxAppearanceCost;
		this.disappearanceCosts = disappearanceCosts;
		this.maxDisappearanceCost = maxDisappearanceCost;

		// Add variables AND set up segment to variable dictionary
		final HashMap< Segment, SegmentHypothesisVariable< Segment > > segmentVariableDict =
				new HashMap< >( segVarSource.size() + segVarDest.size() );
		for ( final SegmentHypothesisVariable< Segment > segVar : segVarSource ) {
			final Segment segment = segVar.getSegment();
			segmentVariableDict.put( segment, segVar );
		}
		for ( final SegmentHypothesisVariable< Segment > segVar : segVarDest ) {
			final Segment segment = segVar.getSegment();
			segmentVariableDict.put( segment, segVar );
		}

		// Add Functions and Factors
		// =========================
		addMappingAssignments();
		addDivisionAssignments();
		addAppearanceAssignments();
		addDisappearanceAssignments();

		return new FactorGraphPlus< Segment >( new DefaultFactorGraph( variables, factors, functions ), segmentVariableDict );
	}

	/**
	 *
	 */
	private int addMappingAssignments() {
		int numMappingsAdded = 0;

		for ( final SegmentHypothesisVariable< Segment > sourceVar : segVarsSource ) {
			for ( final SegmentHypothesisVariable< Segment > destVar : segVarsDest ) {
				final double cost =
						mappingCosts.getCost(
								new ValuePair< Segment, Segment >(
										sourceVar.getSegment(),
										destVar.getSegment() ) );
				if ( cost <= maxMappingCost ) {

					// create mapping variable
					final MovementHypothesisVariable< Segment, SegmentHypothesisVariable< Segment > > newMappingVariable =
							new MovementHypothesisVariable< >( sourceVar, destVar );
					variables.add( newMappingVariable );

					// add unary mapping factor
					final double[] entries = new double[] { 0.0, cost };
					final BooleanTensorTable btt =
							new BooleanTensorTable( unaryDomain, entries, fgp.consumeNextFunctionId() );
					BooleanFactor factor =
							new BooleanFactor( unaryDomain, fgp.consumeNextFactorId() );
					factor.setFunction( btt );
					factor.setVariable( 0, newMappingVariable );
					functions.add( btt );
					factors.add( factor );

					// add mapping constraint (function added in constructor!)
					factor = new MappingFactor( mappingConstraintDomain, fgp
							.consumeNextFactorId() );
					factor.setFunction( mappingConstraint );
					factor.setVariable( 0, newMappingVariable );
					factor.setVariable( 1, sourceVar );
					factor.setVariable( 2, destVar );
					factors.add( factor );

					numMappingsAdded++;
				}
			}
		}
		System.out.println( String.format( "\n\t\tMappings added: %d", numMappingsAdded ) );
		return numMappingsAdded;
	}

	/**
	 *
	 */
	private int addDivisionAssignments() {
		int numDivisionsAdded = 0;

		for ( final SegmentHypothesisVariable< Segment > sourceVar : segVarsSource ) {
			for ( final SegmentHypothesisVariable< Segment > destVar1 : segVarsDest ) {
				for ( final SegmentHypothesisVariable< Segment > destVar2 : segVarsDest ) {

					//Avoid double enumeration of pairs...
					if ( destVar1.hashCode() > destVar2.hashCode() ) continue;

					final double cost =
							divisionCosts.getCost(
									new ValuePair< Segment, Pair< Segment, Segment > >(
											sourceVar.getSegment(),
											new ValuePair< Segment, Segment > (
													destVar1.getSegment(),
													destVar2.getSegment() ) ) );
					if ( cost <= maxDivisionCost ) {

						// create division variable
						final DivisionHypothesisVariable< Segment, SegmentHypothesisVariable< Segment > > newDivisionVariable =
								new DivisionHypothesisVariable< >( sourceVar, destVar1, destVar2 );
						variables.add( newDivisionVariable );

						// add unary division factor
						final double[] entries = new double[] { 0.0, cost };
						final BooleanTensorTable btt =
								new BooleanTensorTable( unaryDomain, entries, fgp
										.consumeNextFunctionId() );
						BooleanFactor factor =
								new BooleanFactor( unaryDomain, fgp.consumeNextFactorId() );
						factor.setFunction( btt );
						factor.setVariable( 0, newDivisionVariable );
						functions.add( btt );
						factors.add( factor );

						// add mapping constraint (function added in constructor!)
						factor = new DivisionFactor( divisionConstraintDomain, fgp
								.consumeNextFactorId() );
						factor.setFunction( divisionConstraint );
						factor.setVariable( 0, newDivisionVariable );
						factor.setVariable( 1, sourceVar );
						factor.setVariable( 2, destVar1 );
						factor.setVariable( 3, destVar2 );
						factors.add( factor );

						numDivisionsAdded++;
					}
				}
			}
		}
		System.out.println( String.format( "\t\tDivisions added: %d", numDivisionsAdded ) );
		return numDivisionsAdded;
	}

	/**
	 *
	 */
	private int addAppearanceAssignments() {
		int numAppAdded = 0;

		for ( final SegmentHypothesisVariable< Segment > destVar : segVarsDest ) {
			final double cost = appearanceCosts.getCost( destVar.getSegment() );
			if ( cost <= maxAppearanceCost ) {

				// create appearance variable
				final AppearanceHypothesisVariable< Segment, SegmentHypothesisVariable< Segment > > newAppearanceVariable =
						new AppearanceHypothesisVariable< >( destVar );
				variables.add( newAppearanceVariable );

				// add unary appearance factor
				final double[] entries = new double[] { 0.0, cost };
				final BooleanTensorTable btt =
						new BooleanTensorTable( unaryDomain, entries, fgp.consumeNextFunctionId() );
				BooleanFactor factor = new BooleanFactor( unaryDomain, fgp.consumeNextFactorId() );
				factor.setFunction( btt );
				factor.setVariable( 0, newAppearanceVariable );
				functions.add( btt );
				factors.add( factor );

				// add appearance constraint (function added in constructor!)
				factor = new AppearanceFactor( appdisappConstraintDomain, fgp
						.consumeNextFactorId() );
				factor.setFunction( appdisappConstraint );
				factor.setVariable( 0, newAppearanceVariable );
				factor.setVariable( 1, destVar );
				factors.add( factor );

				numAppAdded++;
			}
		}
		System.out.println( String.format( "\t\tApps added: %d", numAppAdded ) );
		return numAppAdded;
	}

	/**
	 *
	 */
	private int addDisappearanceAssignments() {
		int numDisappAdded = 0;

		for ( final SegmentHypothesisVariable< Segment > sourceVar : segVarsSource ) {
			final double cost = disappearanceCosts.getCost( sourceVar.getSegment() );
			if ( cost <= maxDisappearanceCost ) {

				// create disappearance variable
				final DisappearanceHypothesisVariable< Segment, SegmentHypothesisVariable< Segment > > newDisappearanceVariable =
						new DisappearanceHypothesisVariable< >( sourceVar );
				variables.add( newDisappearanceVariable );

				// add unary appearance factor
				final double[] entries = new double[] { 0.0, cost };
				final BooleanTensorTable btt =
						new BooleanTensorTable( unaryDomain, entries, fgp.consumeNextFunctionId() );
				BooleanFactor factor = new BooleanFactor( unaryDomain, fgp.consumeNextFactorId() );
				factor.setFunction( btt );
				factor.setVariable( 0, newDisappearanceVariable );
				functions.add( btt );
				factors.add( factor );

				// add appearance constraint (function added in constructor!)
				factor = new DisappearanceFactor( appdisappConstraintDomain, fgp
						.consumeNextFactorId() );
				factor.setFunction( appdisappConstraint );
				factor.setVariable( 0, newDisappearanceVariable );
				factor.setVariable( 1, sourceVar );
				factors.add( factor );

				numDisappAdded++;
			}
		}
		System.out.println( String.format( "\t\tDisapps added: %d", numDisappAdded ) );
		return numDisappAdded;
	}

}
