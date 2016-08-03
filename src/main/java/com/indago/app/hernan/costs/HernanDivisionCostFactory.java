/**
 *
 */
package com.indago.app.hernan.costs;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import com.indago.costs.CostParams;
import com.indago.costs.CostsFactory;
import com.indago.costs.SegmentCostUtils;
import com.indago.data.segmentation.LabelingSegment;
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

	private final RandomAccessibleInterval< DoubleType > sourceImage;

	private CostParams params;

	private static double a_1 = 25;
	private static double a_2 = 50;
	private static double a_3 = 1 / 3;
	private static double a_4 = 1 / 3;
	private static double a_5 = 50;

	/**
    * @param destFrameId
    * @param sourceImage
    */
	public HernanDivisionCostFactory(
			final RandomAccessibleInterval< DoubleType > sourceImage ) {
		this.sourceImage = sourceImage;
    }

	/**
	 * @see com.indago.costs.CostsFactory#getCost(java.lang.Object)
	 */
	@Override
	public double getCost(
			final Pair< LabelingSegment, Pair< LabelingSegment, LabelingSegment > > segments ) {
		final double deltaSize1to2 = deltaSize( segments.getA(), segments.getB().getA(), segments.getB().getB() );
		final double deltaSizeBetween2s = deltaSize( segments.getB().getA(), segments.getB().getB() );
		final double avgDeltaPosToChildren = avgDeltaPosSquared( segments.getA(), segments.getB().getA(), segments.getB().getB() );
		final double deltaPosChildren = deltaPosSquared( segments.getB().getA(), segments.getB().getB() );
		final double offElongationPenalty = offElongationPenalty( segments.getA(), segments.getB().getA(), segments.getB().getB() );

		if ( avgDeltaPosToChildren > HernanCostConstants.MAX_AVG_SQUARED_DIVISION_MOVE_DISTANCE ) { return HernanCostConstants.TRUNCATE_COST_VALUE; }
		if ( deltaPosChildren > HernanCostConstants.MAX_SQUARED_DIVISION_OFFSPRING_DISTANCE ) { return HernanCostConstants.TRUNCATE_COST_VALUE; }

//		System.out.println(
//				String.format(
//						"DivisionCost terms:\t%f\t%f\t%f\t%f\t%f",
//						deltaSize1to2,
//						deltaSizeBetween2s,
//						avgDeltaPosToChildren,
//						deltaPosChildren,
//						offElongationPenalty ) );
		return a_1 * deltaSize1to2 + a_2 * deltaSizeBetween2s + a_3 * avgDeltaPosToChildren + a_4 * deltaPosChildren + a_5 * offElongationPenalty;
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
	private double deltaSize( final LabelingSegment s1, final LabelingSegment s2_1, final LabelingSegment s2_2 ) {
		return Math.abs( s1.getArea() - s2_1.getArea() - s2_2.getArea() );
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

	/**
	 * @param segments
	 * @return
	 */
	private double avgDeltaPosSquared( final LabelingSegment s1, final LabelingSegment s2_1, final LabelingSegment s2_2 ) {
		final RealLocalizable pos1 = s1.getCenterOfMass();
		final RealLocalizable pos2_1 = s2_1.getCenterOfMass();
		final RealLocalizable pos2_2 = s2_2.getCenterOfMass();

		final double[] vecTo1 = new double[ pos1.numDimensions() ];
		final double[] vecTo2_1 = new double[ pos2_1.numDimensions() ];
		final double[] vecTo2_2 = new double[ pos2_2.numDimensions() ];
		pos1.localize( vecTo1 );
		pos2_1.localize( vecTo2_1 );
		pos2_2.localize( vecTo2_2 );

		final double dist1to2_1 = VectorUtil.getSquaredDistance( vecTo1, vecTo2_1 );
		final double dist1to2_2 = VectorUtil.getSquaredDistance( vecTo1, vecTo2_2 );
		return .5 * ( dist1to2_1 + dist1to2_2 );
	}

	/**
	 * @param s1
	 * @param s2_1
	 * @param s2_2
	 * @return Values between 0 and 90 (degrees)
	 */
	private double offElongationPenalty( final LabelingSegment s1, final LabelingSegment s2_1, final LabelingSegment s2_2 ) {
		final SimpleRegression regressionS1 = SegmentCostUtils.getSimpleRegressionOfSegmentPixels( s1 );
		final double angleS1 = Math.toDegrees( Math.atan( regressionS1.getSlope() ) );
		final double confidenceS1 = 1 - regressionS1.getRSquare();

		final SimpleRegression regressionS2s = SegmentCostUtils.getSimpleRegressionOfSegmentPixels( s2_1, s2_2 );
		final double angleS2s = Math.toDegrees( Math.atan( regressionS2s.getSlope() ) );
//		final double confidenceS2s = 1-regressionS2s.getRSquare();

		return confidenceS1 * Math.abs( angleS1 - angleS2s );
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
