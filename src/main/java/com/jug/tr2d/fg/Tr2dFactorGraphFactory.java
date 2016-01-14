/**
 *
 */
package com.jug.tr2d.fg;

import java.util.ArrayList;
import java.util.HashMap;

import com.indago.fg.CostsFactory;
import com.indago.fg.DefaultFactorGraph;
import com.indago.fg.factor.Factor;
import com.indago.fg.function.Function;
import com.indago.segment.Segment;
import com.indago.segment.fg.FactorGraphPlus;
import com.indago.segment.fg.SegmentHypothesisVariable;

import net.imglib2.util.Pair;

/**
 * @author jug
 */
public class Tr2dFactorGraphFactory {

	/**
	 * @param fgPlus
	 * @param segments
	 * @param transCosts
	 * @return
	 */

	public static < T extends Segment > FactorGraphPlus< T > createTrackingTransitionGraph(
			final ArrayList< T > segmentsSource,
			final ArrayList< T > segmentsDest,
			final CostsFactory< Pair< T, T > > transCosts ) {

		final int factorId = 0;
		final int functionId = 0;

		final ArrayList< Function< ?, ? > > functions = new ArrayList< >();
		final ArrayList< Factor< ?, ?, ? > > factors = new ArrayList< >();
		final ArrayList< SegmentHypothesisVariable< T > > variables = new ArrayList< >();

		// Add variables AND set up segment to variable dictionary
		final HashMap< T, SegmentHypothesisVariable< T > > segmentVariableDict =
				new HashMap< >( segmentsSource.size() + segmentsDest.size() );
		for ( final T segment : segmentsSource ) {
			final SegmentHypothesisVariable< T > var = new SegmentHypothesisVariable< >( segment );
			segmentVariableDict.put( segment, var );
			variables.add( var );
		}
		for ( final T segment : segmentsDest ) {
			final SegmentHypothesisVariable< T > var = new SegmentHypothesisVariable< >( segment );
			segmentVariableDict.put( segment, var );
			variables.add( var );
		}

		// Add Functions

		// Add Factors

		return new FactorGraphPlus< T >( new DefaultFactorGraph( variables, factors, functions ), segmentVariableDict );
	}

}
