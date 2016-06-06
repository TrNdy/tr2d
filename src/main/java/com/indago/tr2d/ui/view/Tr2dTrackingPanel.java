/**
 *
 */
package com.indago.tr2d.ui.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
	private JButton bRun;

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
		tabs.add( "Frame models", buildFramePanel() );
		tabs.add( "Tracking model", buildTrackingPanel() );
		tabs.add( "Solver", buildSolverPanel() );

		this.add( tabs, BorderLayout.CENTER );
	}

	private Component buildFramePanel() {
		final JPanel panel = new Tr2dFrameInteractionPanel( model );
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

		model.bdvSetHandlePanel( new BdvHandlePanel( ( Frame ) this.getTopLevelAncestor(), Bdv.options().is2D() ) );

		controls.add( bRun );
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
		}
	}
}
