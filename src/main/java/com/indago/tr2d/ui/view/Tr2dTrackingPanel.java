/**
 *
 */
package com.indago.tr2d.ui.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import com.indago.tr2d.ui.model.Tr2dTrackingModel;
import com.indago.tr2d.ui.view.bdv.overlays.Tr2dTrackingOverlay;

import bdv.util.Bdv;
import bdv.util.BdvHandlePanel;
import net.miginfocom.swing.MigLayout;

/**
 * @author jug
 */
public class Tr2dTrackingPanel extends JPanel implements ActionListener {

	private final Tr2dTrackingModel model;

	private JTabbedPane tabs;
	private JButton bEditFrame;
	private JButton bRun;

	private Tr2dFrameEditPanel frameEditPanel;

	public Tr2dTrackingPanel( final Tr2dTrackingModel trackingModel ) {
		super( new BorderLayout() );
		this.model = trackingModel;
		buildGui();

		model.bdvAdd( model.getTr2dModel().getRawData(), "RAW" );
		if ( model.getImgSolution() != null ) {
			model.bdvAdd( model.getImgSolution(), "solution" );
		}
		model.bdvAdd( new Tr2dTrackingOverlay( model ), "overlay" );
	}

	/**
	 * Builds the GUI of this panel.
	 */
	private void buildGui() {
		tabs = new JTabbedPane( JTabbedPane.TOP );
		tabs.add( "tracker", buildSolverPanel() );
		tabs.add( "frame editor", buildFrameEditPanel() );
		tabs.add( "track editor", buildTrackEditPanel() );

		this.add( tabs, BorderLayout.CENTER );
	}

	private Component buildFrameEditPanel() {
		frameEditPanel = new Tr2dFrameEditPanel( model );
		return frameEditPanel;
	}

	private Component buildTrackEditPanel() {
		final JPanel panel = new JPanel( new BorderLayout() );
		return panel;
	}

	private JPanel buildSolverPanel() {
		final JPanel panel = new JPanel( new BorderLayout() );

		final JPanel controls = new JPanel( new MigLayout() );
		final JPanel viewer = new JPanel( new BorderLayout() );

		final MigLayout layout = new MigLayout();
		final JPanel panelEdit = new JPanel( layout );
		panelEdit.setBorder( BorderFactory.createTitledBorder( "editing" ) );
		bEditFrame = new JButton( "frame" );
		bEditFrame.addActionListener( this );
		panelEdit.add( bEditFrame );

		bRun = new JButton( "track" );
		bRun.addActionListener( this );

		model.bdvSetHandlePanel( new BdvHandlePanel( ( Frame ) this.getTopLevelAncestor(), Bdv.options().is2D() ) );

		controls.add( panelEdit, "wrap" );
		controls.add( bRun, "growx, wrap" );
		viewer.add( model.bdvGetHandlePanel().getViewerPanel(), BorderLayout.CENTER );

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
					model.bdvRemoveAll();
					model.bdvAdd( model.getTr2dModel().getRawData(), "RAW" );
					model.bdvAdd( model.getImgSolution(), "solution" );
				}

			};
			new Thread( runnable ).start();
		} else if ( e.getSource().equals( bEditFrame ) ) {
			frameEditPanel.setFrameToShow( model.bdvGetHandlePanel().getBdvHandle().getViewerPanel().getState().getCurrentTimepoint() );
			frameEditPanel.selectionFromCurrentSolution();
			tabs.setSelectedComponent( frameEditPanel );
		}
	}
}
