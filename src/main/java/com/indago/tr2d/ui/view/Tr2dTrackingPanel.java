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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.indago.tr2d.ui.model.Tr2dSolDiffModel;
import com.indago.tr2d.ui.model.Tr2dTrackingModel;

import bdv.util.Bdv;
import bdv.util.BdvHandlePanel;
import indago.ui.progress.DialogProgress;
import net.miginfocom.swing.MigLayout;

/**
 * @author jug
 */
public class Tr2dTrackingPanel extends JPanel implements ActionListener, FocusListener, ChangeListener {

	private static final long serialVersionUID = -500536787731292765L;

	private final Tr2dTrackingModel model;

	private JTabbedPane tabs;
	private JButton bRun;
	private JButton bRestart;
	private JButton bFetch;

	private Tr2dFrameEditPanel frameEditPanel;

	private DialogProgress trackingProgressDialog;

	private JTextField txtMaxMovementsPerNode;
	private JTextField txtMaxDivisionsPerNode;
	private JTextField txtMaxMovementSearchRadius;
	private JTextField txtMaxDivisionSearchRadius;
	private JTextField txtMaxPixelComponentSize;
	private JTextField txtMinPixelComponentSize;

	public Tr2dTrackingPanel( final Tr2dTrackingModel trackingModel ) {
		super( new BorderLayout() );
		this.model = trackingModel;
		model.addStateChangedListener( this );

		buildGui();
		model.populateBdv();

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

		final JPanel panelFetch = new JPanel( new MigLayout() );
		panelFetch.setBorder( BorderFactory.createTitledBorder( "segmentation fetching" ) );

		txtMaxPixelComponentSize = new JTextField( 5 );
		txtMaxPixelComponentSize.addActionListener( this );
		txtMaxPixelComponentSize.addFocusListener( this );
		txtMinPixelComponentSize = new JTextField( 5 );
		txtMinPixelComponentSize.addActionListener( this );
		txtMinPixelComponentSize.addFocusListener( this );

		bFetch = new JButton( "fetch" );
		bFetch.addActionListener( this );

		panelFetch.add( new JLabel( "Max segment size:" ), "growx" );
		panelFetch.add( txtMaxPixelComponentSize, "growx, wrap" );
		panelFetch.add( new JLabel( "Min segment size:" ), "growx" );
		panelFetch.add( txtMinPixelComponentSize, "growx, wrap" );
		panelFetch.add( bFetch, "growx, wrap" );

		final JPanel panelGraphConstructionParams = new JPanel( new MigLayout() );
		txtMaxMovementSearchRadius = new JTextField( 3 );
		txtMaxMovementSearchRadius.addActionListener( this );
		txtMaxMovementSearchRadius.addFocusListener( this );
		txtMaxMovementsPerNode = new JTextField( 3 );
		txtMaxMovementsPerNode.addActionListener( this );
		txtMaxMovementsPerNode.addFocusListener( this );
		txtMaxDivisionSearchRadius = new JTextField( 3 );
		txtMaxDivisionSearchRadius.addActionListener( this );
		txtMaxDivisionSearchRadius.addFocusListener( this );
		txtMaxDivisionsPerNode = new JTextField( 3 );
		txtMaxDivisionsPerNode.addActionListener( this );
		txtMaxDivisionsPerNode.addFocusListener( this );

		bRun = new JButton( "track" );
		bRun.addActionListener( this );
		bRestart = new JButton( "restart" );
		bRestart.addActionListener( this );

		panelGraphConstructionParams.setBorder( BorderFactory.createTitledBorder( "graph parameters" ) );
		panelGraphConstructionParams.add( new JLabel( "Max move radius:" ), "growx" );
		panelGraphConstructionParams.add( txtMaxMovementSearchRadius, "growx, wrap" );
		panelGraphConstructionParams.add( new JLabel( "Max move assmts:" ), "growx" );
		panelGraphConstructionParams.add( txtMaxMovementsPerNode, "growx, wrap" );
		panelGraphConstructionParams.add( new JLabel( "Max division radius:" ), "growx" );
		panelGraphConstructionParams.add( txtMaxDivisionSearchRadius, "growx, wrap" );
		panelGraphConstructionParams.add( new JLabel( "Max division assmts:" ), "growx" );
		panelGraphConstructionParams.add( txtMaxDivisionsPerNode, "growx, wrap" );
		panelGraphConstructionParams.add( bRun, "growx, wrap" );
		panelGraphConstructionParams.add( bRestart, "growx, wrap" );

		model.bdvSetHandlePanel(
				new BdvHandlePanel( ( Frame ) this.getTopLevelAncestor(), Bdv
						.options()
						.is2D()
						.inputTriggerConfig( model.getTr2dModel().getDefaultInputTriggerConfig() ) ) );

		controls.add( panelFetch, "growx, wrap" );
		controls.add( panelGraphConstructionParams, "growx, wrap" );

		viewer.add( model.bdvGetHandlePanel().getViewerPanel(), BorderLayout.CENTER );

		final JSplitPane splitPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, controls, viewer );
		panel.add( splitPane, BorderLayout.CENTER );

