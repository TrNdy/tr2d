/**
 *
 */
package com.indago.app.hernan.costs;


/**
 * @author jug
 */
public class HernanCostConstants {

	public static final double TRUNCATE_COST_THRESHOLD = 10000;
	@Deprecated  // remove asap
	public static final double TRUNCATE_COST_VALUE = TRUNCATE_COST_THRESHOLD * 2;

	public static final double MAX_SQUARED_MOVEMENT_DISTANCE = 20 * 20;
	public static final double MAX_AVG_SQUARED_DIVISION_MOVE_DISTANCE = 25 * 25;
	public static final double MAX_SQUARED_DIVISION_OFFSPRING_DISTANCE = 50 * 50;
}
