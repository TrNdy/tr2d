/**
 *
 */
package com.jug.tr2d;

import java.io.File;

import com.jug.segmentation.SegmentationMagic;
import com.jug.segmentation.SilentWekaSegmenter;
import com.jug.util.converter.RealDoubleThresholdConverter;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.type.numeric.real.DoubleType;

/**
 * @author jug
 */
public class Tr2dWekaSegmentationModel {

	private static final String USE_LOADED_CLASSIFIER = "<default>";

	private final Tr2dModel model;

	private SilentWekaSegmenter< DoubleType > segClassifier;

	private RandomAccessibleInterval< DoubleType > imgClassification = null;
	private RandomAccessibleInterval< DoubleType > imgSegmentation = null;

	/**
	 *
	 */
	public Tr2dWekaSegmentationModel( final Tr2dModel model ) {
		this.model = model;
	}

	/**
	 * Loads the given classifier file.
	 *
	 * @param classifierFile
	 */
	private void loadClassifier( final File classifierFile ) {
		SegmentationMagic
				.setClassifier( classifierFile.getParent() + "/", classifierFile.getName() );
		segClassifier = SegmentationMagic.getClassifier();
	}

	/**
	 * @return
	 */
	public String getClassifierPath() {
		if ( segClassifier != null ) { return USE_LOADED_CLASSIFIER; }
		return Tr2dProperties.CLASSIFIER_PATH;
	}

	/**
	 * @param absolutePath
	 */
	public void setClassifierPath( final String absolutePath ) throws IllegalArgumentException {
		if ( !absolutePath.equals( USE_LOADED_CLASSIFIER ) ) {
			final File cf = new File( absolutePath );
			if ( !cf.exists() || !cf.canRead() )
				throw new IllegalArgumentException( String
					.format( "Given classifier file cannot be read (%s)", absolutePath ) );
			loadClassifier( cf );
		} else if ( segClassifier == null )
			throw new IllegalArgumentException( "No previously loaded classifier found to be used." );
	}

	/**
	 * Performs the segmentation procedure previously set up.
	 */
	public void segment() {
		if ( segClassifier == null )
			throw new IllegalArgumentException( "No previously loaded classifier found to be used." );

		// classify frames
		imgClassification = SegmentationMagic.returnClassification( model.getImgOrig() );
		imgSegmentation = Converters.convert(
				imgClassification,
				new RealDoubleThresholdConverter( 0.5 ),
				new DoubleType() );
	}

	/**
	 * @return
	 * @throws IllegalAccessException
	 */
	public RandomAccessibleInterval< DoubleType > getClassification()
			throws IllegalAccessException {
		if ( imgClassification == null ) { throw new IllegalAccessException( "Classification not available. Method 'segment()' might not have been called before..." ); }
		return imgClassification;
	}

	/**
	 * @return
	 * @throws IllegalAccessException
	 */
	public RandomAccessibleInterval< DoubleType > getSegmentation()
			throws IllegalAccessException {
		if ( imgSegmentation == null ) { throw new IllegalAccessException( "Segmentation not available. Method 'segment()' might not have been called before..." ); }
		return imgSegmentation;
	}

}
