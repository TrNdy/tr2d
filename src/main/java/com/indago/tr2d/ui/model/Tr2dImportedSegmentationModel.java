/**
 *
 */
package com.indago.tr2d.ui.model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.indago.io.projectfolder.ProjectFile;
import com.indago.io.projectfolder.ProjectFolder;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.DoubleType;

/**
 * @author jug
 */
public class Tr2dImportedSegmentationModel {

	private final Tr2dSegmentationCollectionModel model;

	private ProjectFolder projectFolder;

	private final List< ProjectFile > files;
	private final List< RandomAccessibleInterval< DoubleType > > imgs;

	/**
	 * @param parentFolder
	 * @param model2
	 */
	public Tr2dImportedSegmentationModel( final Tr2dSegmentationCollectionModel tr2dSegmentationCollectionModel, final ProjectFolder parentFolder ) {
		this.model = tr2dSegmentationCollectionModel;
		files = new ArrayList< >();
		imgs = new ArrayList< >();

		try {
			this.projectFolder = parentFolder.addFolder( "imported" );
		} catch ( final IOException e ) {
			this.projectFolder = null;
			System.err.println( "ERROR: Subfolder for imported segmentation hypotheses could not be created." );
			e.printStackTrace();
		}
	}

	/**
	 * @return
	 */
	public List< RandomAccessibleInterval< DoubleType > > getSegmentHypotheses() {
		return imgs;
	}

	public void importSegmentation( final File f ) {

	}
}
