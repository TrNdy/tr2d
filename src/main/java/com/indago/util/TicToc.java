/**
 *
 */
package com.indago.util;


/**
 * @author jug
 */
public class TicToc {

	private boolean isTicked = false;
	private long t0 = 0;
	private long t1 = 0;

	public long tic() {
		isTicked = true;
		t0 = System.nanoTime();
		return t0;
	}

	public long tic( final String message ) {
		final long ret = tic();
		System.out.println( String.format( "t0=%d - %s", ret, message ) );
		return ret;
	}

	public long toc() {
		isTicked = false;
		t1 = System.nanoTime();
		return t1 - t0;
	}

	public long toc( final String message ) {
		final long ret = toc();
		System.out.println( String.format( "dt=%d - %s", ret, message ) );
		return ret;
	}

	public long getLatest() {
		if (!isTicked) {
			return t1-t0;
		} else {
			throw new IllegalStateException("toc() not called before trying to retrieve latest time span...");
		}
	}
}
