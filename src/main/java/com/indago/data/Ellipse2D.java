/**
 *
 */
package com.indago.data;


/**
 * @author jug
 */
public class Ellipse2D {

	private Point2D center;
	private double angle;
	private double a;
	private double b;

	/**
	 * Creates an non-rotated Ellipse2D at (0,0) with a and be being set to 0.
	 */
	public Ellipse2D() {
		this( new Point2D(), 0.0, 0.0, 0.0 );
	}

	/**
	 * Creates an Ellipse2D.
	 *
	 * @param offset
	 *            a vector pointing from the origin to the center of the
	 *            ellipse.
	 * @param angle
	 *            counter-clockwise rotation fo the ellipse -- given in rad!
	 * @param a
	 *            length of major axis.
	 * @param b
	 *            length of minor axis.
	 */
	public Ellipse2D( final Point2D offset, final double angle, final double a, final double b ) {
		this.setCenter( offset );
		this.setAngle( angle );
		this.setA( a );
		this.setB( b );
	}

	/**
	 * @return the center
	 */
	public Point2D getCenter() {
		return center;
	}

	/**
	 * @param center
	 *            the center to set
	 */
	public void setCenter( final Point2D center ) {
		this.center = center;
	}

	/**
	 * @return the angle
	 */
	public double getAngle() {
		return angle;
	}

	/**
	 * @param angle
	 *            the angle to set
	 */
	public void setAngle( final double angle ) {
		this.angle = angle;
	}

	/**
	 * @return the a
	 */
	public double getA() {
		return a;
	}

	/**
	 * @param a
	 *            the a to set
	 */
	public void setA( final double a ) {
		this.a = a;
	}

	/**
	 * @return the b
	 */
	public double getB() {
		return b;
	}

	/**
	 * @param b
	 *            the b to set
	 */
	public void setB( final double b ) {
		this.b = b;
	}

	/**
	 * @return a Rectangle2D that is an axis-parallel bounding box of this
	 *         ellipse.
	 */
	public Rectangle2D getBoundingBox() {
		if ( a == 0 || b == 0 ) { return new Rectangle2D( center.getX(), center.getY(), 0, 0 ); }

		final Rectangle2D ret = new Rectangle2D();

		// Equations describing rotated ellipse:
		//   x = cx + a*cos(t)*cos(angle) - b*sin(t)*sin(angle)  [1]
		//   y = cy + b*sin(t)*cos(angle) + a*cos(t)*sin(angle)  [2]
		// with (cx,cy) being the center of the ellipse, a the major axis length,
		// b the minor axis length, angle the rotation in rad, and t simply the param
		// that determines the location on the ellipse. (See this.getPointAt(t).)

		// To find the bounds in x-direction we have to find the t for which the gradient
		// in x-direction becomes 0:
		//   0 = dx/dt = -a*sin(t)*cos(angle) - b*cos(t)*sin(angle)
		//     ==>  tan(t) = -b*tan(angle)/a   [3]
		// Compute t and plug it into [1], subtract PI and plug it another time into [1].
		final double t1 = Math.atan( -this.b * Math.tan( this.angle )/this.a );
		final double t2 = t1 - Math.PI;
		double x1 = this.getPointAt( t1 ).getX();
		double x2 = this.getPointAt( t2 ).getX();
		if ( x1 > x2 ) { final double muh = x1; x1 = x2; x2 = muh; }
		ret.setX( x1 );
		ret.setWidth( x2 - x1 );

		// Same scheme for y-bounds. This time set the gradient in y-direction to 0:
		//   0 = dy/dt = b*cos(t)*cos(angle) - a*sin(t)*sin(angle)
		//     ==>  tan(t) = b*cot(angle)/a    [4]
		// Plug again t and t-PI into [2] - voilá, done!
		final double t3 = Math.atan( this.b * ( 1.0 / Math.tan( this.angle ) ) / this.a );
		final double t4 = t3 - Math.PI;
		double y1 = this.getPointAt( t3 ).getY();
		double y2 = this.getPointAt( t4 ).getY();
		if ( y1 > y2 ) { final double muh = y1; y1 = y2; y2 = muh; }
		ret.setY( y1 );
		ret.setHeight( y2 - y1 );

		return ret;
	}

