/**
 *
 */
package com.indago.data;

import java.util.ArrayList;
import java.util.List;

import Jama.Matrix;

/**
 * @author jug
 */
public class PointCloud1D {

	private final List< Double > points;

	public PointCloud1D() {
		this.points = new ArrayList< Double >();
	}

	public PointCloud1D( final List< Double > points ) {
		this.points = points;
	}


	/**
	 * @param points
	 * @return
	 */
	public static double[] getDoubleArray( final List< Double > points ) {
		final double[] doubles = new double[ points.size() ];
		for ( int i = 0; i < doubles.length; i++ ) {
			doubles[ i ] = points.get( i ).doubleValue();
		}
		return doubles;
	}

	public double[] getDoubleArray() {
		return PointCloud1D.getDoubleArray( this.points );
	}

	/* EXPECTED VALUE (MEAN) */

	public static double getExpectedValue( final double[] points ) {
		double e = 0.0;
		for ( int i=0; i<points.length; i++ ) {
			e += points[i];
		}
		e /= points.length;
		return e;
	}

	public static Double getExpectedValue( final List< Double > points ) {
		final double[] doubles = getDoubleArray( points );
		return new Double( PointCloud1D.getExpectedValue( doubles ) );
	}

	public Double getExpectedValue() {
		return PointCloud1D.getExpectedValue( this.points );
	}

	/* VARIANCE */

	public static double getVariance( final double[] points ) {
		final double e = PointCloud1D.getExpectedValue( points );
		double v = 0.0;
		for ( int i = 0; i < points.length; i++ ) {
			v += ( points[ i ] - e ) * ( points[ i ] - e );
		}
		v /= ( points.length - 1 );
		return v;
	}

	public static Double getVarience( final List< Double > points ) {
		final double[] doubles = getDoubleArray( points );
		return new Double( PointCloud1D.getVariance( doubles ) );
	}

	public Double getVariance() {
		return PointCloud1D.getVarience( this.points );
	}

	/* COVARIANCE MATRIX */

	public static Matrix getCovarianceMatrix( final double[] points1, final double[] points2 ) {
		assert ( points1.length == points2.length );

		final double e1 = PointCloud1D.getExpectedValue( points1 );
		final double e2 = PointCloud1D.getExpectedValue( points2 );

		final double[][] cov = new double[][] { { 0, 0 }, { 0, 0 } };
		for ( int i = 0; i < points1.length; i++ ) {
			cov[ 0 ][ 0 ] += ( points1[ i ] - e1 ) * ( points1[ i ] - e1 );
			cov[ 0 ][ 1 ] += ( points1[ i ] - e1 ) * ( points2[ i ] - e2 );
			cov[ 1 ][ 0 ] += ( points2[ i ] - e2 ) * ( points1[ i ] - e1 );
			cov[ 1 ][ 1 ] += ( points2[ i ] - e2 ) * ( points2[ i ] - e2 );
		}
		cov[ 0 ][ 0 ] /= points1.length - 1;
		cov[ 0 ][ 1 ] /= points1.length - 1;
		cov[ 1 ][ 0 ] /= points1.length - 1;
		cov[ 1 ][ 1 ] /= points1.length - 1;

//		final double[][] forCov = new double[ points1.length ][ 2 ];
//		for ( int i = 0; i < points1.length; i++ ) {
//			forCov[ i ][ 0 ] = points1[ i ];
//			forCov[ i ][ 1 ] = points2[ i ];
//		}
//		final Covariance c = new Covariance( forCov );
//		final double[][] cov2 = c.getCovarianceMatrix().getData();

		return new Matrix( cov );
	}

	public static Matrix getCovarienceMatrix( final List< Double > points1, final List< Double > points2 ) {
		final double[] doubles1 = getDoubleArray( points1 );
		final double[] doubles2 = getDoubleArray( points2 );
		return PointCloud1D.getCovarianceMatrix( doubles1, doubles2 );
	}

	public Matrix getCovarianceMatrix( final List< Double > points ) {
		return PointCloud1D.getCovarienceMatrix( this.points, points );
	}

	public Matrix getCovarianceMatrix( final PointCloud1D cloud ) {
		return PointCloud1D.getCovarienceMatrix( this.points, cloud.points );
	}

	public void add( final double d ) {
		points.add( new Double( d ) );
	}

	public void add( final Double d ) {
		points.add( d );
	}
}
