/**
 *
 */
package com.indago.tr2d.ui.view;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.indago.app.hernan.costs.HernanAppearanceCostFactory;
import com.indago.app.hernan.costs.HernanDisappearanceCostFactory;
import com.indago.app.hernan.costs.HernanDivisionCostFactory;
import com.indago.app.hernan.costs.HernanMappingCostFactory;
import com.indago.app.hernan.costs.HernanSegmentCostFactory;
import com.indago.tr2d.ui.model.Tr2dModel;
import com.indago.tr2d.ui.model.Tr2dSegmentationCollectionModel;
import com.indago.tr2d.ui.model.Tr2dTrackingModel;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandlePanel;
import bdv.util.BdvSource;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.DoubleType;

/**
 * @author jug
 */
public class Tr2dMainPanel extends JPanel implements ActionListener, ChangeListener {

	private final Frame frame;

	private final Tr2dModel model;

	private JTabbedPane tabs;
	private JPanel tabData;
	private JPanel tabSegmentation;
	private JPanel tabTracking;

	private BdvHandlePanel bdvData;

	/**
	 * @param imgPlus
	 */
	@SuppressWarnings( { "rawtypes", "unchecked" } )
	public Tr2dMainPanel( final Frame frame, final Tr2dModel model ) {
		super( new BorderLayout( 5, 5 ) );

		setBorder( BorderFactory.createEmptyBorder( 10, 15, 5, 15 ) );
		this.frame = frame;
		this.model = model;

		buildGui();

		//final BdvSource show =
		final BdvSource source = BdvFunctions.show(
				model.getRawData(),
				"RAW",
				Bdv.options().addTo( bdvData ) );
		source.setDisplayRangeBounds( 0, model.getMaxRawValue() );
		source.setDisplayRange( model.getMinRawValue(), model.getMaxRawValue() );

	}

	private void buildGui() {

		// === TAB DATA ===========================================================================
		tabs = new JTabbedPane();
		tabData = new JPanel( new BorderLayout() );
		bdvData = new BdvHandlePanel( frame, Bdv.options().is2D() );
		tabData.add( bdvData.getViewerPanel(), BorderLayout.CENTER );

		// === TAB SEGMENTATION ===================================================================
		final Tr2dSegmentationCollectionModel segModel = new Tr2dSegmentationCollectionModel( model );
		tabSegmentation = new Tr2dSegmentationCollectionPanel( segModel );

		// === TAB TRACKING========================================================================
		final RandomAccessibleInterval< DoubleType > imgOrig = model.getRawData();
		//TODO this should at some point be a given model, not fixed the Hernan thing...
		tabTracking =
				new Tr2dTrackingPanel(
						new Tr2dTrackingModel( model, segModel,
								new HernanSegmentCostFactory( imgOrig ),
								new HernanAppearanceCostFactory( imgOrig ),
								new HernanMappingCostFactory( imgOrig ),
								new HernanDivisionCostFactory( imgOrig ),
								new HernanDisappearanceCostFactory( imgOrig ) ) );

		// --- ASSEMBLE PANEL ---------------------------------------------------------------------

		tabs.add( "Dataset", tabData );
		tabs.add( "Hypotheses collection", tabSegmentation );
		tabs.add( "Tracking", tabTracking );

		this.add( tabs, BorderLayout.CENTER );

		// - - - - - - - - - - - - - - - - - - - - - - - -
		// KEYSTROKE SETUP (usingInput- and ActionMaps)
		// - - - - - - - - - - - - - - - - - - - - - - - -
		this.getInputMap( WHEN_IN_FOCUSED_WINDOW ).put(
				KeyStroke.getKeyStroke( '.' ),
				"tr2d_bindings" );
		this.getInputMap( WHEN_IN_FOCUSED_WINDOW ).put(
				KeyStroke.getKeyStroke( ',' ),
				"tr2d_bindings" );

		this.getActionMap().put( "tr2d_bindings", new AbstractAction() {

			private static final long serialVersionUID = 2L;

			@Override
			public void actionPerformed( final ActionEvent e ) {
				if ( e.getActionCommand().equals( "," ) ) {
				}
				if ( e.getActionCommand().equals( "." ) ) {
				}
			}
		} );
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed( final ActionEvent e ) {
		// TODO Auto-generated method stub
	}

	/**
	 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
	 */
	@Override
	public void stateChanged( final ChangeEvent e ) {
		// TODO Auto-generated method stub
	}
}