		populateTextFieldsViaModelValues();

		return panel;
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed( final ActionEvent e ) {
		if ( trackingProgressDialog == null ) {
			trackingProgressDialog = new DialogProgress( this, "Starting tracking...", 10 );
			model.addProgressListener( trackingProgressDialog );
		}

		if ( e.getSource().equals( bRun ) ) {
			model.runInThread( false );
		} else if ( e.getSource().equals( bRestart ) ) {
			this.frameEditPanel.emptyUndoRedoStacks();
			model.runInThread( true, true );
		} else if ( e.getSource().equals( bFetch ) ) {
			final Thread t = new Thread( new Runnable() {

				@Override
				public void run() {
					model.fetch();
					model.populateBdv();
				}
			} );
			t.start();
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
		if ( e.getSource().equals( txtMaxMovementSearchRadius ) || e.getSource().equals( txtMaxMovementsPerNode ) || e.getSource().equals(
				txtMaxDivisionSearchRadius ) || e.getSource().equals( txtMaxDivisionsPerNode ) || e.getSource().equals(
						txtMaxPixelComponentSize ) || e.getSource().equals( txtMinPixelComponentSize ) ) {
			parseAndSetParametersInModel();
			model.saveStateToFile();
		}
	}

	/**
	 *
	 */
	private void parseAndSetParametersInModel() {
		try {
			model.setMaxMovementSearchRadius( Integer.parseInt( txtMaxMovementSearchRadius.getText() ) );
		} catch ( final NumberFormatException e ) {
			txtMaxMovementSearchRadius.setText( "" + model.getMaxMovementSearchRadius() );
		}
		try {
			model.setMaxMovementsToAddPerHypothesis( Integer.parseInt( txtMaxMovementsPerNode.getText() ) );
		} catch ( final NumberFormatException e ) {
			txtMaxMovementsPerNode.setText( "" + model.getMaxMovementsToAddPerHypothesis() );
		}
		try {
			model.setMaxDivisionSearchRadius( Integer.parseInt( txtMaxDivisionSearchRadius.getText() ) );
		} catch ( final NumberFormatException e ) {
			txtMaxDivisionSearchRadius.setText( "" + model.getMaxDivisionSearchRadius() );
		}
		try {
			model.setMaxDivisionsToAddPerHypothesis( Integer.parseInt( txtMaxDivisionsPerNode.getText() ) );
		} catch ( final NumberFormatException e ) {
			txtMaxDivisionsPerNode.setText( "" + model.getMaxDivisionsToAddPerHypothesis() );
		}
		try {
			if ( txtMaxPixelComponentSize.getText().trim().isEmpty() ) {
				model.setMaxPixelComponentSize( Integer.MAX_VALUE );
			} else {
				model.setMaxPixelComponentSize( Integer.parseInt( txtMaxPixelComponentSize.getText() ) );
			}
		} catch ( final NumberFormatException e ) {
			txtMaxPixelComponentSize.setText( "" + model.getMaxPixelComponentSize() );
		}
		try {
			model.setMinPixelComponentSize( Integer.parseInt( txtMinPixelComponentSize.getText() ) );
		} catch ( final NumberFormatException e ) {
			txtMinPixelComponentSize.setText( "" + model.getMinPixelComponentSize() );
		}
	}

	/**
	 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
	 */
	@Override
	public void stateChanged( final ChangeEvent e ) {
		populateTextFieldsViaModelValues();
	}

	private void populateTextFieldsViaModelValues() {
		this.txtMaxMovementSearchRadius.setText( "" + model.getMaxMovementSearchRadius() );
		this.txtMaxMovementsPerNode.setText( "" + model.getMaxMovementsToAddPerHypothesis() );
		this.txtMaxDivisionSearchRadius.setText( "" + model.getMaxDivisionSearchRadius() );
		this.txtMaxDivisionsPerNode.setText( "" + model.getMaxDivisionsToAddPerHypothesis() );
		this.txtMaxPixelComponentSize.setText( "" + model.getMaxPixelComponentSize() );
		this.txtMinPixelComponentSize.setText( "" + model.getMinPixelComponentSize() );
	}
}
