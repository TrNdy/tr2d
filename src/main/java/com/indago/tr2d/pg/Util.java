/**
 *
 */
package com.indago.tr2d.pg;

import java.util.Comparator;

import com.indago.pg.assignments.DivisionHypothesis;
import com.indago.pg.assignments.MovementHypothesis;

/**
 * @author jug
 */
public class Util {

	private static Comparator< DivisionHypothesis > compDivHyp = new Comparator< DivisionHypothesis >() {

		@Override
		public int compare( final DivisionHypothesis o1, final DivisionHypothesis o2 ) {
			return ( int ) Math.signum( o1.getCost() - o2.getCost() );
		}
	};

	public static Comparator< DivisionHypothesis > getCostComparatorForDivisionHypothesis() {
		return compDivHyp;
	}

	private static Comparator< MovementHypothesis > compMoveHyp = new Comparator< MovementHypothesis >() {

		@Override
		public int compare( final MovementHypothesis o1, final MovementHypothesis o2 ) {
			return ( int ) Math.signum( o1.getCost() - o2.getCost() );
		}
	};

	public static Comparator< MovementHypothesis > getCostComparatorForMovementHypothesis() {
		return compMoveHyp;
	}
}
