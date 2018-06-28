/**
 *
 */
package com.indago.tr2d.costs;

import com.indago.costs.CostFactory;
import com.indago.costs.CostParams;
import com.indago.data.segmentation.LabelingSegment;
import com.indago.util.math.VectorUtil;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Pair;


/**
 * @author jug
 */
public class HernanMovementCostFactory
		implements
		CostFactory< Pair< Pair< LabelingSegment, LabelingSegment >, Pair< Double, Double > > > {

	private final RandomAccessibleInterval< DoubleType > sourceImage;

	private CostParams params;

	public HernanMovementCostFactory(
			final RandomAccessibleInterval< DoubleType > sourceImage ) {
		this.sourceImage = sourceImage;

		params = new CostParams();
		params.add( "Δsize(A,B)", 1.0 );
		params.add( "Δpos(A,B)", 1.0 );
	}

	/**
	 * @see com.indago.costs.CostFactory#getName()
	 */
	@Override
	public String getName() {
		return "Mapping Costs";
	}

	/**
	 * Gets a Pair containing a Pair of segments (from; to), and a Pair of
	 * Doubles (flow vector).
	 * TODO: make parameter some struct instead of such a crazy Pair of
	 * Pair,Pair construction...
	 *
	 * @see com.indago.costs.CostFactory#getCost(java.lang.Object)
	 */
	@Override
	public double getCost( final Pair< Pair< LabelingSegment, LabelingSegment >, Pair< Double, Double > > segmentsAndFlowVector ) {
		final double a_1 = params.get( 0 );
		final double a_2 = params.get( 1 );

		final Pair< LabelingSegment, LabelingSegment > segments = segmentsAndFlowVector.getA();
		final Pair< Double, Double > flow = segmentsAndFlowVector.getB();
		final double deltaSize = deltaSize( segments.getA(), segments.getB() );
		double deltaPos = deltaPosSquared( segments.getA(), segments.getB(), flow );

		if ( deltaPos > HernanCostConstants.MAX_SQUARED_MOVEMENT_DISTANCE ) { deltaPos*=2; }

		return a_1 * deltaSize + a_2 * deltaPos;
	}

	private double deltaSize( final LabelingSegment s1, final LabelingSegment s2 ) {
		return Math.abs( s1.getArea() - s2.getArea() );
	}

	private double deltaPosSquared( final LabelingSegment s1, final LabelingSegment s2, final Pair< Double, Double > flow ) {
		final RealLocalizable pos1 = s1.getCenterOfMass();
		final RealLocalizable pos2 = s2.getCenterOfMass();
		final double[] vecA = new double[ pos1.numDimensions() ];
		final double[] vecB = new double[ pos2.numDimensions() ];
		pos1.localize( vecA );
		pos2.localize( vecB );

		// add flow vector to 'from segment'
		vecA[ 0 ] += flow.getA();
		vecA[ 1 ] += flow.getB();

		final double centroidDistance = VectorUtil.getSquaredDistance( vecA, vecB );
		return centroidDistance;
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
