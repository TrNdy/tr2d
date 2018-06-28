/**
 *
 */
package com.indago.data;


/**
 * @author jug
 */
public class Point2D {

	private double x;
	private double y;

	/**
	 * Creates the point (0,0).
	 */
	public Point2D() {
		this( 0, 0 );
	}

	public Point2D( final double x, final double y ) {
		this.x = x;
		this.y = y;
	}

	public Point2D( final Point2D p ) {
		this( p.getX(), p.getY() );
	}

	public double getX() {
		return x;
	}

	public void setX( final double x ) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY( final double y ) {
		this.y = y;
	}

	/**
	 * Rotates this point around a given center.
	 *
	 * @param center
	 *            the center-point of the rotation.
	 * @param angle
	 *            rotational angle in radiant.
	 */
	public void rotate( final Point2D center, final double angle ) {
		// Translate center to origin
		final double x = this.x - center.x;
		final double y = this.y - center.y;

		// Rotate around origin
		final double xrot = x * Math.cos( angle ) - y * Math.sin( angle );
		final double yrot = x * Math.sin( angle ) + y * Math.cos( angle );

		// Translate back
		this.x = xrot + center.x;
		this.y = yrot + center.y;
	}

	public void add( final Point2D p ) {
		this.x += p.x;
		this.y += p.y;
	}

	public void multiply( final double d ) {
		this.x *= d;
		this.y *= d;
	}

	public static double distance( final Point2D p1, final Point2D p2 ) {
		return Math.sqrt( ( p1.x - p2.x ) * ( p1.x - p2.x ) + ( p1.y - p2.y ) * ( p1.y - p2.y ) );
	}

	public static double distanceSq( final Point2D p1, final Point2D p2 ) {
		return ( p1.x - p2.x ) * ( p1.x - p2.x ) + ( p1.y - p2.y ) * ( p1.y - p2.y );
	}
}
