/**
 *
 */
package com.indago.data;


/**
 * @author jug
 */
public class Pixel2D< T > extends Point2D {

	private T data;


	public Pixel2D( final double x, final double y, final T data ) {
		super( x, y );
		this.data = data;
	}

	/**
	 * @param p
	 */
	public Pixel2D( final Pixel2D< T > p ) {
		super( p );
		this.data = p.data;
	}

	/**
	 * @return the data
	 */
	public T getData() {
		return data;
	}

	/**
	 * @param data
	 *            the data to set
	 */
	public void setData( final T data ) {
		this.data = data;
	}
}
