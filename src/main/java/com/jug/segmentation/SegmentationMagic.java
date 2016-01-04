/**
 *
 */
package com.jug.segmentation;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.SubsampleIntervalView;
import net.imglib2.view.Views;

import com.jug.fkt.Constant1D;
import com.jug.fkt.Function1D;

/**
 * @author jug
 */
public class SegmentationMagic {

	static SilentWekaSegmenter< DoubleType > wekaClassifier;
	private static long numSolutions;

	static RandomAccessibleInterval< DoubleType > lastClassified;

	private static Function1D< Double > fktUnary = new Constant1D( -1.0 );
	private static Function1D< Double > fktPairwiseX = new Constant1D( 0.0 );
	private static Function1D< Double > fktPairwiseY = new Constant1D( 0.0 );
	private static Function1D< Double > fktPairwiseZ = new Constant1D( 0.0 );
	private static double costIsing = 1.0;

	public static void setClassifier( final String folder, final String file ) {
		wekaClassifier = new SilentWekaSegmenter< DoubleType >( folder, file );
	}

	public static void setClassifier( final SilentWekaSegmenter< DoubleType > classifier ) {
		wekaClassifier = classifier;
	}

	public static SilentWekaSegmenter< DoubleType > getClassifier() {
		return wekaClassifier;
	}

	public static RandomAccessibleInterval< DoubleType > returnClassification( final RandomAccessibleInterval< DoubleType > rai ) {
		lastClassified = wekaClassifier.classifyPixels( rai, true );

		final long[] min = new long[ lastClassified.numDimensions() ];
		lastClassified.min( min );
		min[ 2 ]++;
		final long[] max = new long[ lastClassified.numDimensions() ];
		lastClassified.max( max );
		// TODO: FIXES A BUG IN THE IMGLIB... NEEDS TO BE REMOVED AFTER THE BUG IS REMOVED!!!
		if ( ( max[ 2 ] - min[ 2 ] + 1 ) % 2 == 1 ) {
			max[ 2 ]++;
		}

		final SubsampleIntervalView< DoubleType > subsampleGapClass = ( SubsampleIntervalView< DoubleType > ) Views.subsample( Views.interval( lastClassified, min, max ), 1, 1, 2 );

		return subsampleGapClass;
	}

	/**
	 * 
	 * @param rai
	 * @param withClassificationOfGaps
	 * @return
	 */
	private static RandomAccessibleInterval< LongType > returnParamaxflowBaby( final RandomAccessibleInterval< DoubleType > rai, final boolean withClassificationOfGaps ) {
		final ParaMaxFlow< DoubleType > paramaxflow = new ParaMaxFlow< DoubleType >( rai, ( withClassificationOfGaps ) ? returnClassification( rai ) : null, false, fktUnary, costIsing, fktPairwiseX, fktPairwiseY, fktPairwiseZ );

		numSolutions = paramaxflow.solve( -1000000, 1000000 );

		final Img< LongType > sumRegions = paramaxflow.getRegionsImg();

		return sumRegions;
	}

	/**
	 * 
	 * @param rai
	 * @param unaryCostFactor
	 * @param unaryPotentialImage
	 * @param isingCost
	 * @param pairwisePotentialImage
	 * @return
	 */
	private static RandomAccessibleInterval< LongType > returnParamaxflowBaby( final RandomAccessibleInterval< DoubleType > rai, final double unaryCostFactor, final RandomAccessibleInterval< DoubleType > unaryPotentialImage, final double isingCost, final RandomAccessibleInterval< DoubleType > pairwisePotentialImage ) {
		final ParaMaxFlow< DoubleType > paramaxflow = new ParaMaxFlow< DoubleType >( rai, false, unaryCostFactor, unaryPotentialImage, isingCost, pairwisePotentialImage );

		numSolutions = paramaxflow.solve( -1000000, 1000000 );

		final Img< LongType > sumRegions = paramaxflow.getRegionsImg();

		return sumRegions;
	}

	/**
	 * 
	 * @param rai
	 * @param classRai
	 * @return
	 */
	private static RandomAccessibleInterval< LongType > returnParamaxflowBaby( final RandomAccessibleInterval< DoubleType > rai, final RandomAccessibleInterval< LongType > classRai ) {
		final ParaMaxFlow< DoubleType > paramaxflow = new ParaMaxFlow< DoubleType >( rai, classRai, false, fktUnary, costIsing, fktPairwiseX, fktPairwiseY, fktPairwiseZ );

		numSolutions = paramaxflow.solve( -1000000, 1000000 );

		final Img< LongType > sumRegions = paramaxflow.getRegionsImg();

		return sumRegions;
	}

