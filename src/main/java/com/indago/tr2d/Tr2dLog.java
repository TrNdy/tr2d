package com.indago.tr2d;
/**
 *
 */

import com.indago.IndagoLog;
import org.scijava.log.Logger;

/**
 * @author jug
 */
public class Tr2dLog {

	public static Logger log = IndagoLog.stderrLogger().subLogger("tr2d");
	public static Logger solverlog = IndagoLog.stderrLogger().subLogger("gurobi");

}
