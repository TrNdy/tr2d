/**
 *
 */
package com.indago.data;


/**
 * @author jug
 */
public class Circle {

	private Point2D center;
	private double radius;

	/**
	 * Creates an non-rotated Ellipse2D at (0,0) with a and be being set to 0.
	 */
	public Circle() {
		this( new Point2D(), 0.0 );
	}

	/**
	 * Creates an Ellipse2D.
	 *
	 * @param offset
	 *            a vector pointing from the origin to the center of the
	 *            ellipse.
	 * @param radius
	 *            counter-clockwise rotation fo the ellipse -- given in rad!
	 */
	public Circle( final Point2D offset, final double radius ) {
		this.setCenter( offset );
		this.setRadius( radius );
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
	public double getRadius() {
		return radius;
	}

	/**
	 * @param angle
	 *            the angle to set
	 */
	public void setRadius( final double angle ) {
		this.radius = angle;
	}

	public boolean isZeroCircle() {
		return ( this.radius == 0.0 );
	}

	public Rectangle2D getBoundingBox() {
		return new Rectangle2D( this.center.getX()-this.radius, this.center.getY()-this.radius, 2*this.radius, 2*this.radius );
	}

	public boolean contains( final Point2D p ) {
		return ( Point2D.distance( this.center, p ) <= this.radius );
	}
}
