/**
 *
 */
package com.jug.tr2d.datasets.hernan;

import com.indago.fg.CostsFactory;
import com.indago.segment.LabelingSegment;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Pair;


/**
 * @author jug
 */
public class HernanTransitionCostFactory
		implements
		CostsFactory< Pair< LabelingSegment, LabelingSegment > > {

	private final long destframeId;
	private final Object sourceImage;

	/**
	 * @param destFrameId
	 * @param sourceImage
	 */
	public HernanTransitionCostFactory(
			final long destFrameId,
			final RandomAccessibleInterval< DoubleType > sourceImage ) {
		this.destframeId = destFrameId;
		this.sourceImage = sourceImage;
	}

	/**
	 * @see com.indago.fg.CostsFactory#getCost(java.lang.Object)
	 */
	@Override
	public double getCost( final Pair< LabelingSegment, LabelingSegment > segment ) {
		return 0.1;
	}

}
