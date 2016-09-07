/**
 *
 */
package com.indago.tr2d.ui.view;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;

import com.indago.app.hernan.Tr2dApplication;
import com.indago.tr2d.ui.model.Tr2dFlowModel;
import com.indago.tr2d.ui.view.bdv.overlays.Tr2dFlowOverlay;

import bdv.util.Bdv;
import bdv.util.BdvHandlePanel;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;
import net.miginfocom.swing.MigLayout;

/**
 * @author jug
 */
public class Tr2dFlowPanel extends JPanel implements ActionListener {

	private static final long serialVersionUID = -637212854591745171L;

	private final Tr2dFlowModel model;

	private JTextField txtScaleFactor;
	private JTextField txtBlockRadius;
	private JTextField txtMaxDist;
	private JButton btnComputeFlow;
	private JButton btnRemoveFlow;

	public Tr2dFlowPanel( final Tr2dFlowModel model ) {
		super( new BorderLayout() );
		this.model = model;

		buildGui();
	}

	private void buildGui() {
		final JPanel viewer = new JPanel( new BorderLayout() );
		model.bdvSetHandlePanel(
				new BdvHandlePanel( ( Frame ) this.getTopLevelAncestor(), Bdv
						.options()
						.is2D()
						.inputTriggerConfig( model.getModel().getDefaultInputTriggerConfig() ) ) );
		viewer.add( model.bdvGetHandlePanel().getViewerPanel(), BorderLayout.CENTER );

		// show loaded image
		final RandomAccessibleInterval< FloatType > flowImg = model.getFlowImage();
		model.bdvAdd( model.getModel().getRawData(), "RAW" );
		if ( flowImg != null ) {
//			model.bdvAdd( Views.hyperSlice( flowImg, 2, 0 ), "r", false );
//			model.bdvAdd( Views.hyperSlice( flowImg, 2, 1 ), "phi", false );
			model.bdvAdd( new Tr2dFlowOverlay( model ), "overlay_flow" );
		}

		final MigLayout layout = new MigLayout( "", "[][grow]", "" );
		final JPanel controls = new JPanel( layout );

		final JLabel labelScaleFactor = new JLabel( "Scale factor:" );
		txtScaleFactor = new JTextField( "" + model.getScaleFactor() );
		final JLabel labelBlockRadius = new JLabel( "Block radius:" );
		txtBlockRadius = new JTextField( "" + model.getBlockRadius() );
		final JLabel labelMaxDist = new JLabel( "Max. distance:" );
		txtMaxDist = new JTextField( "" + model.getMaxDistance() );
		btnComputeFlow = new JButton( "compute flow" );
		btnComputeFlow.addActionListener( this );
		btnRemoveFlow = new JButton( "remove flow" );
		btnRemoveFlow.addActionListener( this );

		controls.add( labelScaleFactor );
		controls.add( txtScaleFactor, "growx, wrap" );
		controls.add( labelBlockRadius );
		controls.add( txtBlockRadius, "growx, wrap" );
		controls.add( labelMaxDist );
		controls.add( txtMaxDist, "growx, wrap" );
		controls.add( btnComputeFlow, "span, growx, wrap" );
		controls.add( btnRemoveFlow, "span, growx, wrap" );

		final JSplitPane splitPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, controls, viewer );
		splitPane.setResizeWeight( 0.1 ); // 1.0 == extra space given to left component alone!
		this.add( splitPane, BorderLayout.CENTER );
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed( final ActionEvent e ) {
		if ( e.getSource().equals( btnComputeFlow ) ) {

			try {
				model.setScaleFactor( Double.parseDouble( txtScaleFactor.getText() ) );
				model.setBlockRadius( Integer.parseInt( txtBlockRadius.getText() ) );
				model.setMaxDistance( Integer.parseInt( txtMaxDist.getText() ) );
				model.computeAndStoreFlow();

				final RandomAccessibleInterval< FloatType > flowImg = model.getFlowImage();
				model.bdvRemoveAll();
				model.bdvRemoveAllOverlays();
				model.bdvAdd( model.getModel().getRawData(), "RAW" );
//				model.bdvAdd( Views.hyperSlice( flowImg, 2, 0 ), "r", false );
//				model.bdvAdd( Views.hyperSlice( flowImg, 2, 1 ), "phi", false );
				model.bdvAdd( new Tr2dFlowOverlay( model ), "overlay_flow" );

			} catch ( final NumberFormatException nfe ) {
				Tr2dApplication.log.error( "NumberFormatException@tr2dFlowPanel: " + nfe.getMessage() );
			}
		} else
		if ( e.getSource().equals( btnRemoveFlow ) ) {
			model.removeFlowFiles();
			final RandomAccessibleInterval< FloatType > flowImg = null;
			model.bdvRemoveAllOverlays();
			model.bdvRemoveAll();
			model.bdvAdd( model.getModel().getRawData(), "RAW" );
		}
	}
}
