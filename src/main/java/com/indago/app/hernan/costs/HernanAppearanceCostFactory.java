/**
 *
 */
package com.indago.app.hernan.costs;

import com.indago.costs.CostParams;
import com.indago.costs.CostsFactory;
import com.indago.data.segmentation.LabelingSegment;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.DoubleType;


/**
 * @author jug
 */
public class HernanAppearanceCostFactory implements CostsFactory< LabelingSegment > {

	private final RandomAccessibleInterval< DoubleType > imgOrig;

	private CostParams params;

	private static double a_1 = 3;
	private static double a_2 = 1;
	private static double a_3 = 1 / 2;

	/**
	 * @param frameId
	 * @param imgOrig
	 */
	public HernanAppearanceCostFactory(
			final RandomAccessibleInterval< DoubleType > imgOrig ) {
		this.imgOrig = imgOrig;
	}

	/**
	 * @see com.indago.costs.CostsFactory#getCost(java.lang.Object)
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

	/**
	 * @see com.indago.costs.CostsFactory#getParameters()
	 */
	@Override
	public CostParams getParameters() {
		return params;
	}

	/**
	 * @see com.indago.costs.CostsFactory#setParameters(com.indago.costs.CostParams)
	 */
	@Override
	public void setParameters( final CostParams p ) {
		this.params = p;
	}
}
