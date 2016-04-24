/**
 *
 */
package com.indago.tr2d.ui.model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import com.indago.io.DoubleTypeImgLoader;
import com.indago.io.projectfolder.ProjectFile;
import com.indago.io.projectfolder.ProjectFolder;

import io.scif.img.ImgIOException;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.DoubleType;

/**
 * @author jug
 */
public class Tr2dImportedSegmentationModel {

	private final Tr2dSegmentationCollectionModel model;

	private ProjectFolder projectFolder;

	private final Vector< ProjectFile > files;
	private final List< RandomAccessibleInterval< DoubleType > > imgs;

	/**
	 * @param parentFolder
	 * @param model2
	 */
	public Tr2dImportedSegmentationModel( final Tr2dSegmentationCollectionModel tr2dSegmentationCollectionModel, final ProjectFolder parentFolder ) {
		this.model = tr2dSegmentationCollectionModel;

		try {
			this.projectFolder = parentFolder.addFolder( "imported" );
			this.projectFolder.loadFiles();
		} catch ( final IOException e ) {
			this.projectFolder = null;
			System.err.println( "ERROR: Subfolder for imported segmentation hypotheses could not be created." );
			e.printStackTrace();
		}

		imgs = new ArrayList< >();
		files = new Vector< >( projectFolder.getFiles() );
		for ( final ProjectFile pf : files ) {
			try {
				final RandomAccessibleInterval< DoubleType > rai = DoubleTypeImgLoader.loadTiffEnsureFloatType( pf.getFile() );
				imgs.add( rai );
			} catch ( final ImgIOException e ) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * @return
	 */
	public List< RandomAccessibleInterval< DoubleType > > getSegmentHypothesesImages() {
		return imgs;
	}

	public void importSegmentation( final File f ) {

	}

	/**
	 * @return the projectFolder
	 */
	public ProjectFolder getProjectFolder() {
		return projectFolder;
	}

	/**
	 * @return
	 */
	public Tr2dSegmentationCollectionModel getModel() {
		return model;
	}

	/**
	 * @return
	 */
	public Vector< ProjectFile > getLoadedFiles() {
		return files;
	}
}
