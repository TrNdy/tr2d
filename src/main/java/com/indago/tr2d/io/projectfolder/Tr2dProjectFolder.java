/**
 *
 */
package com.indago.tr2d.io.projectfolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.indago.io.projectfolder.ProjectFolder;

/**
 * @author jug
 */
public class Tr2dProjectFolder extends ProjectFolder {

	// FOLDERS
	public static String WEKA_SEGMENTATION_FOLDER = "WEKA_SEGMENTATION_FOLDER";
	public static String TRACKING_FOLDER = "TRACKING_FOLDER";

	// FILES
	public static String RAW_DATA = "RAW_DATA";

	/**
	 * @param id
	 * @param baseFolder
	 * @throws IOException
	 */
	public Tr2dProjectFolder( final File baseFolder ) throws IOException {
		super( "TR2D", baseFolder );
		addFile( RAW_DATA, "raw.tif" );
		try {
			addFolder( WEKA_SEGMENTATION_FOLDER, "segmentation_weka" );
			addFolder( TRACKING_FOLDER, "tracking" );
		} catch ( final IOException ioe ) {
			ioe.printStackTrace();
		}
	}

	/**
	 * @param absolutePath
	 */
	public void restartWithRawDataFile( final String pathToRawDataFile ) {
		try {
			deleteContent();
			Files.copy( new File( pathToRawDataFile ).toPath(), getFile( RAW_DATA ).getFile().toPath() );
		} catch ( final IOException e ) {
			System.err.println( String.format( "ERROR: Project folder (%s) could not be set up.", super.getAbsolutePath() ) );
			e.printStackTrace();
			System.exit( 3 );
		}
	}

}
