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
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.indago.tr2d.Tr2dContext;
import org.scijava.log.Logger;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.io.yaml.YamlConfigIO;
import org.scijava.ui.behaviour.util.InputActionBindings;

import com.indago.IndagoLog;
import com.indago.tr2d.Tr2dLog;
import com.indago.tr2d.ui.model.Tr2dModel;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandlePanel;
import bdv.util.BdvSource;
import com.indago.log.LoggingPanel;

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
	private JPanel tabSegmentationEdit;
	private JPanel tabFlow;
	private Tr2dTrackingPanel tabTracking;
	private Tr2dExportPanel tabExport;

	private BdvHandlePanel bdvData;

	private JSplitPane splitPane;

	private final LoggingPanel logPanel;

	public Tr2dMainPanel( final Frame frame, final Tr2dModel model, final Logger logger) {
		super( new BorderLayout( 5, 5 ) );
		logPanel = new LoggingPanel(Tr2dContext.ops.context());
		model.setRefToMainPanel( this );

		setBorder( BorderFactory.createEmptyBorder( 10, 15, 5, 15 ) );
		this.frame = frame;
		this.model = model;

		buildGui(logger);
	}

	private void buildGui(Logger logger) {
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

		// === TAB SEGMENTATION EDIT ==============================================================
		tabSegmentationEdit = new Tr2dSegmentationEditorPanel( model.getSegmentationEditorModel() );

		// === TAB FLOW ===========================================================================
		tabFlow = new Tr2dFlowPanel( model.getFlowModel() );

		// === TAB TRACKING========================================================================
		//TODO this should at some point be a given model, not fixed the Hernan thing...
		tabTracking = new Tr2dTrackingPanel( model.getTrackingModel() );

		// === TAB EXPORT========================================================================
		tabExport = new Tr2dExportPanel( model );

		// --- ASSEMBLE PANEL ---------------------------------------------------------------------

		tabs.add( "data", tabData );
		tabs.add( "segments", tabSegmentation );
		tabs.add( "segmentation edit", tabSegmentationEdit );
		tabs.add( "flow", tabFlow );
		tabs.add( "tracking", tabTracking );
		tabs.add( "export", tabExport );

		// --- LOGGING PANEL (from IndagoLoggingWrapper dependency) -------------------------------
		IndagoLog.log = setupLogger(logger, "indago");
		Tr2dLog.log = setupLogger(logger, "tr2dy");
		Tr2dLog.gurobilog = setupLogger(logger, "gurobi");

		splitPane = new JSplitPane( JSplitPane.VERTICAL_SPLIT, tabs, logPanel );
		splitPane.setResizeWeight( .5 ); // 1.0 == extra space given to left (top) component alone!
		splitPane.setOneTouchExpandable( true );

		this.add( splitPane, BorderLayout.CENTER );
	}

	private Logger setupLogger(Logger logger, String name) {
		Logger log = logger.subLogger(name);
		log.addLogListener(logPanel);
		return log;
	}

	/**
	 * @return the loaded <code>InputTriggerConfig</code>, or <code>null</code>
	 *         if none was found.
	 *
	 */
	private InputTriggerConfig loadInputTriggerConfig() {
		try
		{

			Tr2dLog.log.info( "Try to fetch yaml from " + ClassLoader.getSystemResource( "tr2d.yaml" ) );
			URL yamlURL = ClassLoader.getSystemResource( "tr2d.yaml" );
			if ( yamlURL == null ) {
				Tr2dLog.log.info( "Try to fetch yaml from " + getClass().getClassLoader().getResource( "tr2d.yaml" ) );
				yamlURL = getClass().getClassLoader().getResource( "tr2d.yaml" );
			}
			if ( yamlURL != null ) {
				final BufferedReader in = new BufferedReader( new InputStreamReader( yamlURL.openStream() ) );
				final InputTriggerConfig conf = new InputTriggerConfig( YamlConfigIO.read( in ) );

				final InputActionBindings bindings = new InputActionBindings();
				SwingUtilities.replaceUIActionMap( this, bindings.getConcatenatedActionMap() );
				SwingUtilities.replaceUIInputMap( this, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, bindings.getConcatenatedInputMap() );

//				final AbstractActions a = new AbstractActions( bindings, "tabs", conf, new String[] { "tr2d" } );
//
//				a.runnableAction(
//						() -> tabs.setSelectedIndex( Math.min( tabs.getSelectedIndex() + 1, tabs.getTabCount() - 1 ) ),
//						"next tab",
//						"COLON" );
//				a.runnableAction(
//						() -> tabs.setSelectedIndex( Math.max( tabs.getSelectedIndex() - 1, 0 ) ),
//						"previous tab",
//						"COMMA" );

				return conf;
			} else {
				Tr2dLog.log.info( "Falling back to default BDV action settings." );
				final InputTriggerConfig conf = new InputTriggerConfig();
				final InputActionBindings bindings = new InputActionBindings();
				SwingUtilities.replaceUIActionMap( this, bindings.getConcatenatedActionMap() );
				SwingUtilities.replaceUIInputMap( this, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, bindings.getConcatenatedInputMap() );
				return conf;
			}
		} catch ( IllegalArgumentException | IOException e ) {
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

	/**
	 * @return the logPanel
	 */
	public LoggingPanel getLogPanel() {
		return logPanel;
	}

	public void selectTab( final JPanel tab ) {
		for ( int i = 0; i < tabs.getTabCount(); ++i ) {
			if ( tabs.getComponentAt( i ) == tab ) {
				tabs.setSelectedIndex( i );
				break;
			}
		}
	}

	/**
	 * @return the tabExport
	 */
	public Tr2dExportPanel getTabExport() {
		return tabExport;
	}

	/**
	 * @return the tabTracking
	 */
	public Tr2dTrackingPanel getTabTracking() {
		return tabTracking;
	}
}