	public static RandomAccessibleInterval< LongType > returnSegmentation( final RandomAccessibleInterval< LongType > sumRegions, final long segNum ) {
		final long[] dims = new long[ sumRegions.numDimensions() ];
		sumRegions.dimensions( dims );
		final ImgFactory< LongType > imgFactory = new ArrayImgFactory< LongType >();
		final Img< LongType > ret = imgFactory.create( dims, new LongType() );

		// create a cursor that automatically localizes itself on every move
		final Cursor< LongType > targetCursor = ret.localizingCursor();
		final RandomAccess< LongType > sourceRandomAccess = sumRegions.randomAccess();

		// iterate over the input cursor
		while ( targetCursor.hasNext() ) {
			// move input cursor forward
			targetCursor.fwd();

			// set the output cursor to the position of the input cursor
			sourceRandomAccess.setPosition( targetCursor );

			// set the value of this pixel of the output image, every Type supports T.set( T type )
			final long seg = ( sourceRandomAccess.get().get() < segNum ) ? 0 : 1;
			targetCursor.get().set( new LongType( seg ) );
		}

		return ret;
	}

	/**
	 * 
	 * @param rai
	 * @return
	 */
	public static RandomAccessibleInterval< LongType > returnFunctionPotentialBasedParamaxflowRegionSums( final RandomAccessibleInterval< DoubleType > rai ) {
		return returnParamaxflowBaby( rai, false );
	}

	/**
	 * 
	 * @param rai
	 * @param classRai
	 * @return
	 */
	public static RandomAccessibleInterval< LongType > returnClassificationModulatedParamaxflowRegionSums( final RandomAccessibleInterval< DoubleType > rai, final RandomAccessibleInterval< DoubleType > classRai ) {
		return returnParamaxflowBaby( rai, true );
	}

	/**
	 * @param rai
	 * @param unariesMaxValue
	 * @param imgUnaryCostImage
	 * @param isingCosts
	 * @param imgPairwiseCostImage
	 * @return
	 */
	public static RandomAccessibleInterval< LongType > returnPotentialImageBasedParamaxflowRegionSums( final RandomAccessibleInterval< DoubleType > rai, final double unariesMaxValue, final RandomAccessibleInterval< DoubleType > imgUnaryCostImage, final double isingCosts, final RandomAccessibleInterval< DoubleType > imgPairwiseCostImage ) {
		return returnParamaxflowBaby( rai, unariesMaxValue, imgUnaryCostImage, isingCosts, imgPairwiseCostImage );
	}

	public static long getNumSolutions() {
		return numSolutions;
	}

	public static void showLastClassified() {
		ImageJFunctions.show( lastClassified );
	}

	/**
	 * @return the fktUnary
	 */
	public static Function1D< Double > getFktUnary() {
		return fktUnary;
	}

	/**
	 * @param fktUnary
	 *            the fktUnary to set
	 */
	public static void setFktUnary( final Function1D< Double > fktUnary ) {
		SegmentationMagic.fktUnary = fktUnary;
	}

	/**
	 * @return the fktPairwiseX
	 */
	public static Function1D< Double > getFktPairwiseX() {
		return fktPairwiseX;
	}

	/**
	 * @param fktPairwiseX
	 *            the fktPairwiseX to set
	 */
	public static void setFktPairwiseX( final Function1D< Double > fktPairwiseX ) {
		SegmentationMagic.fktPairwiseX = fktPairwiseX;
	}

	/**
	 * @return the fktPairwiseY
	 */
	public static Function1D< Double > getFktPairwiseY() {
		return fktPairwiseY;
	}

	/**
	 * @param fktPairwiseY
	 *            the fktPairwiseY to set
	 */
	public static void setFktPairwiseY( final Function1D< Double > fktPairwiseY ) {
		SegmentationMagic.fktPairwiseY = fktPairwiseY;
	}

	/**
	 * @return the fktPairwiseZ
	 */
	public static Function1D< Double > getFktPairwiseZ() {
		return fktPairwiseZ;
	}

	/**
	 * @param fktPairwiseZ
	 *            the fktPairwiseZ to set
	 */
	public static void setFktPairwiseZ( final Function1D< Double > fktPairwiseZ ) {
		SegmentationMagic.fktPairwiseZ = fktPairwiseZ;
	}

	/**
	 * @return the costIsing
	 */
	public static double getCostIsing() {
		return costIsing;
	}

	/**
	 * @param costIsing
	 *            the costIsing to set
	 */
	public static void setCostIsing( final double costIsing ) {
		SegmentationMagic.costIsing = costIsing;
	}

}
