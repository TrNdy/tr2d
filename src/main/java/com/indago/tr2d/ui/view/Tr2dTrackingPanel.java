/**
 *
 */
package com.indago.tr2d.ui.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;

import com.indago.iddea.view.component.IddeaComponent;
import com.indago.tr2d.ui.model.Tr2dTrackingModel;
import com.indago.tr2d.ui.util.MessageConsole;

import net.miginfocom.swing.MigLayout;

/**
 * @author jug
 */
public class Tr2dTrackingPanel extends JPanel implements ActionListener {

	private final Tr2dTrackingModel model;

	private JTabbedPane tabs;
	private MessageConsole log;
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
		tabs.add( "Frame models", buildFramePanel() );
		tabs.add( "Tracking model", buildTrackingPanel() );
		tabs.add( "Solver", buildSolverPanel() );

		final JPanel logPanel = new JPanel( new BorderLayout() );
		final JTextPane logText = new JTextPane();
		logPanel.add( new JScrollPane( logText ), BorderLayout.CENTER );
		log = new MessageConsole( logText, true );
		log.redirectOut();
		log.redirectErr( Color.RED, null );
		log.setMessageLines( 10000 );

		final JSplitPane splitPane = new JSplitPane( JSplitPane.VERTICAL_SPLIT, tabs, logPanel );
		splitPane.setResizeWeight( .8 ); // 1.0 == extra space given to top component alone!
		this.add( splitPane, BorderLayout.CENTER );
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

		final JPanel controls = new JPanel( new MigLayout() );
		final JPanel viewer = new JPanel( new BorderLayout() );

		bRun = new JButton( "run..." );
		bRun.addActionListener( this );

		icSolution = new IddeaComponent();
		icSolution.showMenu( false );
		icSolution.setToolBarLocation( BorderLayout.WEST );
		icSolution.setToolBarVisible( false );
//		icSegmentation.setPreferredSize( new Dimension( imgPlus.getWidth(), imgPlus.getHeight() ) );
//		icSegmentation.showStackSlider( true );
//		icSegmentation.showTimeSlider( true );

		controls.add( bRun );
		viewer.add( icSolution, BorderLayout.CENTER );

		final JSplitPane splitPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, controls, viewer );
		panel.add( splitPane, BorderLayout.CENTER );

		return panel;
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed( final ActionEvent e ) {
		if ( e.getSource().equals( bRun ) ) {
			final Runnable runnable = new Runnable(){

				@Override
				public void run() {
					model.run();
					icSolution.setSourceImage( model.getImgSolution() );
				}

			};
			new Thread( runnable ).start();
		}
	}
}
