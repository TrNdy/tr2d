/**
 *
 */
package com.indago.tr2d.datasets.hernan;

import com.indago.fg.CostsFactory;
import com.indago.segment.Segment;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.DoubleType;


/**
 * @author jug
 */
public class HernanSegmentCostFactory implements CostsFactory< Segment > {

	private final RandomAccessibleInterval< DoubleType > sourceImage;

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
	 * @see com.indago.fg.CostsFactory#getCost(java.lang.Object)
	 */
	@Override
	public double getCost( final Segment segment ) {
		return -1 * segment.getArea();
	}

}
