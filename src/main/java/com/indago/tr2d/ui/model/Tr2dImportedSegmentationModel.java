/**
 *
 */
package com.indago.tr2d.ui.model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.ListModel;

import org.python.google.common.io.Files;

import com.indago.io.DoubleTypeImgLoader;
import com.indago.io.projectfolder.ProjectFile;
import com.indago.io.projectfolder.ProjectFolder;
import com.jgoodies.common.collect.LinkedListModel;

import io.scif.img.ImgIOException;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.DoubleType;
import weka.gui.ExtensionFileFilter;

/**
 * @author jug
 */
public class Tr2dImportedSegmentationModel {

	private final Tr2dSegmentationCollectionModel model;

	private ProjectFolder projectFolder;

	private final Vector< ProjectFile > files;
	private final List< RandomAccessibleInterval< DoubleType > > imgs;

	private LinkedListModel< ProjectFile > linkedListModel = null;

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
		files = new Vector< ProjectFile >( projectFolder.getFiles( new ExtensionFileFilter( "tif", "TIFF Image Stack" ) ) );
		for ( final ProjectFile pf : files ) {
			try {
				final RandomAccessibleInterval< DoubleType > rai = DoubleTypeImgLoader.loadTiffEnsureDoubleType( pf.getFile() );
				imgs.add( rai );
			} catch ( final ImgIOException e ) {
				e.printStackTrace();
			}
		}
		linkedListModel = new LinkedListModel< >( getLoadedFiles() );
	}

	/**
	 * @return
	 */
	public List< RandomAccessibleInterval< DoubleType > > getSegmentHypothesesImages() {
		return imgs;
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

	/**
	 * Copies the given file into the project folder.
	 *
	 * @param f
	 * @throws IOException
	 * @throws ImgIOException
	 */
	public void importSegmentation( final File f ) throws IOException, ImgIOException {
		final ProjectFile pf = projectFolder.addFile( f.getName() );
		Files.copy( f, pf.getFile() );

		files.add( pf );
		linkedListModel.add( pf );
		RandomAccessibleInterval< DoubleType > rai;
		rai = DoubleTypeImgLoader.loadTiffEnsureDoubleType( pf.getFile() );
		imgs.add( rai );
	}

	/**
	 * @param idx
	 */
	public void removeSegmentation( final int idx ) {
		final ProjectFile pf = files.remove( idx );
		linkedListModel.remove( pf );
		imgs.remove( idx );

		if ( !pf.getFile().delete() ) {
			System.err.println( String.format( "ERROR: imported segmentation file %s could not be deleted from project folder.", pf ) );
		}
	}

	/**
	 * @return
	 */
	public ListModel< ProjectFile > getListModel() {
		return linkedListModel;
	}
}
