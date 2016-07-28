/**
 *
 */
package com.indago.app.hernan.costs;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.indago.costs.CostsFactory;
import com.indago.data.segmentation.LabelingSegment;
import com.indago.geometry.GrahamScan;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.roi.Regions;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;


/**
 * @author jug
 */
public class HernanSegmentCostFactory implements CostsFactory< LabelingSegment > {

	private final RandomAccessibleInterval< DoubleType > sourceImage;

	private static double a_1 = 1;
	private static double a_2 = 1;

	/**
	 * @param frameId
	 * @param segments
	 * @param sourceImage
	 */
	public HernanSegmentCostFactory(
			final RandomAccessibleInterval< DoubleType > sourceImage ) {
		this.sourceImage = sourceImage;
	}

	/**
	 * @see com.indago.costs.CostsFactory#getCost(java.lang.Object)
	 */
	@Override
	public double getCost( final LabelingSegment segment ) {
		return -( a_1 * segment.getArea() - a_2 * getNonConvexityPenalty( segment ) );
	}

	/**
	 * Computes the convex hull of a segment an returns the area difference of
	 * the convex hull and the segment itself (in pixels, returns 0 if negative
	 * (due to discrete pixels vs polygon area)).
	 *
	 * @param segment
	 * @return
	 */
	private double getNonConvexityPenalty( final LabelingSegment segment ) {
		final HashMap< Integer, Pair< Integer, Integer > > minmaxPerLine = new HashMap<>();
		final RandomAccess< DoubleType > ra = sourceImage.randomAccess();

		final IterableInterval< DoubleType > pixels = Regions.sample( segment.getRegion(), sourceImage );

		final Cursor< ? > cSegment = segment.getRegion().cursor();
		while ( cSegment.hasNext() ) {
			cSegment.fwd();
			ra.setPosition( cSegment );

			final int xCoordinate = ra.getIntPosition( 0 );
			final int yCoordinate = ra.getIntPosition( 1 );

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

}
