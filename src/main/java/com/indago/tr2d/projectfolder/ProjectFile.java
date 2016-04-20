/**
 *
 */
package com.indago.tr2d.projectfolder;

import java.io.File;

/**
 * @author jug
 */
public class ProjectFile {

	private final String hint;
	private final String filename;
	private final String path;

	public ProjectFile( final String hint, final String path, final String filename ) {
		this.hint = hint;
		this.path = path;
		this.filename = filename;
	}

	public String getHint() {
		return hint;
	}

	public String getFilename() {
		return filename;
	}

	public String getRelPath() {
		if ( filename.equals( "" ) ) {
			return path;
		} else {
			if ( path.equals( "" ) ) {
				return filename;
			} else {
				return path + File.separator + filename;
			}
		}
	}

	public String getAbsolutePathIn( final String projectBasePath ) {
		return projectBasePath + File.separator + getRelPath();
	}

	public String getAbsolutePathIn( final File projectBasePath ) {
		return projectBasePath.getAbsolutePath() + File.separator + getRelPath();
	}

	public boolean existsIn( final String projectBasePath ) {
		return existsIn( new File( getAbsolutePathIn( projectBasePath ) ) );
	}

	public boolean existsIn( final File projectBasePath ) {
		return projectBasePath.exists();
	}

	public boolean readableIn( final String projectBasePath ) {
		return new File( getAbsolutePathIn( projectBasePath ) ).canRead();
	}

	public boolean writeableIn( final String projectBasePath ) {
		return new File( getAbsolutePathIn( projectBasePath ) ).canWrite();
	}
}
