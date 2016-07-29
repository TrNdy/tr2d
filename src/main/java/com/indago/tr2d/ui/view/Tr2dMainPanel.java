/**
 *
 */
package com.indago.tr2d.ui.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.io.yaml.YamlConfigIO;

import com.indago.app.hernan.Tr2dApplication;
import com.indago.tr2d.ui.model.Tr2dModel;
import com.indago.tr2d.ui.util.MessageConsole;

import bdv.util.AbstractActions;
import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandlePanel;
import bdv.util.BdvSource;
import bdv.viewer.InputActionBindings;
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
	private JPanel tabFlow;
	private JPanel tabTracking;

	private BdvHandlePanel bdvData;

	private MessageConsole log;

	private JSplitPane splitPane;

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
	}

	private void buildGui() {

		// === TAB DATA ===========================================================================
		tabs = new JTabbedPane();
		tabData = new JPanel( new BorderLayout() );
		bdvData = new BdvHandlePanel( frame, Bdv.options().is2D() );
		tabData.add( bdvData.getViewerPanel(), BorderLayout.CENTER );
		final BdvSource source = BdvFunctions.show(
				model.getRawData(),
				"RAW",
				Bdv.options().addTo( bdvData ) );
		source.setDisplayRangeBounds( 0, model.getMaxRawValue() );
		source.setDisplayRange( model.getMinRawValue(), model.getMaxRawValue() );

		// === TAB SEGMENTATION ===================================================================
		tabSegmentation = new Tr2dSegmentationCollectionPanel( model.getSegmentationModel() );

		// === TAB FLOW ===================================================================
		tabFlow = new Tr2dFlowPanel( model.getFlowModel() );

		// === TAB TRACKING========================================================================
		final RandomAccessibleInterval< DoubleType > imgOrig = model.getRawData();
		//TODO this should at some point be a given model, not fixed the Hernan thing...
		tabTracking = new Tr2dTrackingPanel( model.getTrackingModel() );

		// --- ASSEMBLE PANEL ---------------------------------------------------------------------

		tabs.add( "data", tabData );
		tabs.add( "segments", tabSegmentation );
		tabs.add( "flow", tabFlow );
		tabs.add( "tracking", tabTracking );

		final JPanel logPanel = new JPanel( new BorderLayout() );
		final JTextPane logText = new JTextPane();
		final JScrollPane scroll = new JScrollPane( logText );
		scroll.setPreferredSize( new Dimension( 400, 3000 ) );
		logPanel.add( scroll, BorderLayout.CENTER );
		log = new MessageConsole( logText, true );
		log.redirectOut();
		log.redirectErr( Color.RED, null );
		log.setMessageLines( 10000 );

		splitPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, tabs, logPanel );
		splitPane.setResizeWeight( .8 ); // 1.0 == extra space given to left (top) component alone!
		splitPane.setOneTouchExpandable( true );

		this.add( splitPane, BorderLayout.CENTER );


		try
		{
			String path = "";
			try {
				path = Tr2dApplication.class.getClassLoader().getResource( "tr2d.yaml" ).getPath();
			} catch ( final Exception e ) {
// >>>>>> find this and code worked on various OSes... great, please delete the commented parts...
//				try {
//					System.out.println( "attempt 2..." );
//					path = Tr2dApplication.class.getClassLoader().getResource( "resources/tr2d.yaml" ).getPath();
//				} catch ( final Exception e2 ) {
					System.out.println( ">>> Error: tr2d.yaml not found in project/JAR resources..." );
//				}
			}

			final InputTriggerConfig conf = new InputTriggerConfig( YamlConfigIO.read( path ) );

//		Code to create an minimal tr2d.yaml...
//		--------------------------------------
//			final InputTriggerConfig conf = new InputTriggerConfig();
//			final KeyStrokeAdder adder = conf.keyStrokeAdder(
//					this.getInputMap( WHEN_IN_FOCUSED_WINDOW ),
//					"tr2d" );
//			adder.put( "tr2d_bindings", "COMMA", "COLON" );
//
//			// dump config...
//			final InputTriggerDescriptionsBuilder builder = new InputTriggerDescriptionsBuilder();
//			builder.addMap(
//					this.getInputMap( WHEN_IN_FOCUSED_WINDOW ),
//					"tr2d" );
//			YamlConfigIO.write( builder.getDescriptions(), "/Users/jug/Desktop/tr2d.yaml" );

			final InputActionBindings bindings = new InputActionBindings();
			SwingUtilities.replaceUIActionMap( this, bindings.getConcatenatedActionMap() );
			SwingUtilities.replaceUIInputMap( this, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, bindings.getConcatenatedInputMap() );

			final AbstractActions a = new AbstractActions( bindings, "tabs", conf, new String[] { "tr2d" } );

			a.runnableAction(
					() -> tabs.setSelectedIndex( Math.min( tabs.getSelectedIndex() + 1, tabs.getTabCount() - 1 ) ),
					"next tab",
					"PERIOD" );
			a.runnableAction(
					() -> tabs.setSelectedIndex( Math.max( tabs.getSelectedIndex() - 1, 0 ) ),
					"previous tab",
					"COMMA" );
		}
		catch ( IllegalArgumentException | IOException e )
		{
			e.printStackTrace();
		}
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

	public void collapseLog() {
		splitPane.setDividerLocation( 1.0 );
	}
}
