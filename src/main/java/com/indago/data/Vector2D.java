/**
 *
 */
package com.indago.data;


/**
 * @author jug
 */
public class Vector2D extends Point2D {

	/**
	 * Creates a 2D null vector.
	 */
	public Vector2D() {
		super( 0, 0 );
	}

	public Vector2D( final double x, final double y ) {
		super( x, y );
	}

	public Vector2D( final Vector2D v ) {
		super( v );
	}

	/**
	 * @return the vector
	 */
	public double[] getVector() {
		return new double[] { super.getX(), super.getY() };
	}

	/**
	 *
	 * @param x
	 * @param y
	 */
	public void setVector( final double x, final double y ) {
		super.setX( x );
		super.setY( y );
	}

	/**
	 * @return
	 */
	public double getAngle() {
		return Math.atan2( super.getY(), super.getX() );
	}

	/**
	 *
	 */
	public void normalize() {
		if ( getLength() == 0 ) return;
		super.setX( super.getX() / getLength() );
		super.setY( super.getY() / getLength() );
	}

	/**
	 * @return the length of this Vector2D.
	 */
	public double getLength() {
		return Math.sqrt( super.getX() * super.getX() + super.getY() * super.getY() );
	}

	/**
	 * @param v
	 *            the vector that will be project onto this.
	 * @return the projection of the given vector. (Same or inverse direction as
	 *         <code>this</code>!)
	 */
	public Vector2D project( final Vector2D v ) {
		final Vector2D ret = new Vector2D( this );
		ret.normalize();
		final double proj_length = this.dot( v ) / this.getLength();
		ret.multiply( proj_length );
		return ret;
	}

	/**
	 * A.dot(B) = ||A|| * ||B|| * cos(phi)
	 *
	 * @param v
	 * @return dot-product of this and given vector v.
	 */
	public double dot( final Vector2D v ) {
		return getX() * v.getX() + getY() * v.getY();
	}
}
