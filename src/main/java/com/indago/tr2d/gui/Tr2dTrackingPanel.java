/**
 *
 */
package com.indago.tr2d.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

import com.indago.tr2d.Tr2dTrackingModelHernan;

/**
 * @author jug
 */
public class Tr2dTrackingPanel extends JPanel implements ActionListener {

	private final Tr2dTrackingModelHernan model;

	private JButton bRun;

	public Tr2dTrackingPanel( final Tr2dTrackingModelHernan trackingModel ) {
		this.model = trackingModel;
		buildGui();
	}

	/**
	 * Builds the GUI of this panel.
	 */
	private void buildGui() {
		bRun = new JButton( "run" );
		bRun.addActionListener( this );

		this.add( bRun );
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed( final ActionEvent e ) {
		if ( e.getSource().equals( bRun ) ) {
			model.run();
		}
	}
}
