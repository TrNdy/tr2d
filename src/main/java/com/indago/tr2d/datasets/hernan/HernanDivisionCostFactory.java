/**
 *
 */
package com.indago.tr2d.datasets.hernan;

import com.indago.fg.CostsFactory;
import com.indago.segment.LabelingSegment;
import com.indago.util.math.VectorUtil;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Pair;


/**
 * @author jug
 */
public class HernanDivisionCostFactory
		implements
		CostsFactory< Pair< LabelingSegment, Pair< LabelingSegment, LabelingSegment > > > {

	private final Object sourceImage;

	/**
    * @param destFrameId
    * @param sourceImage
    */
	public HernanDivisionCostFactory(
			final RandomAccessibleInterval< DoubleType > sourceImage ) {
		this.sourceImage = sourceImage;
    }

	/**
	 * @see com.indago.fg.CostsFactory#getCost(java.lang.Object)
	 */
	@Override
	public double getCost(
			final Pair< LabelingSegment, Pair< LabelingSegment, LabelingSegment > > segments ) {
		final RealLocalizable posA = segments.getA().getCenterOfMass();
		final RealLocalizable posBA = segments.getB().getA().getCenterOfMass();
		final RealLocalizable posBB = segments.getB().getB().getCenterOfMass();
		final double[] vecA = new double[ posA.numDimensions() ];
		final double[] vecBA = new double[ posBA.numDimensions() ];
		final double[] vecBB = new double[ posBB.numDimensions() ];
		posA.localize( vecA );
		posBA.localize( vecBA );
		posBB.localize( vecBB );
		final double centroidDistanceABA = VectorUtil.getSquaredDistance( vecA, vecBA );
		final double centroidDistanceABB = VectorUtil.getSquaredDistance( vecA, vecBB );
		final double centroidDistanceBABB = VectorUtil.getSquaredDistance( vecBA, vecBB );

		if ( Math.max(
				centroidDistanceABA,
				centroidDistanceABB ) > HernanCostConstants.MAX_DIVISION_MOVE_DISTANCE ) { return HernanCostConstants.TRUNCATE_COST_VALUE; }
		if ( centroidDistanceBABB > HernanCostConstants.MAX_DIVISION_OFFSPRING_DISTANCE ) { return HernanCostConstants.TRUNCATE_COST_VALUE; }

//		return .5 * Math.min( centroidDistanceABA, centroidDistanceABB )/Math.PI
//				+ 0.25 * centroidDistanceBABB/Math.PI;
		return 0;
	}

}
