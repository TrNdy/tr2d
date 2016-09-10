/**
 *
 */
package com.indago.tr2d.costs;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import com.indago.costs.CostFactory;
import com.indago.costs.CostParams;
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
		CostFactory< Pair< LabelingSegment, Pair< LabelingSegment, LabelingSegment > > > {

	private final RandomAccessibleInterval< DoubleType > sourceImage;

	private CostParams params;

	/**
    * @param destFrameId
    * @param sourceImage
    */
	public HernanDivisionCostFactory(
			final RandomAccessibleInterval< DoubleType > sourceImage ) {
		this.sourceImage = sourceImage;

		params = new CostParams();
		params.add( "Δsize(A,B1+B2)", 25 );
		params.add( "Δsize(B1,B2)", 50 );
		params.add( "avg(Δpos(A↦B1),Δpos(A↦B2)) ", 0.333 );
		params.add( "Δpos(B1↦B2)", 0.333 );
		params.add( "off elong. penalty", 50 );
    }

	/**
	 * @see com.indago.costs.CostFactory#getName()
	 */
	@Override
	public String getName() {
		return "Division Costs";
	}

	/**
	 * @see com.indago.costs.CostFactory#getCost(java.lang.Object)
	 */
	@Override
	public double getCost(
			final Pair< LabelingSegment, Pair< LabelingSegment, LabelingSegment > > segments ) {
		final double a_1 = params.get( 0 );
		final double a_2 = params.get( 1 );
		final double a_3 = params.get( 2 );
		final double a_4 = params.get( 3 );
		final double a_5 = params.get( 4 );

		final double deltaSize1to2 = deltaSize( segments.getA(), segments.getB().getA(), segments.getB().getB() );
		final double deltaSizeBetween2s = deltaSize( segments.getB().getA(), segments.getB().getB() );
		final double avgDeltaPosToChildren = avgDeltaPosSquared( segments.getA(), segments.getB().getA(), segments.getB().getB() );
		final double deltaPosChildren = deltaPosSquared( segments.getB().getA(), segments.getB().getB() );
		final double offElongationPenalty = offElongationPenalty( segments.getA(), segments.getB().getA(), segments.getB().getB() );

		if ( avgDeltaPosToChildren > HernanCostConstants.MAX_AVG_SQUARED_DIVISION_MOVE_DISTANCE ) { return HernanCostConstants.TRUNCATE_COST_VALUE; }
		if ( deltaPosChildren > HernanCostConstants.MAX_SQUARED_DIVISION_OFFSPRING_DISTANCE ) { return HernanCostConstants.TRUNCATE_COST_VALUE; }

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
