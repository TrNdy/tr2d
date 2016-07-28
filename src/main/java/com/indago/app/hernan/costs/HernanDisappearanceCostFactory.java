/**
 *
 */
package com.indago.app.hernan.costs;

import com.indago.data.segmentation.LabelingSegment;
import com.indago.old_fg.CostsFactory;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.DoubleType;


/**
 * @author jug
 */
public class HernanDisappearanceCostFactory implements CostsFactory< LabelingSegment > {

	private final RandomAccessibleInterval< DoubleType > imgOrig;

	private static double a_1 = 1;
	private static double a_2 = 1 / 2;
	private static double a_3 = 1 / 3;

	/**
	 * @param frameId
	 * @param imgOrig
	 */
	public HernanDisappearanceCostFactory(
			final RandomAccessibleInterval< DoubleType > imgOrig ) {
		this.imgOrig = imgOrig;
	}

	/**
	 * @see com.indago.old_fg.CostsFactory#getCost(java.lang.Object)
	 */
	@Override
	public double getCost( final LabelingSegment segment ) {
		return  a_1 * segment.getArea() +
				a_2 * Math.sqrt( getDistToImageBorder( segment ) ) +
				a_3 * getDistToImageBorder( segment );
	}

	/**
	 * @param segment
	 * @return
	 */
	private double getDistToImageBorder( final LabelingSegment segment ) {
		final double posX = segment.getCenterOfMass().getDoublePosition( 0 );
		final double posY = segment.getCenterOfMass().getDoublePosition( 1 );
		double distBorder = Math.min( posX - imgOrig.min( 0 ), posY - imgOrig.min( 1 ) );
		distBorder = Math.min(
				distBorder,
				Math.min( imgOrig.max( 0 ) - posX, imgOrig.max( 1 ) - posY ) );
		return distBorder;
	}

}
