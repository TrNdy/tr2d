/**
 *
 */
package com.indago.tr2d.ui.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import com.indago.tr2d.ui.model.Tr2dSolDiffModel;
import com.indago.tr2d.ui.model.Tr2dTrackingModel;
import com.indago.tr2d.ui.view.bdv.overlays.Tr2dFlowOverlay;
import com.indago.tr2d.ui.view.bdv.overlays.Tr2dTrackingOverlay;

import bdv.util.Bdv;
import bdv.util.BdvHandlePanel;
import indago.ui.progress.DialogProgress;
import net.imglib2.type.numeric.ARGBType;
import net.miginfocom.swing.MigLayout;

/**
 * @author jug
 */
public class Tr2dTrackingPanel extends JPanel implements ActionListener, FocusListener {

	private static final long serialVersionUID = -500536787731292765L;

	private final Tr2dTrackingModel model;

	private JTabbedPane tabs;

	private JCheckBox cbIterativeFixing;
	private JTextField txtMaxDelta;

	private JButton bRun;
	private JButton bRestart;
	private JButton bRefetch;

	private Tr2dFrameEditPanel frameEditPanel;

	private DialogProgress trackingProgressDialog;

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

		trackingProgressDialog = null;
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

		final JPanel panelMinDivDist = new JPanel( new MigLayout() );
		txtMaxDelta = new JTextField( "" + model.getMaxDelta(), 3 );
		txtMaxDelta.addActionListener( this );
		txtMaxDelta.addFocusListener( this );
		cbIterativeFixing = new JCheckBox( "only when violated" );
		cbIterativeFixing.setSelected( model.isIterativelySolvingMinDivDist() );
		cbIterativeFixing.addActionListener( this );
		panelMinDivDist.add( txtMaxDelta, "growx, wrap" );
		panelMinDivDist.add( cbIterativeFixing, "growx, wrap" );
		panelMinDivDist.setBorder( BorderFactory.createTitledBorder( "Min division dist." ) );

		bRun = new JButton( "track again" );
		bRun.addActionListener( this );
		bRestart = new JButton( "track" );
		bRestart.addActionListener( this );
		bRefetch = new JButton( "fetch & track" );
		bRefetch.addActionListener( this );

		model.bdvSetHandlePanel(
				new BdvHandlePanel( ( Frame ) this.getTopLevelAncestor(), Bdv
						.options()
						.is2D()
						.inputTriggerConfig( model.getTr2dModel().getDefaultInputTriggerConfig() ) ) );

		controls.add( panelMinDivDist, "growx, wrap" );

		controls.add( bRun, "growx, wrap" );
		controls.add( bRestart, "growx, wrap" );
		controls.add( bRefetch, "growx, wrap" );
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
		createProgressDialog();

		if ( e.getSource().equals( bRun ) ) {
			parseAndSetDivisionDistanceSettingsInModel();
			model.runInThread( false, false );
		} else if ( e.getSource().equals( bRestart ) ) {
			this.frameEditPanel.emptyUndoRedoStacks();
			parseAndSetDivisionDistanceSettingsInModel();
			model.runInThread( true, true );
			bRun.setEnabled( true );
		} else if ( e.getSource().equals( bRefetch ) ) {
			parseAndSetDivisionDistanceSettingsInModel();
			final Thread t = new Thread( new Runnable() {

				@Override
				public void run() {
					model.reset();
					model.runInThread( true, false );
				}
			} );
			t.start();
			bRun.setEnabled( true );
		} else if ( e.getSource().equals( txtMaxDelta ) ) {
			parseAndSetDivisionDistanceSettingsInModel();
			model.saveStateToFile();
		} else if ( e.getSource().equals( cbIterativeFixing ) ) {
			parseAndSetDivisionDistanceSettingsInModel();
			model.saveStateToFile();
			if ( !model.isIterativelySolvingMinDivDist() ) {
				bRun.setEnabled( false );
			}
		}
	}

	/**
	 *
	 */
	public void createProgressDialog() {
		if ( trackingProgressDialog == null ) {
			trackingProgressDialog = new DialogProgress( this, "Starting tracking...", 10 );
			model.addProgressListener( trackingProgressDialog );
		}
	}

	/**
	 * @see java.awt.event.FocusListener#focusGained(java.awt.event.FocusEvent)
	 */
	@Override
	public void focusGained( final FocusEvent e ) {}

	/**
	 * @see java.awt.event.FocusListener#focusLost(java.awt.event.FocusEvent)
	 */
	@Override
	public void focusLost( final FocusEvent e ) {
		if ( e.getSource().equals( txtMaxDelta ) ) {
			parseAndSetDivisionDistanceSettingsInModel();
			model.saveStateToFile();
			if ( !model.isIterativelySolvingMinDivDist() ) {
				bRun.setEnabled( false );
			}
		}
	}

	/**
	 *
	 */
	private void parseAndSetDivisionDistanceSettingsInModel() {
		try {
			final int maxDelta = Integer.parseInt( txtMaxDelta.getText() );
			model.setMaxDelta( maxDelta );
		} catch ( final NumberFormatException e ) {
			txtMaxDelta.setText( "" + model.getMaxDelta() );
		}
		try {
			model.setIterativelySolvingMinDivDist( cbIterativeFixing.isSelected() );
		} catch ( final NumberFormatException e ) {
			cbIterativeFixing.setSelected( true );
		}
	}
}
