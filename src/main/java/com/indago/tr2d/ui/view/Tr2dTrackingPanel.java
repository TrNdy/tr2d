/**
 *
 */
package com.indago.tr2d.ui.view;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

import com.indago.iddea.view.component.IddeaComponent;
import com.indago.tr2d.ui.model.Tr2dTrackingModel;

/**
 * @author jug
 */
public class Tr2dTrackingPanel extends JPanel implements ActionListener {

	private final Tr2dTrackingModel model;

	private JButton bRun;

	private IddeaComponent icSolution = null;


	public Tr2dTrackingPanel( final Tr2dTrackingModel trackingModel ) {
		super( new BorderLayout() );
		this.model = trackingModel;
		buildGui();
	}

	/**
	 * Builds the GUI of this panel.
	 */
	private void buildGui() {
		bRun = new JButton( "run" );
		bRun.addActionListener( this );

		icSolution = new IddeaComponent();
		icSolution.showMenu( false );
		icSolution.setToolBarLocation( BorderLayout.WEST );
		icSolution.setToolBarVisible( false );
//		icSegmentation.setPreferredSize( new Dimension( imgPlus.getWidth(), imgPlus.getHeight() ) );
//		icSegmentation.showStackSlider( true );
//		icSegmentation.showTimeSlider( true );

		this.add( bRun, BorderLayout.NORTH );
		this.add( icSolution, BorderLayout.CENTER );
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed( final ActionEvent e ) {
		if ( e.getSource().equals( bRun ) ) {
			model.run();
			icSolution.setSourceImage( model.getImgSolution() );
		}
	}
}
