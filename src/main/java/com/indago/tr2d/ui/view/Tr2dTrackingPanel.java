/**
 *
 */
package com.indago.tr2d.ui.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import com.indago.iddea.view.component.IddeaComponent;
import com.indago.tr2d.ui.model.Tr2dTrackingModel;

/**
 * @author jug
 */
public class Tr2dTrackingPanel extends JPanel implements ActionListener {

	private final Tr2dTrackingModel model;

	private JTabbedPane tabs;
	private JButton bRun;

	private IddeaComponent icSolution = null;


	public Tr2dTrackingPanel( final Tr2dTrackingModel trackingModel ) {
		super( new BorderLayout() );
		this.model = trackingModel;
		buildGui();
		if ( model.getImgSolution() != null ) {
			icSolution.setSourceImage( model.getImgSolution() );
		}
	}

	/**
	 * Builds the GUI of this panel.
	 */
	private void buildGui() {
		tabs = new JTabbedPane( JTabbedPane.TOP );
		tabs.add( "Frames", buildFramePanel() );
		tabs.add( "Tracking", buildTrackingPanel() );
		tabs.add( "Solver", buildSolverPanel() );
		this.add( tabs, BorderLayout.CENTER );
	}

	private Component buildFramePanel() {
		final JPanel panel = new JPanel( new BorderLayout() );
		return panel;
	}

	private Component buildTrackingPanel() {
		final JPanel panel = new JPanel( new BorderLayout() );
		return panel;
	}

	private JPanel buildSolverPanel() {
		final JPanel panel = new JPanel( new BorderLayout() );

		bRun = new JButton( "run" );
		bRun.addActionListener( this );

		icSolution = new IddeaComponent();
		icSolution.showMenu( false );
		icSolution.setToolBarLocation( BorderLayout.WEST );
		icSolution.setToolBarVisible( false );
//		icSegmentation.setPreferredSize( new Dimension( imgPlus.getWidth(), imgPlus.getHeight() ) );
//		icSegmentation.showStackSlider( true );
//		icSegmentation.showTimeSlider( true );

		panel.add( bRun, BorderLayout.NORTH );
		panel.add( icSolution, BorderLayout.CENTER );

		return panel;
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
