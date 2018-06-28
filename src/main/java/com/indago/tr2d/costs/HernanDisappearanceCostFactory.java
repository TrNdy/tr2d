/**
 *
 */
package com.indago.tr2d.costs;

import com.indago.costs.CostFactory;
import com.indago.costs.CostParams;
import com.indago.data.segmentation.LabelingSegment;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.DoubleType;


/**
 * @author jug
 */
public class HernanDisappearanceCostFactory implements CostFactory< LabelingSegment > {

	private final RandomAccessibleInterval< DoubleType > imgOrig;

	private CostParams params;

	public HernanDisappearanceCostFactory(
			final RandomAccessibleInterval< DoubleType > imgOrig ) {
		this.imgOrig = imgOrig;

		params = new CostParams();
		params.add( "area", 2 );
		params.add( "√(d_border)", 0 );
		params.add( "d_border", 0 );
	}

	/**
	 * @see com.indago.costs.CostFactory#getName()
	 */
	@Override
	public String getName() {
		return "Disappearance Costs";
	}

	/**
	 * @see com.indago.costs.CostFactory#getCost(java.lang.Object)
	 */
	@Override
	public double getCost( final LabelingSegment segment ) {
		final double a_1 = params.get( 0 );
		final double a_2 = params.get( 1 );
		final double a_3 = params.get( 2 );

		return  a_1 * segment.getArea() +
				a_2 * Math.sqrt( getDistToImageBorder( segment ) ) +
				a_3 * getDistToImageBorder( segment );
	}

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
	 * @see com.indago.costs.CostFactory#getParameters()
	 */
	@Override
	public CostParams getParameters() {
		return params;
	}

	/**
	 * @see com.indago.costs.CostFactory#setParameters(com.indago.costs.CostParams)
	 */
	@Override
	public void setParameters( final CostParams p ) {
		this.params = p;
	}

}
