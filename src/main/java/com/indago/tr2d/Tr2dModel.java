/**
 *
 */
package com.indago.tr2d;

import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.DoubleType;

/**
 * @author jug
 */
public class Tr2dModel {

	private final ImagePlus imgPlus;
	private RandomAccessibleInterval< DoubleType > imgOrig;
	private RandomAccessibleInterval< DoubleType > imgOrigNorm;

	/**
	 *
	 * @param imgPlus
	 */
	public Tr2dModel( final ImagePlus imgPlus ) {
		this.imgPlus = imgPlus;
	}

	/**
	 * @return the imgPlus
	 */
	public ImagePlus getImgPlus() {
		return imgPlus;
	}

	/**
	 * @return the imgOrig
	 */
	public RandomAccessibleInterval< DoubleType > getImgOrig() {
		return imgOrig;
	}

	/**
	 * @param imgOrig
	 *            the imgOrig to set
	 */
	public void setImgOrig( final RandomAccessibleInterval< DoubleType > imgOrig ) {
		this.imgOrig = imgOrig;
	}

	/**
	 * @return the imgOrigNorm
	 */
	public RandomAccessibleInterval< DoubleType > getImgOrigNorm() {
		return imgOrigNorm;
	}

	/**
	 * @param imgOrigNorm
	 *            the imgOrigNorm to set
	 */
	public void setImgOrigNorm( final RandomAccessibleInterval< DoubleType > imgOrigNorm ) {
		this.imgOrigNorm = imgOrigNorm;
	}

}
