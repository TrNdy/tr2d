/**
 *
 */
package com.indago.tr2d.ui.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.indago.data.segmentation.SegmentationMagic;
import com.indago.data.segmentation.SilentWekaSegmenter;
import com.indago.tr2d.Tr2dProperties;
import com.indago.util.DataMover;
import com.indago.util.converter.RealDoubleThresholdConverter;

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

	private List< Double > listThresholds = new ArrayList< Double >();

	private RandomAccessibleInterval< DoubleType > imgClassification = null;
	private RandomAccessibleInterval< DoubleType > imgSegmentHypotheses = null;

	/**
	 *
	 */
	public Tr2dWekaSegmentationModel( final Tr2dModel model ) {
		this.model = model;
		getListThresholds().add( 0.2 );
		getListThresholds().add( 0.5 );
		getListThresholds().add( 0.7 );
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

		// collect thresholds in SumImage
		RandomAccessibleInterval< DoubleType > imgTemp;
		imgSegmentHypotheses =
				DataMover.createEmptyArrayImgLike( imgClassification, new DoubleType() );
		for ( final Double d : listThresholds ) {
			imgTemp = Converters.convert(
					imgClassification,
					new RealDoubleThresholdConverter( d ),
					new DoubleType() );
			DataMover.add( imgTemp, imgSegmentHypotheses );
		}
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
	public RandomAccessibleInterval< DoubleType > getSegmentHypotheses()
			throws IllegalAccessException {
		if ( imgSegmentHypotheses == null ) { throw new IllegalAccessException( "Segmentation not available. Method 'segment()' might not have been called before..." ); }
		return imgSegmentHypotheses;
	}

	/**
	 * @return the listThresholds
	 */
	public List< Double > getListThresholds() {
		return listThresholds;
	}

	/**
	 *
	 * @param list
	 */
	public void setListThresholds( final List< Double > list ) {
		this.listThresholds = list;
	}
}
