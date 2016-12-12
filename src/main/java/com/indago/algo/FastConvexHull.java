/**
 *
 */
package com.indago.algo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.indago.data.Pixel2D;
import com.indago.data.PixelCloud2D;

/**
 * @author jug
 */
public class FastConvexHull< T > {

	public PixelCloud2D< T > execute( final PixelCloud2D< T > cloud ) {
		final List< Pixel2D< T >> xSorted = new PixelCloud2D< T >( cloud.getPoints() ).getPoints();
		Collections.sort( xSorted, new XCompare() );

		final int n = xSorted.size();

		final List<Pixel2D<T>> lUpper = new ArrayList<Pixel2D<T>>(n);
		for ( int i = 0; i < n; i++ ) {
			lUpper.add( new Pixel2D< T >( cloud.getPoints().get( 0 ) ) );
		}

		lUpper.set( 0, xSorted.get( 0 ) );
		lUpper.set( 1, xSorted.get( 1 ) );

		int lUpperSize = 2;

		for ( int i = 2; i < n; i++ ) {
			lUpper.set( lUpperSize, xSorted.get( i ));
			lUpperSize++;

			while ( lUpperSize > 2 && !rightTurn( lUpper.get( lUpperSize - 3 ), lUpper.get( lUpperSize - 2 ), lUpper.get( lUpperSize - 1 ) ) ) {
				// Remove the middle point of the three last
				lUpper.set( lUpperSize - 2, lUpper.get( lUpperSize - 1 ) );
				lUpperSize--;
			}
		}

		final List<Pixel2D<T>> lLower = new ArrayList<Pixel2D<T>>(n);
		for ( int i = 0; i < n; i++ ) {
			lLower.add( new Pixel2D< T >( cloud.getPoints().get( 0 ) ) );
		}

		lLower.set( 0, xSorted.get( n - 1 ));
		lLower.set( 1, xSorted.get( n - 2 ));

		int lLowerSize = 2;

		for ( int i = n - 3; i >= 0; i-- ) {
			lLower.set( lLowerSize, xSorted.get( i ));
			lLowerSize++;

			while ( lLowerSize > 2 && !rightTurn( lLower.get( lLowerSize - 3 ), lLower.get( lLowerSize - 2 ), lLower.get( lLowerSize - 1 ) ) ) {
				// Remove the middle point of the three last
				lLower.set( lLowerSize - 2, lLower.get( lLowerSize - 1 ) );
				lLowerSize--;
			}
		}

		final PixelCloud2D< T > result = new PixelCloud2D< T >();

		for ( int i = 0; i < lUpperSize; i++ ) {
			result.addPoint( lUpper.get( i ) );
		}

		for ( int i = 1; i < lLowerSize - 1; i++ ) {
			result.addPoint ( lLower.get( i ) );
		}

		return result;
	}

	private boolean rightTurn( final Pixel2D<T> a, final Pixel2D<T> b, final Pixel2D<T> c ) {
		return ( b.getX() - a.getX() ) * ( c.getY() - a.getY() ) - ( b.getY() - a.getY() ) * ( c.getX() - a.getX() ) > 0;
	}

	private class XCompare implements Comparator< Pixel2D<T> > {

		@Override
		public int compare( final Pixel2D<T> o1, final Pixel2D<T> o2 ) {
			return ( new Double( o1.getX() ) ).compareTo( new Double( o2.getX() ) );
		}
	}
}
