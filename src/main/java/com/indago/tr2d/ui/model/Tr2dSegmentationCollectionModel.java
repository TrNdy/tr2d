/**
 *
 */
package com.indago.tr2d.ui.model;

import com.indago.io.projectfolder.ProjectFolder;
import com.indago.tr2d.io.projectfolder.Tr2dProjectFolder;

/**
 * @author jug
 */
public class Tr2dSegmentationCollectionModel {

	private final Tr2dModel model;

	private final ProjectFolder projectFolder;

	private final Tr2dImportedSegmentationModel importedSegmentationModel;
	private final Tr2dWekaSegmentationModel wekaModel;

	/**
	 * @param model
	 */
	public Tr2dSegmentationCollectionModel( final Tr2dModel model ) {
		this.model = model;
		projectFolder = model.getProjectFolder().getFolder( Tr2dProjectFolder.SEGMENTATION_FOLDER );

		importedSegmentationModel = new Tr2dImportedSegmentationModel( this, projectFolder );
		wekaModel = new Tr2dWekaSegmentationModel( this, projectFolder );
	}

	/**
	 * @return the wekaModel
	 */
	public Tr2dImportedSegmentationModel getImportedSegmentationModel() {
		return importedSegmentationModel;
	}

	/**
	 * @return the wekaModel
	 */
	public Tr2dWekaSegmentationModel getWekaModel() {
		return wekaModel;
	}

	/**
	 * @return
	 */
	public ProjectFolder getProjectFolder() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @return
	 */
	public Tr2dModel getModel() {
		return model;
	}

}
