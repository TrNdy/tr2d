/**
 *
 */
package com.indago.tr2d.ui.view;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

import com.indago.tr2d.ui.model.Tr2dFlowModel;
import com.indago.tr2d.ui.view.bdv.overlays.Tr2dFlowOverlay;

import bdv.util.Bdv;
import bdv.util.BdvHandlePanel;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import net.miginfocom.swing.MigLayout;

/**
 * @author jug
 */
public class Tr2dFlowPanel extends JPanel implements ActionListener {

	private static final long serialVersionUID = -637212854591745171L;

	private final Tr2dFlowModel model;

	public Tr2dFlowPanel( final Tr2dFlowModel model ) {
		super( new BorderLayout() );
		this.model = model;

		buildGui();
	}

	private void buildGui() {
		final JPanel viewer = new JPanel( new BorderLayout() );
		model.bdvSetHandlePanel( new BdvHandlePanel( ( Frame ) this.getTopLevelAncestor(), Bdv.options().is2D() ) );
		viewer.add( model.bdvGetHandlePanel().getViewerPanel(), BorderLayout.CENTER );

		// show loaded image
		RandomAccessibleInterval< FloatType > flowImg = model.getFlowImage();
		if ( flowImg == null ) {
			model.computeAndStoreFlow();
			flowImg = model.getFlowImage();
		}
		model.bdvAdd( model.getModel().getRawData(), "RAW" );
		model.bdvAdd( Views.hyperSlice( flowImg, 2, 0 ), "r" );
		model.bdvAdd( Views.hyperSlice( flowImg, 2, 1 ), "phi" );
		model.bdvAdd( new Tr2dFlowOverlay( model ), "overlay_flow" );

		final MigLayout layout = new MigLayout();
		final JPanel controls = new JPanel( layout );

		final JSplitPane splitPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, controls, viewer );
		splitPane.setResizeWeight( 0.1 ); // 1.0 == extra space given to left component alone!
		this.add( splitPane, BorderLayout.CENTER );
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed( final ActionEvent e ) {
	}
}
