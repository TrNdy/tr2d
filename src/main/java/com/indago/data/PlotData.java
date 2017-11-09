/**
 *
 */
package com.indago.data;

import ij.IJ;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;



/**
 * @author jug
 */
public class PlotData {

	private final double[] data;

	public PlotData( final int maxX ) {
		data = new double[ maxX + 1 ];
	}

	public double[] getXData() {
		final double[] ret = new double[ data.length ];
		for ( int i = 0; i < data.length; i++ ) {
			ret[ i ] = i;
		}
		return ret;
	}

	public double[] getYData() {
		return data;
	}

	/**
	 *
	 * @param x
	 * @param y
	 */
	public void addValueToXValue( final int x, final double y ) {
		data[ x ] += y;
	}

	/**
	 *
	 * @param x
	 * @param y
	 */
	public void addValueToXValue( final double x, final double y ) {
		final int x1 = ( int ) Math.round( Math.floor( x ) );
		final int x2 = ( int ) Math.round( Math.ceil( x ) );
		final double fraq = x - x1;
		data[ x1 ] += ( 1 - fraq ) * y;
		if ( x2 < data.length ) {
			data[ x2 ] += fraq * y;
		} else {
			data[ x1 ] += fraq * y;
		}
	}

	/**
	 * @param path
	 * @param filename
	 */
	public void saveToFile( final File path, final String filename ) {
		final File file = new File( path, filename );
		try {
			final FileOutputStream fos = new FileOutputStream( file );
			final OutputStreamWriter out = new OutputStreamWriter( fos );

			for ( int i = 0; i < data.length; i++ ) {
				if ( !Double.isNaN( data[ i ] ) ) {
					out.write( String.format( "%4d, %12.5f\n", i, data[ i ] ) );
				}
			}
			out.close();
			fos.close();
		}
		catch ( final FileNotFoundException e ) {
			IJ.error( "File '" + file.getAbsolutePath() + "' could not be opened!" );
		}
		catch ( final IOException e ) {
			IJ.error( "Could not write to file '" + file.getAbsolutePath() + "'!" );
		}

	}

	/**
	 * Gets the y-value for a given x. If necessary this method would
	 * interpolate linear between the two closes existing x-values.
	 *
	 * @param x
	 * @return y-value for given x, or linear interpolation between two closest
	 *         existing x-values. If given x is out of bounds we return 0.0.
	 */
	public double getValueAt( final double x ) {
		final int x1 = ( int ) x;
		final int x2 = ( int ) ( x + 1 );
		final double f1 = x - x1;
		if ( x1 >= 0 && x2 < data.length ) {
			return data[ x1 ] * f1 + data[ x2 ] * ( 1 - f1 );
		} else {
			return 0.0;
		}
	}
}
