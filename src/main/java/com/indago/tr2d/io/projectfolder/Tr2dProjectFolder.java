/**
 *
 */
package com.indago.tr2d.io.projectfolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.indago.io.ProjectFolder;

/**
 * @author jug
 */
public class Tr2dProjectFolder extends ProjectFolder {

	// FOLDERS
	public static String SEGMENTATION_FOLDER = "SEGMENTATION_FOLDER";
	public static String FLOW_FOLDER = "FLOW_FOLDER";
	public static String TRACKING_FOLDER = "TRACKING_FOLDER";
	public static String MANUAL_SEGMENTATION_FOLDER = "MANUAL_SEGMENTATION_FOLDER";

	// FILES
	public static String FRAME_PROPERTIES = "FRAME_PROPERTIES";
	public static String RAW_DATA = "RAW_DATA";

	public Tr2dProjectFolder( final File baseFolder ) throws IOException {
		super( "TR2D", baseFolder );
	}

	public void initialize() throws IOException {
		if ( Tr2dProjectFolder.isValidProjectFolder( super.getFolder() ) ) {
			addFile( RAW_DATA, "raw.tif" );
			addFile( FRAME_PROPERTIES, "frame.props" );
		} else {
			System.out.println( "Not a valid project folder to initialize!" );
		}
		try {
			addFolder( SEGMENTATION_FOLDER, "segmentation" );
			addFolder( FLOW_FOLDER, "flow" );
			addFolder( TRACKING_FOLDER, "tracking" );
			addFolder( MANUAL_SEGMENTATION_FOLDER, "manual_segmentation");
		} catch ( final IOException ioe ) {
			ioe.printStackTrace();
		}
	}

	public void initialize( final File rawToCopy ) throws IOException {
		deleteContent();
		Files.copy( rawToCopy.toPath(), new File( super.getFolder() + File.separator + "RAW.tif" ).toPath() );
		initialize();
	}

	/**
	 * checks if the given <code>File</code> is a folder, if write access is
	 * allowed, and if a 'RAW.tif' file is contained in it.
	 *
	 * @param folder
	 *            Folder to be checked
	 * @return true or false
	 */
	public static boolean isValidProjectFolder( final File folder ) {
		if (!folder.isDirectory()) return false;
		if (!folder.canWrite()) return false;

		boolean rawExists = false;
		final File[] content = folder.listFiles();
		for ( final File f : content ) {
			if ( f.getName().toLowerCase().contains( "raw.tif" ) ) {
				rawExists = true;
				break;
			}
		}
		return rawExists;
	}

}
