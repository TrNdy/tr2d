/**
 *
 */
package com.indago.tr2d.fg.constraints;

import com.indago.old_fg.domain.BooleanFunctionDomain;
import com.indago.old_fg.function.BooleanWeightedIndexSumConstraint;
import com.indago.old_fg.function.WeightedIndexSumConstraint.Relation;

/**
 * @author jug
 */
public class BooleanAssignmentConstraint extends BooleanWeightedIndexSumConstraint {

	/**
	 * @param mappingConstraintDomain
	 */
	public BooleanAssignmentConstraint( final BooleanFunctionDomain mappingConstraintDomain ) {
		// Encode the constraint: ((D-1) * c_1) - c_2 - ... - c_D <= 0
		super( new double[ mappingConstraintDomain.numDimensions() ], Relation.LE, 0 );
		this.getCoefficients()[ 0 ] = mappingConstraintDomain.numDimensions() - 1;
		for ( int i = 1; i < getCoefficients().length; i++ ) {
			this.getCoefficients()[ i ] = -1;
		}
	}

}
