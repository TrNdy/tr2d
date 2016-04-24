/**
 *
 */
package com.indago.tr2d.ui.view;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import com.indago.tr2d.ui.model.Tr2dSegmentationCollectionModel;


/**
 * @author jug
 */
public class Tr2dSegmentationCollectionPanel extends JPanel {

	private final Tr2dSegmentationCollectionModel model;

	private JTabbedPane tabs;

	/**
	 * @param segModel
	 */
	public Tr2dSegmentationCollectionPanel( final Tr2dSegmentationCollectionModel segModel ) {
		super( new BorderLayout( 5, 5 ) );
		this.model = segModel;
		buildGui();
	}

	private void buildGui() {
		tabs = new JTabbedPane();
		final JPanel tabWeka = new Tr2dWekaSegmentationPanel( model.getWekaModel() );
		tabs.add( "Weka Segmentation", tabWeka );
		add( tabs, BorderLayout.CENTER );
	}

}
