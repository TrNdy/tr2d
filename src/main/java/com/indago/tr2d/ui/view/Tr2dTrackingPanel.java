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

import com.indago.tr2d.ui.model.Tr2dSolDiffModel;
import com.indago.tr2d.ui.model.Tr2dTrackingModel;
import com.indago.tr2d.ui.view.bdv.overlays.Tr2dFlowOverlay;
import com.indago.tr2d.ui.view.bdv.overlays.Tr2dTrackingOverlay;

import bdv.util.Bdv;
import bdv.util.BdvHandlePanel;
import net.imglib2.type.numeric.ARGBType;
import net.miginfocom.swing.MigLayout;

/**
 * @author jug
 */
public class Tr2dTrackingPanel extends JPanel implements ActionListener {

	private static final long serialVersionUID = -500536787731292765L;

	private final Tr2dTrackingModel model;

	private JTabbedPane tabs;
	private JButton bRun;
	private JButton bRestart;

	private Tr2dFrameEditPanel frameEditPanel;

	public Tr2dTrackingPanel( final Tr2dTrackingModel trackingModel ) {
		super( new BorderLayout() );
		this.model = trackingModel;

		buildGui();

		model.bdvAdd( model.getTr2dModel().getRawData(), "RAW" );
		if ( model.getImgSolution() != null ) {
			model.bdvAdd( model.getImgSolution(), "solution", 0, 5, new ARGBType( 0x00FF00 ), true );
		}
		model.bdvAdd( new Tr2dTrackingOverlay( model ), "overlay_tracking" );
		model.bdvAdd( new Tr2dFlowOverlay( model.getTr2dModel().getFlowModel() ), "overlay_flow", false );
	}

	/**
	 * Builds the GUI of this panel.
	 */
	private void buildGui() {
		tabs = new JTabbedPane( JTabbedPane.TOP );
		tabs.add( "tracker", buildSolverPanel() );
		tabs.add( "frame editor", buildFrameEditPanel() );
		tabs.add( "cost editor", buildCostEditorPanel() );

		this.add( tabs, BorderLayout.CENTER );
	}

	private Component buildFrameEditPanel() {
		frameEditPanel = new Tr2dFrameEditPanel( model );
		return frameEditPanel;
	}

	private Component buildCostEditorPanel() {
		final JPanel panel = new Tr2dCostEditorPanel( model, new Tr2dSolDiffModel( model ) );
		return panel;
	}

	private JPanel buildSolverPanel() {
		final JPanel panel = new JPanel( new BorderLayout() );

		final JPanel controls = new JPanel( new MigLayout() );
		final JPanel viewer = new JPanel( new BorderLayout() );

		bRun = new JButton( "retrack" );
		bRun.addActionListener( this );
		bRestart = new JButton( "fetch & track" );
		bRestart.addActionListener( this );

		model.bdvSetHandlePanel(
				new BdvHandlePanel( ( Frame ) this.getTopLevelAncestor(), Bdv
						.options()
						.is2D()
						.inputTriggerConfig( model.getTr2dModel().getDefaultInputTriggerConfig() ) ) );

		controls.add( bRun, "growx, wrap" );
		controls.add( bRestart, "growx, wrap" );
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
			model.runInThread( false );
		} else if ( e.getSource().equals( bRestart ) ) {
			model.reset();
			model.runInThread( true );
		}
	}
}
