/**
 *
 */
package com.indago.tr2d.fg.factor;

import java.util.Arrays;

import com.indago.fg.Factor;
import com.indago.fg.Function;
import com.indago.fg.Variable;

/**
 * @author jug
 */
public class AppearanceFactor extends Factor {

	public AppearanceFactor( final Function function, final Variable... variables ) {
		super( function, Arrays.asList( variables ) );
	}

}
