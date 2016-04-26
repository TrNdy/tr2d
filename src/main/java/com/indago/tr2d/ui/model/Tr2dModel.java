/**
 *
 */
package com.indago.tr2d.ui.model;

import com.indago.io.DoubleTypeImgLoader;
import com.indago.io.projectfolder.ProjectFolder;
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

}
