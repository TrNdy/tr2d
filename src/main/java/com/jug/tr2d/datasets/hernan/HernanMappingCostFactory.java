/**
 *
 */
package com.jug.tr2d.datasets.hernan;

import com.indago.fg.CostsFactory;
import com.indago.segment.Segment;
import com.jug.util.math.VectorUtil;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Pair;


/**
 * @author jug
 */
public class HernanMappingCostFactory
		implements
		CostsFactory< Pair< Segment, Segment > > {

	private final long destframeId;
	private final Object sourceImage;

	/**
	 * @param destFrameId
	 * @param sourceImage
	 */
	public HernanMappingCostFactory(
			final long destFrameId,
			final RandomAccessibleInterval< DoubleType > sourceImage ) {
		this.destframeId = destFrameId;
		this.sourceImage = sourceImage;
	}

	/**
	 * @see com.indago.fg.CostsFactory#getCost(java.lang.Object)
	 */
	@Override
	public double getCost( final Pair< Segment, Segment > segment ) {
		final RealLocalizable posA = segment.getA().getCenterOfMass();
		final RealLocalizable posB = segment.getB().getCenterOfMass();
		final double[] vecA = new double[ posA.numDimensions() ];
		final double[] vecB = new double[ posB.numDimensions() ];
		posA.localize( vecA );
		posB.localize( vecB );
		final double centroidDistance = VectorUtil.getSquaredDistance( vecA, vecB );

		if ( centroidDistance > HernanCostConstants.MAX_MAPPING_DISTANCE ) { return HernanCostConstants.TRUNCATE_COST_VALUE; }

		return .0 * centroidDistance / Math.PI;
	}

}
