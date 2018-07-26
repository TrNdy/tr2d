/**
 *
 */
package com.indago.tr2d.costs;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.indago.costs.CostFactory;
import com.indago.costs.CostParams;
import com.indago.data.segmentation.LabelingSegment;
import com.indago.geometry.GrahamScan;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;


/**
 * @author jug
 */
public class HernanSegmentCostFactory implements CostFactory< LabelingSegment > {

	private final RandomAccessibleInterval< DoubleType > sourceImage;

	private CostParams params;

	public HernanSegmentCostFactory(
			final RandomAccessibleInterval< DoubleType > sourceImage ) {
		this.sourceImage = sourceImage;

		params = new CostParams();
		params.add( "area (a)", -1 );
		params.add( "|conv_hull(segm)-segm|", 1 );
		params.add( "value of max_a", 100 );
		params.add( "max(0, (size-max_area))²", 0 );
	}

	/**
	 * @see com.indago.costs.CostFactory#getName()
	 */
	@Override
	public String getName() {
		return "(Negative) Segment Costs";
	}

	/**
	 * @see com.indago.costs.CostFactory#getCost(java.lang.Object)
	 */
	@Override
	public double getCost( final LabelingSegment segment ) {
		final double area = params.get( 0 );
		final double ncp = params.get( 1 );

		final double maxa = params.get( 2 );
		final double maxa_factor = params.get( 3 );

		return area * segment.getArea()
				+ ncp * getNonConvexityPenalty( segment )
				+ maxa_factor * Math.pow( Math.max(0, segment.getArea()-maxa), 2);
	}

	/**
	 * Computes the convex hull of a segment an returns the area difference of
	 * the convex hull and the segment itself (in pixels, returns 0 if negative
	 * (due to discrete pixels vs polygon area)).
	 */
	private double getNonConvexityPenalty( final LabelingSegment segment ) {
		final HashMap< Integer, Pair< Integer, Integer > > minmaxPerLine = new HashMap<>();

		final Cursor< ? > cSegment = segment.getRegion().cursor();
		while ( cSegment.hasNext() ) {
			cSegment.fwd();

			final int xCoordinate = cSegment.getIntPosition( 0 );
			final int yCoordinate = cSegment.getIntPosition( 1 );

			final Pair< Integer, Integer > minmax = minmaxPerLine.get( yCoordinate );
			if ( minmax == null ) {
				minmaxPerLine.put( yCoordinate, new ValuePair< Integer, Integer >( xCoordinate, xCoordinate ) );
			} else {
				boolean replace = false;
				if ( minmax.getA() > xCoordinate ) {
					replace = true;
				}
				if ( minmax.getB() < xCoordinate ) {
					replace = true;
				}
				if ( replace ) {
					minmaxPerLine.replace(
							yCoordinate,
							new ValuePair<>( Math.min( minmax.getA(), xCoordinate ), Math.max( minmax.getB(), xCoordinate ) ) );
				}
			}
		}

		final List< Point > points = new ArrayList<>();
		for ( final int y : minmaxPerLine.keySet() ) {
			final Pair< Integer, Integer > minmax = minmaxPerLine.get( y );
			points.add( new Point( minmax.getA(), y ) );
			points.add( new Point( minmax.getB(), y ) );
		}
		try {
			final List< java.awt.Point > convexHull = GrahamScan.getConvexHull( points );
			return Math.max( 0, GrahamScan.getHullArea( convexHull ) - segment.getArea() );
		} catch ( final IllegalArgumentException iae ) {
			return 0;
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
