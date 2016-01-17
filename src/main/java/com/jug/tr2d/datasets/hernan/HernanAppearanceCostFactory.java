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
public class HernanAppearanceCostFactory implements CostsFactory< Segment > {

	private final long frameId;
	private final RandomAccessibleInterval< DoubleType > imgOrig;

	/**
	 * @param frameId
	 * @param imgOrig
	 */
	public HernanAppearanceCostFactory(
			final long frameId,
			final RandomAccessibleInterval< DoubleType > imgOrig ) {
		this.frameId = frameId;
		this.imgOrig = imgOrig;
	}

	/**
	 * @see com.indago.fg.CostsFactory#getCost(java.lang.Object)
	 */
	@Override
	public double getCost( final Segment segment ) {
		return 3 * segment.getArea();
	}

}
