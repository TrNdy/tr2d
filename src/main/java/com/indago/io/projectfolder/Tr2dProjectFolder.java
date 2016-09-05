/**
 *
 */
package com.indago.io.projectfolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.indago.app.hernan.Tr2dApplication;
import com.indago.io.ProjectFolder;
import com.indago.log.Log;

/**
 * @author jug
 */
public class Tr2dProjectFolder extends ProjectFolder {

	// FOLDERS
	public static String SEGMENTATION_FOLDER = "SEGMENTATION_FOLDER";
	public static String FLOW_FOLDER = "FLOW_FOLDER";
	public static String TRACKING_FOLDER = "TRACKING_FOLDER";

	// FILES
	public static String FRAME_PROPERTIES = "FRAME_PROPERTIES";
	public static String RAW_DATA = "RAW_DATA";

	/**
	 * @param id
	 * @param baseFolder
	 * @throws IOException
	 */
	public Tr2dProjectFolder( final File baseFolder ) throws IOException {
		super( "TR2D", baseFolder );
		addFile( RAW_DATA, "raw.tif" );
		addFile( FRAME_PROPERTIES, "frame.props" );
		try {
			addFolder( SEGMENTATION_FOLDER, "segmentation" );
			addFolder( FLOW_FOLDER, "flow" );
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
			Log.error( String.format( "Project folder (%s) could not be set up.", super.getAbsolutePath() ) );
			e.printStackTrace();
			Tr2dApplication.quit( 3 );
		}
	}

}
