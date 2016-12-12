/**
 *
 */
package com.indago.data;

import java.util.ArrayList;
import java.util.List;

import com.indago.algo.FastConvexHull;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;


/**
 * @author jug
 */
public class PixelCloud2D< T > {

	private final List< Pixel2D< T > > points;

	public PixelCloud2D() {
		points = new ArrayList< Pixel2D< T > >();
	}

	/**
	 * @param points2
	 */
	public PixelCloud2D( final List< Pixel2D< T >> cloud ) {
		points = new ArrayList< Pixel2D< T > >();
		for ( final Pixel2D< T > p : cloud ) {
			points.add( new Pixel2D< T >( p ) );
		}
	}

	public void addPoint( final double x, final double y, final T weight ) {
		final Pixel2D< T > p = new Pixel2D< T >( x, y, weight );
		addPoint( p );
	}

	/**
	 * @param p
	 * @param weight
	 */
	public void addPoint( final Pixel2D< T > p ) {
		getPoints().add( p );
	}

	/**
	 * @return
	 */
	private Matrix getCovarianceMatrix() {
		final PointCloud1D xPoints = getXVals();
		final PointCloud1D yPoints = getYVals();
		return xPoints.getCovarianceMatrix( yPoints );
	}

	/**
	 * @return
	 */
	private PointCloud1D getYVals() {
		final PointCloud1D yVals = new PointCloud1D();
		for ( final Pixel2D< T > point : this.getPoints() ) {
			yVals.add( point.getY() );
		}
		return yVals;
	}

	/**
	 * @return
	 */
	private PointCloud1D getXVals() {
		final PointCloud1D xVals = new PointCloud1D();
		for ( final Pixel2D< T > point : this.getPoints() ) {
			xVals.add( point.getX() );
		}
		return xVals;
	}

	/**
	 * @return
	 */
	public boolean isEmpty() {
		return getPoints().isEmpty();
	}

	/**
	 * @return the points
	 */
	public List< Pixel2D< T > > getPoints() {
		return points;
	}

	/**
	 * @return number of points contained in this cloud.
	 */
	public int size() {
		return points.size();
	}

	/**
	 * @return
	 */
	public Circle getCircularApproximation() {
		final Circle ret;

		if ( this.size() == 1 ) {
			ret = new Circle( this.getPoints().get( 0 ), 0.0 );
		} else if ( this.size() == 2 ) {
			final Point2D p = new Point2D( this.getPoints().get( 0 ) );
			p.add( this.getPoints().get( 1 ) );
			p.multiply( 0.5 );
			final double len = Point2D.distance( p, this.getPoints().get( 1 ) );
			ret = new Circle( p, len / 2 );
		} else {
			// determine center
			final Point2D center = new Point2D( this.getXVals().getExpectedValue(), this.getYVals().getExpectedValue() );
			final PixelCloud2D< T > hullPoints = this.getPointsOnConvexHull();
			double d = 0;
			for ( final Point2D p : hullPoints.getPoints() ) {
//				d = Math.max( d, Point2D.distance( p, center ) );
				d += Point2D.distance( p, center );
			}
			d /= hullPoints.size();
			ret = new Circle( center, d );
		}

		return ret;
	}

	/**
	 * @return
	 */
	public PixelCloud2D< T > getPointsOnConvexHull() {
		final FastConvexHull< T > fch = new FastConvexHull< T >();
		return fch.execute( this );
	}

	/**
	 * @return
	 */
	public Ellipse2D getEllipticalApproximation() {
		final Ellipse2D ret;

		if ( this.size() == 0 ) {
			ret = new Ellipse2D( new Point2D( 0, 0 ), 0.0, 0.0, 0.0 );
		} else if ( this.size() == 1 ) {
			ret = new Ellipse2D( this.getPoints().get( 0 ), 0.0, 0.0, 0.0 );
		} else if ( this.size() == 2 ) {
			final Point2D p = new Point2D( this.getPoints().get( 0 ) );
			p.add( this.getPoints().get( 1 ) );
			p.multiply( 0.5 );
			final double len = Point2D.distance( p, this.getPoints().get( 1 ) );
			ret = new Ellipse2D( p, Math.atan2( this.getPoints().get( 1 ).getY() - p.getY(), this.getPoints().get( 1 ).getX() - p.getX() ), len / 2, 0.5 );
		} else {
			// determine center
			final Point2D center = new Point2D( this.getXVals().getExpectedValue(), this.getYVals().getExpectedValue() );

			// determine first two principle components
			final Matrix covM = this.getCovarianceMatrix();
			final EigenvalueDecomposition eigen = covM.eig();
			final Matrix V = eigen.getV();
//			final Matrix D = eigen.getD();
			final double[] evs = eigen.getRealEigenvalues();

			Vector2D firstEV = new Vector2D( V.get( 0, 0 ), V.get( 1, 0 ) );
//			Vector2D secondEV = new Vector2D( V.get( 0, 1 ), V.get( 1, 1 ) );

			double a = Math.sqrt( evs[ 0 ] );
			double b = Math.sqrt( evs[ 1 ] );
			if ( a < b ) {
				final double c = a;
				a = b;
				b = c;

				firstEV = new Vector2D( V.get( 0, 1 ), V.get( 1, 1 ) );
//				secondEV = new Vector2D( V.get( 0, 0 ), V.get( 1, 0 ) );
			}

			ret = new Ellipse2D( center, firstEV.getAngle(), a, b ); //D.get( 0, 0 ), D.get( 1, 1 ) );
			ret.scaleToHostFractionOfPoints( this, .9 );
		}
		return ret;
	}

	/**
	 * Currently this needs linear running time (in the size of the cloud)! :(
	 * 
	 * @param p
	 *            Point2D
	 * @return returns the point from the cloud that is closest to p. (Null if
	 *         clouds does not contain any points!)
	 */
	public Point2D getClosestPoint( final Point2D p ) {
		double minDist = Double.MAX_VALUE;
		Point2D ret = null;

		for ( final Point2D candidate : points ) {
			if ( Point2D.distanceSq( p, candidate ) < minDist ) {
				minDist = Point2D.distanceSq( p, candidate );
				ret = candidate;
			}
		}
		return ret;
	}
}
