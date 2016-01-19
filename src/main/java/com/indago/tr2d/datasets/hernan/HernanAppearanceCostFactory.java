/**
 *
 */
package com.indago.tr2d.datasets.hernan;

import com.indago.fg.CostsFactory;
import com.indago.segment.LabelingSegment;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.DoubleType;


/**
 * @author jug
 */
public class HernanAppearanceCostFactory implements CostsFactory< LabelingSegment > {

	private final RandomAccessibleInterval< DoubleType > imgOrig;

	/**
	 * @param frameId
	 * @param imgOrig
	 */
	public HernanAppearanceCostFactory(
			final RandomAccessibleInterval< DoubleType > imgOrig ) {
		this.imgOrig = imgOrig;
	}

	/**
	 * @see com.indago.fg.CostsFactory#getCost(java.lang.Object)
	 */
	@Override
	public double getCost( final LabelingSegment segment ) {
		return 3 * segment.getArea();
	}

}
