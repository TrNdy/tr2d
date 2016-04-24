/**
 *
 */
package com.indago.tr2d.ui.model;

/**
 * @author jug
 */
public class Tr2dSegmentationCollectionModel {

	private final Tr2dWekaSegmentationModel wekaModel;

	/**
	 * @param model
	 */
	public Tr2dSegmentationCollectionModel( final Tr2dModel model ) {
		wekaModel = new Tr2dWekaSegmentationModel( model );
	}

	/**
	 * @return the wekaModel
	 */
	public Tr2dWekaSegmentationModel getWekaModel() {
		return wekaModel;
	}

}
