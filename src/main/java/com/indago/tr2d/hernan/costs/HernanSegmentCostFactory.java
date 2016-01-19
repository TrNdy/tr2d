/**
 *
 */
package com.indago.tr2d.hernan.costs;

import com.indago.data.segmentation.LabelingSegment;
import com.indago.fg.CostsFactory;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.DoubleType;


/**
 * @author jug
 */
public class HernanSegmentCostFactory implements CostsFactory< LabelingSegment > {

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
	public double getCost( final LabelingSegment segment ) {
		return -1 * segment.getArea();
	}

}
