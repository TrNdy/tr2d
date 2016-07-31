/**
 *
 */
package com.indago.tr2d.ui.model;

import com.indago.app.hernan.costs.HernanAppearanceCostFactory;
import com.indago.app.hernan.costs.HernanDisappearanceCostFactory;
import com.indago.app.hernan.costs.HernanDivisionCostFactory;
import com.indago.app.hernan.costs.HernanMappingCostFactory;
import com.indago.app.hernan.costs.HernanSegmentCostFactory;
import com.indago.io.DoubleTypeImgLoader;
import com.indago.io.ProjectFolder;
import com.indago.util.ImglibUtil;

import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
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
		final Img< DoubleType > temp = ImagePlusAdapter.wrapNumeric( imgPlus );
		imgRaw = DoubleTypeImgLoader.wrapEnsureType( imgPlus );
		ImglibUtil.computeMinMax( Views.iterable( imgRaw ), min, max );

		segModel = new Tr2dSegmentationCollectionModel( this );
		flowModel = new Tr2dFlowModel( this );
		trackingModel =
				new Tr2dTrackingModel( this, getSegmentationModel(), new HernanSegmentCostFactory( imgRaw ), new HernanAppearanceCostFactory( imgRaw ), new HernanMappingCostFactory( imgRaw ), new HernanDivisionCostFactory( imgRaw ), new HernanDisappearanceCostFactory( imgRaw ) );
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

}