	public Point2D getPointAt( final double t ) {
		// Equations describing rotated ellipse:
		//   x = cx + a*cos(t)*cos(angle) - b*sin(t)*sin(angle)  [1]
		//   y = cy + b*sin(t)*cos(angle) + a*cos(t)*sin(angle)  [2]
		// with (cx,cy) being the center of the ellipse, a the major axis length,
		// b the minor axis length, angle the rotation in rad, and t simply the param
		// that determines the location on the ellipse.
		final double cx = this.center.getX();
		final double cy = this.center.getY();
		final double x = cx + this.a * Math.cos( t ) * Math.cos( this.angle ) - this.b * Math.sin( t ) * Math.sin( this.angle );
		final double y = cy + this.b * Math.sin( t ) * Math.cos( this.angle ) + this.a * Math.cos( t ) * Math.sin( this.angle );
		return new Point2D( x, y );
	}

	/**
	 * @return true if either of the two axis length is zero.
	 */
	public boolean isZeroEllipse() {
		if ( a == 0 || b == 0 ) return true;
		return false;
	}

	public void scaleToHostFractionOfPoints( final PixelCloud2D points, final double fractionInside ) {
		if ( Double.isInfinite( a ) || Double.isInfinite( b ) ) return;
		if ( Double.isNaN( a ) || Double.isNaN( b ) ) return;
		if ( a == 0 || b == 0 ) return;

		// Idea: binary search
		double factor = 1.0;

		final double old_a = this.a;
		final double old_b = this.b;

		int inside = countPointsInside( points );
		double curFracIn = ( ( double ) inside ) / points.size();

		// If current Ellipse2D is to small --> iteratively double size until too big!
		double lower_bound = 0;
		while ( curFracIn < fractionInside ) {
			lower_bound = factor; // since we know it now, set tighter lower_bound...
			factor *= 2;
			if ( Double.isInfinite( factor ) ) {
				System.out.println( String.format( "Reset to: %f, %f", old_a, old_b ) );
				this.a = old_a;
				this.b = old_b;
				return;
			}
			this.a = old_a * factor;
			this.b = old_b * factor;
			inside = countPointsInside( points );
			curFracIn = ( ( double ) inside ) / points.size();
		}
		double upper_bound = factor;

		// Initialize binary search right between the bounds!
		factor = ( upper_bound + lower_bound ) / 2;
		this.a = old_a * factor;
		this.b = old_b * factor;
		inside = countPointsInside( points );
		curFracIn = ( ( double ) inside ) / points.size();
		double oneLessFrac = ( ( double ) inside - 1 ) / points.size();

		// while NOT DONE... ;)
		while ( curFracIn < fractionInside || oneLessFrac > fractionInside ) {
			if ( ( upper_bound - lower_bound ) < 0.00000001 ) break;

//			System.out.println( String.format( "Binary Search at: %f%% instead of %f%% with factor %f", curFracIn, fractionInside, factor ) );
			if ( curFracIn < fractionInside ) {
				lower_bound = factor;
			} else {
				upper_bound = factor;
			}
			factor = ( upper_bound + lower_bound ) / 2;
			this.a = old_a * factor;
			this.b = old_b * factor;
			inside = countPointsInside( points );
			curFracIn = ( ( double ) inside ) / points.size();
			oneLessFrac = ( ( double ) inside - 1 ) / points.size();
		}
//		System.out.println( String.format( ">> Done with: %f%% instead of %f%%", curFracIn, fractionInside ) );
	}

	public int countPointsInside( final PixelCloud2D< ? > points ) {
		int ret = 0;
		for ( final Point2D p : points.getPoints() ) {
			if ( contains( p ) ) ret++;
		}
		return ret;
	}

	public boolean contains( final Point2D p ) {
		// Idea: rotate p around this.center about this.angle.
		// Then use std-formula for non-rotated ellipses...
		final Point2D prot = new Point2D( p );
		prot.rotate( this.center, -this.angle );

		// ((x*x)/(a*a) + (y*y)/(b*b)) <= 1;
		final double distance = Math.pow( ( prot.getX() - this.center.getX() ), 2 ) / ( a * a ) + Math.pow( ( prot.getY() - this.center.getY() ), 2 ) / ( b * b );
		return distance <= 1.0;
	}
}
