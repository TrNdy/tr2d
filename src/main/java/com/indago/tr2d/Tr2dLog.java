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

	public static Logger log = IndagoLog.stdLogger().subLogger( "tr2d" );
	public static Logger solverlog = log.subLogger( "sol" );
	public static Logger segmenterLog = log.subLogger( "seg" );

}
