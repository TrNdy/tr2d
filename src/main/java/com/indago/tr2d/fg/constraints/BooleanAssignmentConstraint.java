/**
 *
 */
package com.indago.tr2d.fg.constraints;

import com.indago.fg.BooleanWeightedIndexSumConstraint;
import com.indago.fg.Relation;

/**
 * @author jug
 */
public class BooleanAssignmentConstraint extends BooleanWeightedIndexSumConstraint {

	public BooleanAssignmentConstraint( final int dimensions ) {
		// Encode the constraint: ((D-1) * c_1) - c_2 - ... - c_D <= 0
		super( new double[ dimensions ], Relation.LE, 0 );
		this.getCoefficients()[ 0 ] = dimensions - 1;
		for ( int i = 1; i < getCoefficients().length; i++ ) {
			this.getCoefficients()[ i ] = -1;
		}
	}

}
