/**
 *
 */
package com.indago.data;


/**
 * @author jug
 */
public class Rectangle2D {

	private double x;
	private double y;
	private double width;
	private double height;

	/**
	 * Constructs an Rectancle2D at (0,0) with 0 width and 0 height.
	 */
	public Rectangle2D() {
		this( 0, 0, 0, 0 );
	}

	/**
	 * @param i
	 * @param j
	 * @param k
	 * @param l
	 */
	public Rectangle2D( final double x, final double y, final double w, final double h ) {
		this.setX( x );
		this.setY( y );
		this.setWidth( w );
		this.setHeight( h );
	}

	/**
	 * @return the x
	 */
	public double getX() {
		return x;
	}

	/**
	 * @return the y
	 */
	public double getY() {
		return y;
	}

	/**
	 * @return the width
	 */
	public double getWidth() {
		return width;
	}

	/**
	 * @return the height
	 */
	public double getHeight() {
		return height;
	}

	/**
	 * @param x
	 *            the x to set
	 */
	public void setX( final double x ) {
		this.x = x;
	}

	/**
	 * @param y
	 *            the y to set
	 */
	public void setY( final double y ) {
		this.y = y;
	}

	/**
	 * @param width
	 *            the width to set
	 */
	public void setWidth( final double width ) {
		this.width = width;
	}

	/**
	 * @param height
	 *            the height to set
	 */
	public void setHeight( final double height ) {
		this.height = height;
	}
}
