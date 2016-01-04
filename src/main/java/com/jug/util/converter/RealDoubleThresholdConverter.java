/**
 *
 */
package com.jug.util.converter;

import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;

/**
 * Converts any {@link RealType} to a {@link DoubleType} and divides by the
 * given number.
 *
 * If the input type is complex, it loses the imaginary part without complaining
 * further.
 *
 * @author Jug
 */
public class RealDoubleThresholdConverter< R extends RealType< R > > implements Converter< R, DoubleType > {

	double threshold;

	public RealDoubleThresholdConverter() {
		this( .5 );
	}

	public RealDoubleThresholdConverter( final double threshold ) {
		this.threshold = threshold;
	}

	@Override
	public void convert( final R input, final DoubleType output ) {
		output.set( ( input.getRealDouble() > threshold ) ? 1.0 : 0.0 );
	}
}
