/**
 *
 */
package com.indago.app.hernan.costs;

import com.indago.costs.CostsFactory;
import com.indago.data.segmentation.LabelingSegment;
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

	private final RandomAccessibleInterval< DoubleType > sourceImage;

	private static double a_1 = .33;
	private static double a_2 = 1.0;

	/**
	 * @param destFrameId
	 * @param sourceImage
	 */
	public HernanMappingCostFactory(
			final RandomAccessibleInterval< DoubleType > sourceImage ) {
		this.sourceImage = sourceImage;
	}

	/**
	 * @see com.indago.costs.CostsFactory#getCost(java.lang.Object)
	 */
	@Override
	public double getCost( final Pair< LabelingSegment, LabelingSegment > segments ) {
		final double deltaSize = deltaSize( segments.getA(), segments.getB() );
		final double deltaPos = deltaPosSquared( segments.getA(), segments.getB() );

		if ( deltaPos > HernanCostConstants.MAX_SQUARED_MOVEMENT_DISTANCE ) { return HernanCostConstants.TRUNCATE_COST_VALUE; }

		return a_1 * deltaSize + a_2 * deltaPos;
	}

	/**
	 * @param segments
	 * @return
	 */
	private double deltaSize( final LabelingSegment s1, final LabelingSegment s2 ) {
		return Math.abs( s1.getArea() - s2.getArea() );
	}

	/**
	 * @param segments
	 * @return
	 */
	private double deltaPosSquared( final LabelingSegment s1, final LabelingSegment s2 ) {
		final RealLocalizable pos1 = s1.getCenterOfMass();
		final RealLocalizable pos2 = s2.getCenterOfMass();
		final double[] vecA = new double[ pos1.numDimensions() ];
		final double[] vecB = new double[ pos2.numDimensions() ];
		pos1.localize( vecA );
		pos2.localize( vecB );
		final double centroidDistance = VectorUtil.getSquaredDistance( vecA, vecB );
		return centroidDistance;
	}

}
