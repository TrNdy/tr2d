/**
 *
 */
package com.indago.tr2d.ui.view;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import com.indago.tr2d.Tr2dContext;
import com.indago.tr2d.plugins.seg.Tr2dSegmentationPlugin;
import com.indago.tr2d.ui.model.Tr2dSegmentationCollectionModel;


/**
 * @author jug
 */
public class Tr2dSegmentationCollectionPanel extends JPanel {

	private static final long serialVersionUID = -6328417825503609386L;

	private final Tr2dSegmentationCollectionModel model;

	private JTabbedPane tabs;

	public Tr2dSegmentationCollectionPanel( final Tr2dSegmentationCollectionModel segModel ) {
		super( new BorderLayout( 5, 5 ) );
		this.model = segModel;
		buildGui();
	}

	private void buildGui() {
		tabs = new JTabbedPane();

		for ( final String name : Tr2dContext.segPlugins.getPluginNames() ) {
			final Tr2dSegmentationPlugin segPlugin =
					Tr2dContext.segPlugins.createPlugin( name, model.getModel(), model.getModel().getMainPanel().getLogPanel() );
			if(segPlugin.isUsable()) {
				model.addPlugin( segPlugin );
				tabs.add( segPlugin.getUiName(), segPlugin.getInteractionPanel() );
			}
		}

		add( tabs, BorderLayout.CENTER );
	}

}
