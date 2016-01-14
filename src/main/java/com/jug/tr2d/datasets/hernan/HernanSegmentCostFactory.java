/**
 *
 */
package com.jug.tr2d.datasets.hernan;

import com.indago.fg.CostsFactory;
import com.indago.segment.Segment;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.DoubleType;


/**
 * @author jug
 */
public class HernanSegmentCostFactory implements CostsFactory< Segment > {

	private final long frameId;
	private final RandomAccessibleInterval< DoubleType > sourceImage;

	/**
	 * @param frameId
	 * @param segments
	 * @param sourceImage
	 */
	public HernanSegmentCostFactory(
			final long frameId,
			final RandomAccessibleInterval< DoubleType > sourceImage ) {
		this.frameId = frameId;
		this.sourceImage = sourceImage;
	}

	/**
	 * @see com.indago.fg.CostsFactory#getCost(java.lang.Object)
	 */
	@Override
	public double getCost( final Segment segment ) {
		// TODO fill with life!
		return -0.5;
	}

}
