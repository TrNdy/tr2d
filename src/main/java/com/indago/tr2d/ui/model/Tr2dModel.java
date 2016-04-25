/**
 *
 */
package com.indago.tr2d.ui.model;

import com.indago.io.projectfolder.ProjectFolder;
import com.indago.util.converter.RealDoubleNormalizeConverter;

import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;

/**
 * @author jug
 */
public class Tr2dModel {

	private final ImagePlus imgPlus;
	private RandomAccessibleInterval< DoubleType > imgOrig;

	private final ProjectFolder projectFolder;

	/**
	 *
	 * @param projectFolderBasePath
	 * @param imgPlus
	 */
	public Tr2dModel( final ProjectFolder projectFolder, final ImagePlus imgPlus ) {
		this.imgPlus = imgPlus;
		this.projectFolder = projectFolder;
		final Img< ? extends RealType > temp = ImagePlusAdapter.wrapNumeric( imgPlus );
		setImgOrig( Converters.convert(
				( RandomAccessibleInterval ) temp,
				new RealDoubleNormalizeConverter( 1.0 ),
				new DoubleType() ) );
	}

	/**
	 * @return the imgPlus
	 */
	public ImagePlus getImgPlus() {
		return imgPlus;
	}

	/**
	 * @return the imgOrig
	 */
	public RandomAccessibleInterval< DoubleType > getImgOrig() {
		return imgOrig;
	}

	/**
	 * @param imgOrig
	 *            the imgOrig to set
	 */
	public void setImgOrig( final RandomAccessibleInterval< DoubleType > imgOrig ) {
		this.imgOrig = imgOrig;
	}

	/**
	 * @return the imgOrigNorm
	 */
	public RandomAccessibleInterval< DoubleType > getImgOrigNorm() {
		return imgOrig;
	}

	/**
	 * @return the project folder tr2d uses
	 */
	public ProjectFolder getProjectFolder() {
		return projectFolder;
	}

}
