/**
 *
 */
package com.indago.tr2d.ui.model;

import org.scijava.ui.behaviour.io.InputTriggerConfig;

import com.indago.io.DoubleTypeImgLoader;
import com.indago.io.ProjectFolder;
import com.indago.tr2d.costs.HernanAppearanceCostFactory;
import com.indago.tr2d.costs.HernanDisappearanceCostFactory;
import com.indago.tr2d.costs.HernanDivisionCostFactory;
import com.indago.tr2d.costs.HernanMovementCostFactory;
import com.indago.tr2d.costs.HernanSegmentCostFactory;
import com.indago.util.ImglibUtil;

import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;

/**
 * @author jug
 */
public class Tr2dModel {

	private final ImagePlus imgPlus;
	private RandomAccessibleInterval< DoubleType > imgRaw;
	private final DoubleType min = new DoubleType();
	private final DoubleType max = new DoubleType();

	// TODO think about better separation of concerns
	private InputTriggerConfig inputTriggerConfig;

	private final ProjectFolder projectFolder;

	private final Tr2dSegmentationCollectionModel segModel;
	private final Tr2dFlowModel flowModel;
	private final Tr2dTrackingModel trackingModel;

	/**
	 *
	 * @param projectFolderBasePath
	 * @param imgPlus
	 */
	public Tr2dModel( final ProjectFolder projectFolder, final ImagePlus imgPlus ) {
		this.imgPlus = imgPlus;
		this.projectFolder = projectFolder;

		imgRaw = DoubleTypeImgLoader.wrapEnsureType( imgPlus );
		ImglibUtil.computeMinMax( Views.iterable( imgRaw ), min, max );

		segModel = new Tr2dSegmentationCollectionModel( this );
		flowModel = new Tr2dFlowModel( this );
		trackingModel =
				new Tr2dTrackingModel( this, new HernanSegmentCostFactory( imgRaw ), new HernanAppearanceCostFactory( imgRaw ), new HernanMovementCostFactory( imgRaw ), new HernanDivisionCostFactory( imgRaw ), new HernanDisappearanceCostFactory( imgRaw ) );
	}

	/**
	 * @return the imgPlus
	 */
	public ImagePlus getImgPlus() {
		return imgPlus;
	}

	/**
	 * @return the imgRaw
	 */
	public RandomAccessibleInterval< DoubleType > getRawData() {
		return imgRaw;
	}

	/**
	 * @param imgOrig
	 *            the imgOrig to set
	 */
	public void setRawData( final RandomAccessibleInterval< DoubleType > imgOrig ) {
		this.imgRaw = imgOrig;
	}

	/**
	 * @return the project folder tr2d uses
	 */
	public ProjectFolder getProjectFolder() {
		return projectFolder;
	}

	/**
	 * @return
	 */
	public double getMaxRawValue() {
		return max.getRealDouble();
	}

	/**
	 * @return
	 */
	public double getMinRawValue() {
		return min.getRealDouble();
	}

	/**
	 * @return the segModel
	 */
	public Tr2dSegmentationCollectionModel getSegmentationModel() {
		return segModel;
	}

	/**
	 * @return the flowModel
	 */
	public Tr2dFlowModel getFlowModel() {
		return flowModel;
	}

	/**
	 * @return the trackingModel
	 */
	public Tr2dTrackingModel getTrackingModel() {
		return trackingModel;
	}

	/**
	 * @param conf
	 */
	public void setDefaultInputTriggerConfig( final InputTriggerConfig conf ) {
		this.inputTriggerConfig = conf;
	}

	/**
	 * @return the set <code>InputTriggerConfig</code>, or <code>null</code> if
	 *         none was set.
	 * @return
	 */
	public InputTriggerConfig getDefaultInputTriggerConfig() {
		return this.inputTriggerConfig;
	}

}
