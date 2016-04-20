/**
 *
 */
package com.indago.tr2d.projectfolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;

/**
 * @author jug
 */
public class ProjectData {

	public static final ProjectFile RAW_DATA = new ProjectFile( "RAW tiff stack", "", "raw.tif" );

	/**
	 * Delets a given project base folder (recursively), creats it again (empty)
	 * and copies the given raw data tiff file into it.
	 *
	 * @param baseFolder
	 * @param absolutePathRawData
	 */
	public static void restartWith( final File baseFolder, final String absolutePathRawData ) {
		try {
			FileUtils.deleteDirectory( baseFolder );
		} catch ( final IOException e ) {
			System.err.println( String.format( "ERROR: Project base folder could not be reset!", baseFolder ) );
			e.printStackTrace();
			System.exit( 1 );
		}
		if ( !baseFolder.mkdirs() ) {
			System.err.println( String.format( "ERROR: Given baseFolder (%s) could not be created!", baseFolder ) );
			System.exit( 2 );
		} else {
			try {
				Files.copy( new File( absolutePathRawData ).toPath(), new File( RAW_DATA.getAbsPathIn( baseFolder.getAbsolutePath() ) ).toPath() );
			} catch ( final IOException e ) {
				System.err.println( String.format( "ERROR: Project base folder could not be reset!", baseFolder ) );
				e.printStackTrace();
				System.exit( 3 );
			}
		}
	}
}
