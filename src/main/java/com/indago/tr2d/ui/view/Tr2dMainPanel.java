/**
 *
 */
package com.indago.tr2d.ui.view;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.io.yaml.YamlConfigIO;

import com.indago.log.Log;
import com.indago.log.LoggingPanel;
import com.indago.tr2d.ui.model.Tr2dModel;

import bdv.util.AbstractActions;
import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandlePanel;
import bdv.util.BdvSource;
import bdv.viewer.InputActionBindings;

/**
 * @author jug
 */
public class Tr2dMainPanel extends JPanel implements ActionListener, ChangeListener {

	private static final long serialVersionUID = 2153194588508418291L;

	private final Frame frame;

	private final Tr2dModel model;

	private JTabbedPane tabs;
	private JPanel tabData;
	private JPanel tabSegmentation;
	private JPanel tabFlow;
	private JPanel tabTracking;

	private BdvHandlePanel bdvData;

	private JSplitPane splitPane;

	private LoggingPanel logPanel;

	/**
	 * @param imgPlus
	 */
	public Tr2dMainPanel( final Frame frame, final Tr2dModel model ) {
		super( new BorderLayout( 5, 5 ) );

		setBorder( BorderFactory.createEmptyBorder( 10, 15, 5, 15 ) );
		this.frame = frame;
		this.model = model;

		buildGui();
	}

	private void buildGui() {
		// --- INPUT TRIGGERS ---------------------------------------------------------------------
		model.setDefaultInputTriggerConfig( loadInputTriggerConfig() );

		// === TAB DATA ===========================================================================
		tabs = new JTabbedPane();
		tabData = new JPanel( new BorderLayout() );
		bdvData = new BdvHandlePanel( frame, Bdv.options().is2D().inputTriggerConfig( model.getDefaultInputTriggerConfig() ) );
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
		//TODO this should at some point be a given model, not fixed the Hernan thing...
		tabTracking = new Tr2dTrackingPanel( model.getTrackingModel() );

		// --- ASSEMBLE PANEL ---------------------------------------------------------------------

		tabs.add( "data", tabData );
		tabs.add( "segments", tabSegmentation );
		tabs.add( "flow", tabFlow );
		tabs.add( "tracking", tabTracking );

		logPanel = new LoggingPanel();
		final JScrollPane scroll = new JScrollPane( logPanel );
		logPanel.redirectStderr();
		logPanel.redirectStdout();

		splitPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, tabs, scroll );
		splitPane.setResizeWeight( .5 ); // 1.0 == extra space given to left (top) component alone!
		splitPane.setOneTouchExpandable( true );

		this.add( splitPane, BorderLayout.CENTER );
	}

	/**
	 * @return the loaded <code>InputTriggerConfig</code>, or <code>null</code>
	 *         if none was found.
	 *
	 */
	private InputTriggerConfig loadInputTriggerConfig() {
		try
		{

			Log.info( "Try to fetch yaml from " + ClassLoader.getSystemResource( "tr2d.yaml" ) );
			URL yamlURL = ClassLoader.getSystemResource( "tr2d.yaml" );
			if ( yamlURL == null ) {
				yamlURL = getClass().getClassLoader().getResource( "tr2d.yaml" );
			}
			final BufferedReader in = new BufferedReader( new InputStreamReader( yamlURL.openStream() ) );
			final InputTriggerConfig conf = new InputTriggerConfig( YamlConfigIO.read( in ) );

			final InputActionBindings bindings = new InputActionBindings();
			SwingUtilities.replaceUIActionMap( this, bindings.getConcatenatedActionMap() );
			SwingUtilities.replaceUIInputMap( this, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, bindings.getConcatenatedInputMap() );

			final AbstractActions a = new AbstractActions( bindings, "tabs", conf, new String[] { "tr2d" } );

			a.runnableAction(
					() -> tabs.setSelectedIndex( Math.min( tabs.getSelectedIndex() + 1, tabs.getTabCount() - 1 ) ),
					"next tab",
					"COLON" );
			a.runnableAction(
					() -> tabs.setSelectedIndex( Math.max( tabs.getSelectedIndex() - 1, 0 ) ),
					"previous tab",
					"COMMA" );

			return conf;
		}
		catch ( IllegalArgumentException | IOException e )
		{
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed( final ActionEvent e ) {
	}

	/**
	 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
	 */
	@Override
	public void stateChanged( final ChangeEvent e ) {
	}

	public void collapseLog() {
		splitPane.setDividerLocation( 1.0 );
	}
}
