/**
 *
 */
package com.indago.tr2d.hernan.costs;

import com.indago.data.segmentation.LabelingSegment;
import com.indago.fg.CostsFactory;
import com.indago.util.math.VectorUtil;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Pair;


/**
 * @author jug
 */
public class HernanMappingCostFactory
		implements
		CostsFactory< Pair< LabelingSegment, LabelingSegment > > {

	private final Object sourceImage;

	/**
	 * @param destFrameId
	 * @param sourceImage
	 */
	public HernanMappingCostFactory(
			final RandomAccessibleInterval< DoubleType > sourceImage ) {
		this.sourceImage = sourceImage;
	}

	/**
	 * @see com.indago.fg.CostsFactory#getCost(java.lang.Object)
	 */
	@Override
	public double getCost( final Pair< LabelingSegment, LabelingSegment > segments ) {
		final RealLocalizable posA = segments.getA().getCenterOfMass();
		final RealLocalizable posB = segments.getB().getCenterOfMass();
		final double[] vecA = new double[ posA.numDimensions() ];
		final double[] vecB = new double[ posB.numDimensions() ];
		posA.localize( vecA );
		posB.localize( vecB );
		final double centroidDistance = VectorUtil.getSquaredDistance( vecA, vecB );

		if ( centroidDistance > HernanCostConstants.MAX_MOVEMENT_DISTANCE ) { return HernanCostConstants.TRUNCATE_COST_VALUE; }

		return .0 * centroidDistance / Math.PI;
	}

}
