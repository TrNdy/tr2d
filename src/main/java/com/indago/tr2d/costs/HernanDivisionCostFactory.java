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
	 * @param sourceImage
	 *            The original image these costs are computed for (by far not
	 *            all costs depend on this image).
	 */
	public HernanDivisionCostFactory( final RandomAccessibleInterval< DoubleType > sourceImage ) {
		this.sourceImage = sourceImage;

		params = new CostParams();
		params.add( "const", 10 );
		params.add( "Δsize(A,B1+B2)", 0.2 );
		params.add( "Δsize(B1,B2)", 0.2 );
		params.add( "(Δsize(B1,B2))^2", 0.05 );
		params.add( "avg(Δpos(A↦B1),Δpos(A↦B2)) ", 0.5 );
		params.add( "Δpos(B1↦B2)", 0.05 );
		params.add( "off elong. penalty", 1 );
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
		final double a_0 = params.get( 0 );
		final double a_1 = params.get( 1 );
		final double a_2 = params.get( 2 );
		final double a_3 = params.get( 3 );
		final double a_4 = params.get( 4 );
		final double a_5 = params.get( 5 );
		final double a_6 = params.get( 6 );

		final double deltaSize1to2 = deltaSize( segments.getA(), segments.getB().getA(), segments.getB().getB() );
		final double deltaSizeBetween2s = deltaSize( segments.getB().getA(), segments.getB().getB() );
		final double deltaSizeBetween2sSquared = deltaSizeBetween2s * deltaSizeBetween2s;
		double avgDeltaPosToChildren = avgDeltaPosSquared( segments.getA(), segments.getB().getA(), segments.getB().getB() );
		double deltaPosChildren = deltaPosSquared( segments.getB().getA(), segments.getB().getB() );
		final double offElongationPenalty = offElongationPenalty( segments.getA(), segments.getB().getA(), segments.getB().getB() );

		if ( avgDeltaPosToChildren > HernanCostConstants.MAX_AVG_SQUARED_DIVISION_MOVE_DISTANCE ) { avgDeltaPosToChildren*=2; }
		if ( deltaPosChildren > HernanCostConstants.MAX_SQUARED_DIVISION_OFFSPRING_DISTANCE ) { deltaPosChildren*=2; }

		return a_0 + a_1 * deltaSize1to2 + a_2 * deltaSizeBetween2s + a_3 * deltaSizeBetween2sSquared + a_4 * avgDeltaPosToChildren + a_5 * deltaPosChildren + a_6 * offElongationPenalty;
	}

	/**
	 * For movements.
	 *
	 * @param s1
	 *            A <code>LabelingSegment</code> at time <code>t</code>.
	 * @param s2
	 *            A <code>LabelingSegment</code> at time <code>t+1</code>.
	 * @return computed size difference.
	 */
	private double deltaSize( final LabelingSegment s1, final LabelingSegment s2 ) {
		return Math.abs( s1.getArea() - s2.getArea() );
	}

	/**
	 * For divisions.
	 *
	 * @param s1
	 *            A <code>LabelingSegment</code> at time <code>t</code>.
	 * @param s2_1
	 *            First of the two <code>LabelingSegment</code> at time
	 *            <code>t+1</code>.
	 * @param s2_2
	 *            Second of the two <code>LabelingSegment</code> at time
	 *            <code>t+1</code>.
	 * @return the computed size difference.
	 */
	private double deltaSize( final LabelingSegment s1, final LabelingSegment s2_1, final LabelingSegment s2_2 ) {
		return Math.abs( s1.getArea() - s2_1.getArea() - s2_2.getArea() );
	}

	/**
	 * For movements.
	 *
	 * @param s1
	 *            A <code>LabelingSegment</code> at time <code>t</code>.
	 * @param s2
	 *            A <code>LabelingSegment</code> at time
	 *            <code>t+1</code>.
	 * @return the squared centroid distance.
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
	 * For divisions.
	 *
	 * @param s1
	 *            A <code>LabelingSegment</code> at time <code>t</code>.
	 * @param s2_1
	 *            First <code>LabelingSegment</code> at time
	 *            <code>t+1</code>.
	 * @param s2_2
	 *            First <code>LabelingSegment</code> at time
	 *            <code>t+1</code>.
	 * @return the mean squared centroid distance between s1 and either of the
	 *         two s2_x.
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
	 * For divisions.
	 *
	 * @param s1
	 *            A <code>LabelingSegment</code> at time <code>t</code>.
	 * @param s2_1
	 *            First <code>LabelingSegment</code> at time
	 *            <code>t+1</code>.
	 * @param s2_2
	 *            First <code>LabelingSegment</code> at time
	 *            <code>t+1</code>.
	 * @return Values between 0 and 90 (degrees)
	 */
	private double offElongationPenalty( final LabelingSegment s1, final LabelingSegment s2_1, final LabelingSegment s2_2 ) {
		final SimpleRegression regressionS1 = SegmentCostUtils.getSimpleRegressionOfSegmentPixels( s1 );
		final double angleS1 = Math.toDegrees( Math.atan( regressionS1.getSlope() ) );
		final double confidenceS1 = 1 - regressionS1.getRSquare();

		final SimpleRegression regressionS2s = SegmentCostUtils.getSimpleRegressionOfSegmentPixels( s2_1, s2_2 );
		final double angleS2s = Math.toDegrees( Math.atan( regressionS2s.getSlope() ) );
//		final double confidenceS2s = 1-regressionS2s.getRSquare();

		final double ret = confidenceS1 * Math.abs( angleS1 - angleS2s );
		if ( Double.isNaN( ret ) ) {
			return 0;
		} else {
			return ret;
		}
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
