/**
 *
 */
package com.jug.segmentation;

import java.util.ArrayList;
import java.util.List;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealDoubleConverter;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.real.DoubleType;
import trainableSegmentation.WekaSegmentation;

/**
 * @author jug
 */
public class SilentWekaSegmenter< T extends NumericType< T > > {

	/** reference to the segmentation backend */
	WekaSegmentation wekaSegmentation = null;

	public SilentWekaSegmenter( final String directory, final String filename ) {
		// instantiate segmentation backend
		wekaSegmentation = new WekaSegmentation( IJ.createImage( "unused dummy", 10, 5, 0, 16 ) );
		loadClassifier( directory, filename );
	}

	public boolean loadClassifier( final String directory, final String filename ) {
		// Try to load Weka model (classifier and train header)
		if ( false == wekaSegmentation.loadClassifier( directory + filename ) ) {
			IJ.error( "Error when loading Weka classifier from file: " + directory + filename );
			System.out.println( "Error: classifier could not be loaded from '" + directory + filename + "'." );
			return false;
		}

		System.out.println( "Read header from " + directory + filename + " (number of attributes = " + wekaSegmentation.getTrainHeader().numAttributes() + ")" );

		if ( wekaSegmentation.getTrainHeader().numAttributes() < 1 ) {
			IJ.error( "Error", "No attributes were found on the model header loaded from " + directory + filename );
			return false;
		}

		return true;
	}

	public RandomAccessibleInterval< DoubleType > classifyPixels( final RandomAccessibleInterval< T > img, final boolean probabilityMaps ) {
		final List< RandomAccessibleInterval< T >> rais = new ArrayList< RandomAccessibleInterval< T >>();
		rais.add( img );
		return ( classifyPixels( rais, probabilityMaps ) ).get( 0 );
	}

	public List< RandomAccessibleInterval< DoubleType >> classifyPixels( final List< RandomAccessibleInterval< T >> raiList, final boolean probabilityMaps ) {

		final List< RandomAccessibleInterval< DoubleType >> results = new ArrayList< RandomAccessibleInterval< DoubleType >>();
		for ( int i = 0; i < raiList.size(); ++i )
			results.add( null );

		final int numProcessors = Prefs.getThreads();
		final int numThreads = Math.min( raiList.size(), numProcessors );
		final int numFurtherThreads = ( int ) Math.ceil( ( double ) ( numProcessors - numThreads ) / raiList.size() ) + 1;

		System.out.println( "Processing " + raiList.size() + " image files in " + numThreads + " thread(s)...." );

		final Thread[] threads = new Thread[ numThreads ];

		class ImageProcessingThread extends Thread {

			final int numThread;
			final int numThreads;
			final List< RandomAccessibleInterval< T >> raiList;
			final List< RandomAccessibleInterval< DoubleType >> raiListOutputs;

			public ImageProcessingThread( final int numThread, final int numThreads, final List< RandomAccessibleInterval< T >> raiList, final List< RandomAccessibleInterval< DoubleType >> raiListOutputs ) {
				this.numThread = numThread;
				this.numThreads = numThreads;
				this.raiList = raiList;
				this.raiListOutputs = raiListOutputs;
			}

			@Override
			public void run() {

				for ( int i = numThread; i < raiList.size(); i += numThreads ) {

					final ImagePlus forWekaImagePlus = ImageJFunctions.wrap( raiList.get( i ), "Img_num_" + i ).duplicate();
					System.out.println( "Processing image " + ( i + 1 ) + " in thread " + ( numThread + 1 ) );

					final ImagePlus segmentation = wekaSegmentation.applyClassifier( forWekaImagePlus, numFurtherThreads, probabilityMaps );
//					segmentation.

					if ( null != segmentation ) {
						final ImagePlusImg< ?, ? > temp = ImagePlusAdapter.wrapReal( segmentation );
						final RandomAccessibleInterval< DoubleType > rai = Converters.convert( temp, new RealDoubleConverter(), new DoubleType() );
						raiListOutputs.set( i, rai );
					} else {
						System.out.println( "WARNING!!! One of the input images could not be classified!!!" );
					}

					segmentation.close();
					forWekaImagePlus.close();
				}
			}
		}

		// start threads
		for ( int i = 0; i < numThreads; i++ ) {
			threads[ i ] = new ImageProcessingThread( i, numThreads, raiList, results );
			threads[ i ].start();
		}

		// wait for all threads to terminate
		for ( final Thread thread : threads ) {
			try {
				thread.join();
			} catch ( final InterruptedException e ) {}
		}

		return results;
	}
}
