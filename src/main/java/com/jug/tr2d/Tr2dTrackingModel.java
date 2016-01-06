/**
 *
 */
package com.jug.tr2d;

import com.indago.segment.LabelingBuilder;
import com.indago.segment.LabelingForest;
import com.indago.segment.filteredcomponents.FilteredComponentTree;
import com.indago.segment.filteredcomponents.FilteredComponentTree.Filter;
import com.indago.segment.filteredcomponents.FilteredComponentTree.MaxGrowthPerStep;

import net.imglib2.Dimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.DoubleType;

/**
 * @author jug
 */
public class Tr2dTrackingModel {

	private final Tr2dModel tr2dModel;
	private final Tr2dWekaSegmentationModel tr2dSegModel;

	private Dimensions dim;
	private final int minComponentSize = 10;
	private final int maxComponentSize = 10000;
	private final Filter maxGrowthPerStep;
	private final boolean darkToBright = true;

	/**
	 * @param model
	 */
	public Tr2dTrackingModel( final Tr2dModel model, final Tr2dWekaSegmentationModel modelSeg ) {
		this.tr2dModel = model;
		this.tr2dSegModel = modelSeg;

		maxGrowthPerStep = new MaxGrowthPerStep( 1 );
	}

	/**
	 * @return
	 */
	public RandomAccessibleInterval< DoubleType > getSegmentHypothesesImage() {
		try {
			return tr2dSegModel.getSegmentHypotheses();
		} catch ( final IllegalAccessException e ) {
			System.err.println( "Segmentation Hypotheses could not be accessed!" );
			e.printStackTrace();
		}
		return null;
	}

	public void run() {
		dim = getSegmentHypothesesImage();

		final FilteredComponentTree< DoubleType > tree = FilteredComponentTree.buildComponentTree(
				getSegmentHypothesesImage(),
				new DoubleType(),
				minComponentSize,
				maxComponentSize,
				maxGrowthPerStep,
				darkToBright );

		final LabelingBuilder builder = new LabelingBuilder( dim );
		final LabelingForest labelingForest = builder.buildLabelingForest( tree );

		System.out.println( "Success!" );
	}

}
