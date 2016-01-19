/**
 *
 */
package com.indago.tr2d.datasets.hernan;

import com.indago.fg.CostsFactory;
import com.indago.segment.LabelingSegment;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.DoubleType;


/**
 * @author jug
 */
public class HernanDisappearanceCostFactory implements CostsFactory< LabelingSegment > {

	private final RandomAccessibleInterval< DoubleType > imgOrig;

	/**
	 * @param frameId
	 * @param imgOrig
	 */
	public HernanDisappearanceCostFactory(
			final RandomAccessibleInterval< DoubleType > imgOrig ) {
		this.imgOrig = imgOrig;
	}

	/**
	 * @see com.indago.fg.CostsFactory#getCost(java.lang.Object)
	 */
	@Override
	public double getCost( final LabelingSegment segment ) {
		final double posX = segment.getCenterOfMass().getDoublePosition( 0 );
		final double posY = segment.getCenterOfMass().getDoublePosition( 1 );
		double distBorder = Math.min( posX - imgOrig.min( 0 ), posY - imgOrig.min( 1 ) );
		distBorder = Math
				.min( distBorder, Math.min( imgOrig.max( 0 ) - posX, imgOrig.max( 1 ) - posY ) );
		double factor = distBorder / 25;
		factor = Math.min( 1.5, factor );
		return segment.getArea() * factor;
	}

}
