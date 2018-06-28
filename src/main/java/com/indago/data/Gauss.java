/**
 *
 */
package com.indago.data;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.util.ValuePair;

/**
 * @author jug
 */
public class Gauss {

	private final List< Double > data_x;
	private final List< Double > data_y;
	private final List< Double > data_w;

	public Gauss() {
		data_x = new ArrayList< Double >();
		data_y = new ArrayList< Double >();
		data_w = new ArrayList< Double >();
	}

	public void addDatapoint( final double x, final double y, final double weight ) {
		data_x.add( new Double( x ) );
		data_y.add( new Double( y ) );
		data_w.add( new Double( weight ) );
	}

	public double getSigma( final boolean weighted ) {
		double sum = 0;
		final ValuePair< Double, Double > mu = getMu( weighted );
		final double mu_x = mu.getA().doubleValue();
		final double mu_y = mu.getB().doubleValue();

		for ( int i = 0; i < data_x.size(); i++ ) {
			final double diff_x = ( data_x.get( i ) - mu_x );
			final double diff_y = ( data_y.get( i ) - mu_y );
			if ( weighted ) {
				sum += ( diff_x * diff_x + diff_y * diff_y ) * data_w.get( i );
			} else {
				sum += diff_x * diff_x + diff_y * diff_y;
			}
		}

		final int n = data_x.size();
		return sum / ( n - 1 );
	}

	public double getVariance( final boolean weighted ) {
		final double sigma = getSigma( weighted );
		return sigma * sigma;
	}

	public ValuePair< Double, Double > getMu( final boolean weighted ) {
		double mu_x = 0;
		double mu_y = 0;

		for ( int i = 0; i < data_x.size(); i++ ) {
			if ( weighted ) {
				mu_x += data_x.get( i ).doubleValue() * data_w.get( i );
				mu_y += data_y.get( i ).doubleValue() * data_w.get( i );
			} else {
				mu_x += data_x.get( i ).doubleValue();
				mu_y += data_y.get( i ).doubleValue();
			}
		}

		final int n = data_x.size();
		return new ValuePair< Double, Double >( new Double( mu_x / n ), new Double( mu_y / n ) );
	}
}
