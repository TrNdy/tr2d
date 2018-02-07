package com.indago.tr2d.io.projectfolder;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Tr2dProjectFolderTest
{

	@Test(expected = IllegalArgumentException.class)
	public void test() throws IOException
	{
		Path directory = Files.createTempDirectory( "tr2d-project" );
		Path file = Files.createTempFile(directory, "temp", ".tif" );
		Tr2dProjectFolder projectFolder = new Tr2dProjectFolder( directory.toFile() );
		projectFolder.restartWithRawDataFile( file.toString() );
	}
}
