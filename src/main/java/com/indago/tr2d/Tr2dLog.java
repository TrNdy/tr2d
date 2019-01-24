package com.indago.tr2d;
/**
 *
 */

import org.scijava.log.Logger;

import com.indago.IndagoLog;

/**
 * @author jug
 */
public class Tr2dLog {

	public static Logger log = IndagoLog.log.subLogger( "tr2d" );
	public static Logger solverlog = IndagoLog.log.subLogger( "gurobi" );
	public static Logger segmenterLog = IndagoLog.log.subLogger( "seg" );

}
