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
public class HernanAppearanceCostFactory implements CostsFactory< LabelingSegment > {

	private final RandomAccessibleInterval< DoubleType > imgOrig;

	/**
	 * @param frameId
	 * @param imgOrig
	 */
	public HernanAppearanceCostFactory(
			final RandomAccessibleInterval< DoubleType > imgOrig ) {
		this.imgOrig = imgOrig;
	}

	/**
	 * @see com.indago.old_fg.CostsFactory#getCost(java.lang.Object)
	 */
	@Override
	public double getCost( final LabelingSegment segment ) {
		final double posX = segment.getCenterOfMass().getDoublePosition( 0 );
		final double posY = segment.getCenterOfMass().getDoublePosition( 1 );
		double distBorder = Math.min( posX - imgOrig.min( 0 ), posY - imgOrig.min( 1 ) );
		distBorder = Math.min(
				distBorder,
				Math.min( imgOrig.max( 0 ) - posX, imgOrig.max( 1 ) - posY ) );
		double factor = distBorder / 25;
		factor = Math.min( 1.5, factor );
//		return segment.getArea() * ( 1 + factor );
		return 3 * segment.getArea();
	}

}
