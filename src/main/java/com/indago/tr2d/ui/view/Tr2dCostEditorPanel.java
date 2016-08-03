/**
 *
 */
package com.indago.tr2d.ui.view;

import javax.swing.JPanel;

import com.indago.tr2d.ui.model.Tr2dTrackingModel;


/**
 * @author jug
 */
public class Tr2dCostEditorPanel extends JPanel {

	private static final long serialVersionUID = 2601748326346367034L;

	private final Tr2dTrackingModel model;

	/**
	 * @param model
	 */
	public Tr2dCostEditorPanel( final Tr2dTrackingModel model ) {
		this.model = model;
	}
}
