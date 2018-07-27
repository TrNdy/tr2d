/**
 *
 */
package com.indago.tr2d.ui.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.mastodon.adapter.FocusAdapter;
import org.mastodon.adapter.HighlightAdapter;
import org.mastodon.adapter.NavigationHandlerAdapter;
import org.mastodon.adapter.RefBimap;
import org.mastodon.adapter.SelectionAdapter;
import org.mastodon.graph.GraphIdBimap;
import org.mastodon.graph.algorithm.ShortestPath;
import org.mastodon.graph.algorithm.traversal.BreadthFirstIterator;
import org.mastodon.graph.algorithm.traversal.GraphSearch.SearchDirection;
import org.mastodon.revised.trackscheme.TrackSchemeEdge;
import org.mastodon.revised.trackscheme.TrackSchemeEdgeBimap;
import org.mastodon.revised.trackscheme.TrackSchemeGraph;
import org.mastodon.revised.trackscheme.TrackSchemeVertex;
import org.mastodon.revised.trackscheme.TrackSchemeVertexBimap;
import org.mastodon.revised.trackscheme.display.TrackSchemeNavigator.NavigatorEtiquette;
import org.mastodon.revised.trackscheme.display.TrackSchemeOptions;
import org.mastodon.revised.trackscheme.display.TrackSchemePanel;
import org.mastodon.revised.trackscheme.wrap.DefaultModelGraphProperties;
import org.mastodon.revised.trackscheme.wrap.ModelGraphProperties;
import org.mastodon.revised.ui.selection.FocusListener;
import org.mastodon.revised.ui.selection.FocusModel;
import org.mastodon.revised.ui.selection.FocusModelImp;
import org.mastodon.revised.ui.selection.HighlightListener;
import org.mastodon.revised.ui.selection.HighlightModel;
import org.mastodon.revised.ui.selection.HighlightModelImp;
import org.mastodon.revised.ui.selection.NavigationHandler;
import org.mastodon.revised.ui.selection.NavigationHandlerImp;
import org.mastodon.revised.ui.selection.Selection;
import org.mastodon.revised.ui.selection.SelectionImp;
import org.mastodon.revised.ui.selection.SelectionListener;
import org.mastodon.revised.ui.selection.TimepointModel;
import org.mastodon.revised.ui.selection.TimepointModelImp;
import org.scijava.ui.behaviour.MouseAndKeyHandler;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.InputActionBindings;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

import com.indago.data.segmentation.LabelData;
import com.indago.data.segmentation.LabelingFragment;
import com.indago.data.segmentation.LabelingPlus;
import com.indago.data.segmentation.LabelingSegment;
import com.indago.demos.selectsegment.ColorTable;
import com.indago.demos.selectsegment.ColorTableConverter;
import com.indago.demos.selectsegment.InverseBreadthFirstIterator;
import com.indago.demos.selectsegment.SegmentBrowser;
import com.indago.demos.selectsegment.SegmentGraph;
import com.indago.demos.selectsegment.SegmentVertex;
import com.indago.demos.selectsegment.SubsetEdge;
import com.indago.fg.Assignment;
import com.indago.pg.IndicatorNode;
import com.indago.pg.segments.ConflictSet;
import com.indago.pg.segments.SegmentNode;
import com.indago.tr2d.Tr2dLog;
import com.indago.tr2d.pg.Tr2dSegmentationProblem;
import com.indago.tr2d.pg.levedit.EditState;
import com.indago.tr2d.ui.listener.ModelInfeasibleListener;
import com.indago.tr2d.ui.listener.SolutionChangedListener;
import com.indago.tr2d.ui.model.Tr2dTrackingModel;
import com.indago.tr2d.ui.view.bdv.overlays.Tr2dInAssignmentsOverlayOnSelection;
import com.indago.tr2d.ui.view.bdv.overlays.Tr2dOutAssignmentsOverlayOnSelection;
import com.indago.tr2d.ui.view.bdv.overlays.Tr2dTrackingOverlay;
import com.indago.ui.bdv.BdvWithOverlaysOwner;

import bdv.BehaviourTransformEventHandler;
import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandlePanel;
import bdv.util.BdvOverlay;
import bdv.util.BdvSource;
import bdv.util.BdvVirtualChannelSource;
import bdv.util.PlaceHolderOverlayInfo;
import bdv.util.VirtualChannels.VirtualChannel;
import bdv.viewer.ViewerPanel;
import gnu.trove.impl.Constants;
import gnu.trove.set.hash.TLongHashSet;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.ui.TransformEventHandler;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;
import net.imglib2.view.Views;
import net.miginfocom.swing.MigLayout;
/**
 * @author jug
 */
public class Tr2dFrameEditPanel extends JPanel implements ActionListener, BdvWithOverlaysOwner, SolutionChangedListener, ModelInfeasibleListener {

	private static final long serialVersionUID = -9051482691122695949L;

	private final Tr2dTrackingModel model;

	private int currentFrame;

	// === UI Stuff ====================================================================
	private JPanel segHypothesesTreePanel;
	private JButton buttonFirst;
	private JButton buttonPrev;
	private JButton buttonNext;
	private JButton buttonLast;
	private JTextField txtCurFrame;

	// === BDV related stuff ===========================================================
	private BdvHandlePanel bdvHandlePanel;
	private final List< RandomAccessibleInterval< RealType > > imgs;
	private final List< BdvSource > bdvSources = new ArrayList<>();
	private final List< BdvOverlay > overlays = new ArrayList<>();
	private final List< BdvSource > bdvOverlaySources = new ArrayList<>();

	// === Hypotheses browser related stuff ============================================
	private final TrackSchemeOptions trackSchemeOptions;
	private final InputTriggerConfig inputConf;

	// === Leveraged editing related stuff =============================================
	private JButton bForceSelected;
	private JButton bForceAppearance;
	private JButton bForceDisappearance;
	private JButton bForceMoveIn;
	private JButton bForceDivisionIn;
	private JButton bForceMoveOut;
	private JButton bForceDivisionOut;
	private JButton bAvoidSelected;
	private JButton bForceSelectionExactly;

	private JButton bUndo;
	private JButton bRedo;
	private final Stack< Pair< Integer, EditState > > undoStack = new Stack<>();
	private final Stack< Pair< Integer, EditState > > redoStack = new Stack<>();

	// === Hypotheses browser related (debugging) =====================================
	private JButton bSelectionFromSolution;
	private JButton bShowTrackingOverlay;
	private JButton bOverlayMovementInAssignmentsForSelection;
	private JButton bOverlayDivisionInAssignmentsForSelection;
	private JButton bOverlayMovementOutAssignmentsForSelection;
	private JButton bOverlayDivisionOutAssignmentsForSelection;
	private JButton bOverlayRemoveAll;

	// === Other stuff =================================================================
	private final InputActionBindings keybindings;
	private final TriggerBehaviourBindings triggerbindings;
	private final MouseAndKeyHandler mouseAndKeyHandler;

	private SegmentGraph segmentGraph = new SegmentGraph();
	private Map< LabelData, SegmentVertex > mapLabelData2SegmentVertex;
	private Map< LabelingSegment, SegmentVertex > mapLabelingSegment2SegmentVertex;

	private Selection< SegmentVertex, SubsetEdge > selectionModel;
	private HighlightModel< SegmentVertex, SubsetEdge > highlightModel;
	private FocusModel< SegmentVertex, SubsetEdge > focusModel;
	private TimepointModel timepointModel;
	private NavigationHandler< SegmentVertex, SubsetEdge > navigationHandler;

	private TrackSchemePanel trackschemePanel;

	private SegmentBrowser segmentBrowser;

	private JSplitPane vertSplitPane;


	// === INNER CLASSES ETC. ==========================================================

	static class CheckedPairs {

		private static final long NO_ENTRY_VALUE = -1L;

		private final GraphIdBimap< SegmentVertex, ? > idmap;

		private final TLongHashSet set;

		public CheckedPairs( final GraphIdBimap< SegmentVertex, ? > idmap ) {
			this.idmap = idmap;
			this.set = new TLongHashSet( Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, NO_ENTRY_VALUE );
		}

		/**
		 * Returns {@code true} if this is the first time that the pair (a, b)
		 * is checked for
		 *
		 * @param a
		 * @param b
		 * @return
		 */
		boolean isNewPair( final SegmentVertex a, final SegmentVertex b ) {
			final int ai = idmap.getVertexId( a );
			final int bi = idmap.getVertexId( b );
			final long key = ( ( long ) ai << 32 ) | ( bi & 0xFFFFFFFFL );
			if ( set.contains( key ) )
				return false;
			else {
				set.add( key );
				return true;
			}
		}
	}

	public Tr2dFrameEditPanel( final Tr2dTrackingModel model ) {
		super( new BorderLayout() );
		this.model = model;

		this.model.addSolutionChangedListener( this );
		this.model.addModelInfeasibleListener( this );

		trackSchemeOptions = TrackSchemeOptions.options();
		inputConf = getKeyConfig( trackSchemeOptions );

		imgs = new ArrayList<>();
		keybindings = new InputActionBindings();
		triggerbindings = new TriggerBehaviourBindings();
		mouseAndKeyHandler = new MouseAndKeyHandler();

		buildGui();

		this.currentFrame = 0;
		displayFrameData();

//		model.bdvAdd( new Tr2dTrackingOverlay( model ), "overlay_tracking" );
	}

	private void buildGui() {
		final JPanel panelRightSide = new JPanel( new BorderLayout() );

		// LEFT SIDE
		final JPanel controls = new JPanel( new MigLayout() );

		MigLayout layout = new MigLayout();
		final JPanel panelLeveragedEditing = new JPanel( layout );
		panelLeveragedEditing.setBorder( BorderFactory.createTitledBorder( "leveraged editing" ) );
		bForceSelected = new JButton( "force selected" );
		bForceSelected.addActionListener( this );
		bForceAppearance = new JButton( "force appearance" );
		bForceAppearance.addActionListener( this );
		bForceDisappearance = new JButton( "force disappearance" );
		bForceDisappearance.addActionListener( this );
		bForceMoveIn = new JButton( "move to" );
		bForceMoveIn.addActionListener( this );
		bForceDivisionIn = new JButton( "divide to" );
		bForceDivisionIn.addActionListener( this );
		bForceMoveOut = new JButton( "from" );
		bForceMoveOut.addActionListener( this );
		bForceDivisionOut = new JButton( "from" );
		bForceDivisionOut.addActionListener( this );
		bAvoidSelected = new JButton( "avoid selected" );
		bAvoidSelected.addActionListener( this );
		bForceSelectionExactly = new JButton( "all as selected" );
		bForceSelectionExactly.addActionListener( this );
		bUndo = new JButton( "undo" );
		bUndo.setEnabled( false );
		bUndo.addActionListener( this );
		bRedo = new JButton( "redo" );
		bRedo.setEnabled( false );
		bRedo.addActionListener( this );
		panelLeveragedEditing.add( bForceSelected, "span,growx,wrap" );
		panelLeveragedEditing.add( bForceAppearance, "span,growx,wrap" );
		panelLeveragedEditing.add( bForceDisappearance, "span,growx,wrap" );
		panelLeveragedEditing.add( bForceMoveIn, "growx" );
		panelLeveragedEditing.add( bForceMoveOut, "growx,wrap" );
		panelLeveragedEditing.add( bForceDivisionIn, "growx" );
		panelLeveragedEditing.add( bForceDivisionOut, "growx,wrap" );
		panelLeveragedEditing.add( bAvoidSelected, "span,growx,wrap" );
		panelLeveragedEditing.add( bForceSelectionExactly, "span,growx,wrap" );
		panelLeveragedEditing.add( bUndo, "growx" );
		panelLeveragedEditing.add( bRedo, "growx,wrap" );

		layout = new MigLayout();
		final JPanel panelSelection = new JPanel( layout );
		panelSelection.setBorder( BorderFactory.createTitledBorder( "selection" ) );
		bSelectionFromSolution = new JButton( "from solution" );
		bSelectionFromSolution.addActionListener( this );
		panelSelection.add( bSelectionFromSolution, "growx,wrap" );

		layout = new MigLayout();
		final JPanel panelDebug = new JPanel( layout );
		panelDebug.setBorder( BorderFactory.createTitledBorder( "show for selection" ) );
		bShowTrackingOverlay = new JButton( "show tracking overlay" );
		bShowTrackingOverlay.addActionListener( this );
		bOverlayMovementInAssignmentsForSelection = new JButton( "moves in" );
		bOverlayMovementInAssignmentsForSelection.addActionListener( this );
		bOverlayMovementOutAssignmentsForSelection = new JButton( "out" );
		bOverlayMovementOutAssignmentsForSelection.addActionListener( this );
		bOverlayDivisionInAssignmentsForSelection = new JButton( "divisions in" );
		bOverlayDivisionInAssignmentsForSelection.addActionListener( this );
		bOverlayDivisionOutAssignmentsForSelection = new JButton( "out" );
		bOverlayDivisionOutAssignmentsForSelection.addActionListener( this );
		bOverlayRemoveAll = new JButton( "unshow all" );
		bOverlayRemoveAll.addActionListener( this );
		panelDebug.add( bShowTrackingOverlay, "span,growx,wrap" );
		panelDebug.add( bOverlayMovementInAssignmentsForSelection, "growx" );
		panelDebug.add( bOverlayMovementOutAssignmentsForSelection, "growx,wrap" );
		panelDebug.add( bOverlayDivisionInAssignmentsForSelection, "growx" );
		panelDebug.add( bOverlayDivisionOutAssignmentsForSelection, "growx,wrap" );
		panelDebug.add( bOverlayRemoveAll, "span,growx,wrap" );

		controls.add( panelLeveragedEditing, "span, grow, wrap" );
		controls.add( panelSelection, "span, grow, wrap" );
		controls.add( panelDebug, "span, grow, wrap" );

		// RIGHT SIDE
		buttonFirst = new JButton( "<<" );
		buttonPrev = new JButton( "<" );
		buttonNext = new JButton( ">" );
		buttonLast = new JButton( ">>" );
		buttonFirst.addActionListener( this );
		buttonPrev.addActionListener( this );
		buttonNext.addActionListener( this );
		buttonLast.addActionListener( this );

		txtCurFrame = new JTextField( 3 );
		txtCurFrame.setHorizontalAlignment( JTextField.CENTER );
		txtCurFrame.setText( "" + this.currentFrame );
		txtCurFrame.addActionListener( this );

		final JLabel lblNumFrames = new JLabel( "of " + ( model.getTr2dModel().getRawData().dimension( 2 ) - 1 ) );

		final JPanel panelFrameSwitcher = new JPanel();
		panelFrameSwitcher.add( buttonFirst );
		panelFrameSwitcher.add( buttonPrev );
		panelFrameSwitcher.add( txtCurFrame );
		panelFrameSwitcher.add( lblNumFrames );
		panelFrameSwitcher.add( buttonNext );
		panelFrameSwitcher.add( buttonLast );

		segHypothesesTreePanel = new JPanel( new BorderLayout() );
		segHypothesesTreePanel.setPreferredSize( new Dimension( 1000, 150 ) ); // SET INITIAL SIZE OF HYPOTHESES PANEL HERE
		SwingUtilities.replaceUIActionMap( segHypothesesTreePanel, keybindings.getConcatenatedActionMap() );
		SwingUtilities
				.replaceUIInputMap( segHypothesesTreePanel, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, keybindings.getConcatenatedInputMap() );

		bdvHandlePanel = new BdvHandlePanel( ( Frame ) this.getTopLevelAncestor(), Bdv
				.options()
				.is2D()
				.inputTriggerConfig( model.getTr2dModel().getDefaultInputTriggerConfig() ) );
		vertSplitPane = new JSplitPane( JSplitPane.VERTICAL_SPLIT, bdvHandlePanel.getViewerPanel(), segHypothesesTreePanel );
		vertSplitPane.setResizeWeight( 1 );
		vertSplitPane.setOneTouchExpandable( true );

		// ASSEMBLY
		panelRightSide.add( panelFrameSwitcher, BorderLayout.NORTH );
		panelRightSide.add( vertSplitPane, BorderLayout.CENTER );

		final JSplitPane horSplitPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, controls, panelRightSide );
		this.add( horSplitPane, BorderLayout.CENTER );
	}

	private void displayFrameData() {
		final LabelingPlus labelingPlus = model.getLabelingFrames().getLabelingPlusForFrame( this.currentFrame );

		if ( labelingPlus != null ) {
			segmentGraph = new SegmentGraph();
			final ShortestPath< SegmentVertex, SubsetEdge > sp = new ShortestPath<>( segmentGraph, SearchDirection.DIRECTED );

			// create vertices for all segments
			mapLabelData2SegmentVertex = new HashMap<>();
			mapLabelingSegment2SegmentVertex = new HashMap<>();
			for ( final LabelData labelData : labelingPlus.getLabeling().getMapping().getLabels() ) {
				final SegmentVertex newVertex = segmentGraph.addVertex().init( labelData );
				mapLabelData2SegmentVertex.put( labelData, newVertex );
				mapLabelingSegment2SegmentVertex.put( labelData.getSegment(), newVertex );
			}

			final CheckedPairs pairs = new CheckedPairs( segmentGraph.getGraphIdBimap() );
			// Build partial order graph
			for ( final LabelingFragment fragment : labelingPlus.getFragments() ) {
				final ArrayList< LabelData > conflictingSegments = fragment.getSegments();

				// connect regarding subset relation (while removing transitive edges)
				for ( final LabelData subset : conflictingSegments ) {

					final SegmentVertex subv = mapLabelData2SegmentVertex.get( subset );
					for ( final LabelData superset : conflictingSegments ) {
						if ( subset.equals( superset ) )
							continue;
						final SegmentVertex superv = mapLabelData2SegmentVertex.get( superset );

						// Was this (ordered) pair of vertices already checked?
						// If yes, abort.
						if ( !pairs.isNewPair( subv, superv ) )
							continue;

						// Is "subset" really a subset of "superset"?
						// If not, abort.
						if ( !isSubset( subset, superset ) )
							continue;

						// Is there already a path superv --> subv
						// If yes, abort, because the new edge is not necessary.
						if ( sp.findPath( superv, subv ) != null )
							continue;

						// At this point, we know that we will add a new edge superv --> subv.
						// Before we do that, we remove edges that are made obsolete (become transitive) by the new edge.

						// for all edges leaving any vertex in ancestors:
						// delete if target is within descendants
						final Set< SegmentVertex > descendants = new HashSet<>();
						final Set< SegmentVertex > ancestors = new HashSet<>();
						new BreadthFirstIterator<>( subv, segmentGraph ).forEachRemaining( v -> descendants.add( v ) );
						new InverseBreadthFirstIterator<>( superv, segmentGraph ).forEachRemaining( v -> ancestors.add( v ) );
						final ArrayList< SubsetEdge > remove = new ArrayList<>();
						for ( final SegmentVertex a : ancestors )
							for ( final SubsetEdge edge : a.outgoingEdges() )
								if ( descendants.contains( edge.getTarget() ) )
									remove.add( edge );
						remove.forEach( edge -> segmentGraph.remove( edge ) );

						// Add the edge, finally.
						segmentGraph.addEdge( superv, subv );
					}
				}
			}

			// Find all roots
			final Set< SegmentVertex > roots = new HashSet<>();
			for ( final SegmentVertex v : segmentGraph.vertices() ) {
				if ( v.incomingEdges().isEmpty() ) {
					roots.add( v );
				}
			}
			// For each root perform BFS and push level number (timepoint) through graph.
			for ( final SegmentVertex root : roots ) {
				root.setTimepoint( 0 );
				final BreadthFirstIterator< SegmentVertex, SubsetEdge > bfi =
						new BreadthFirstIterator<>( root, segmentGraph );
				while ( bfi.hasNext() ) {
					final SegmentVertex v = bfi.next();
					v.setTimepoint( getMaxParentTimepoint( v ) + 1 );
				}
			}

			display( segmentGraph, labelingPlus );
		}
	}

	/**
	 * Updates displays with new labeling and graph...
	 *
	 * @param modelGraph
	 * @param labelingPlus
	 */
	private void display( final SegmentGraph modelGraph, final LabelingPlus labelingPlus ) {

		final GraphIdBimap< SegmentVertex, SubsetEdge > idmap = modelGraph.getGraphIdBimap();

		selectionModel = new SelectionImp<>( modelGraph, idmap );
		final Selection< SegmentVertex, SubsetEdge > segmentsUnderMouse = new SelectionImp<>( modelGraph, idmap );
		highlightModel = new HighlightModelImp<>( idmap );
		focusModel = new FocusModelImp<>( idmap );
		timepointModel = new TimepointModelImp();
		navigationHandler = new NavigationHandlerImp<>();

		// === TrackScheme ===

		if ( trackschemePanel != null ) {
			segHypothesesTreePanel.remove( trackschemePanel );
		}

		final ModelGraphProperties< SegmentVertex, SubsetEdge > modelGraphProperties = new DefaultModelGraphProperties<>();
		final TrackSchemeGraph< SegmentVertex, SubsetEdge > trackSchemeGraph = new TrackSchemeGraph<>( modelGraph, idmap, modelGraphProperties );
		final RefBimap< SegmentVertex, TrackSchemeVertex > vertexMap = new TrackSchemeVertexBimap<>( idmap, trackSchemeGraph );
		final RefBimap< SubsetEdge, TrackSchemeEdge > edgeMap = new TrackSchemeEdgeBimap<>( idmap, trackSchemeGraph );
		final TrackSchemePanel trackschemePanel = new TrackSchemePanel(
				trackSchemeGraph,
				new HighlightAdapter<>( highlightModel, vertexMap, edgeMap ),
				new FocusAdapter<>( focusModel, vertexMap, edgeMap ),
				timepointModel,
				new SelectionAdapter<>( selectionModel, vertexMap, edgeMap ),
				new NavigationHandlerAdapter<>( navigationHandler, vertexMap, edgeMap ),
				trackSchemeOptions );
		segHypothesesTreePanel.add( trackschemePanel, BorderLayout.CENTER );

		mouseAndKeyHandler.setInputMap( triggerbindings.getConcatenatedInputTriggerMap() );
		mouseAndKeyHandler.setBehaviourMap( triggerbindings.getConcatenatedBehaviourMap() );
		trackschemePanel.getDisplay().addHandler( mouseAndKeyHandler );

		final TransformEventHandler< ? > tfHandler = trackschemePanel.getDisplay().getTransformEventHandler();
		if ( tfHandler instanceof BehaviourTransformEventHandler )
			( ( BehaviourTransformEventHandler< ? > ) tfHandler ).install( triggerbindings );

		trackschemePanel.getNavigator().installActionBindings( keybindings, inputConf, NavigatorEtiquette.FINDER_LIKE );
		trackschemePanel.getNavigator().installBehaviourBindings( triggerbindings, inputConf );

		int maxTimepoint = 0;
		for ( final SegmentVertex v : modelGraph.vertices() )
			maxTimepoint = Math.max( v.getTimepoint(), maxTimepoint );
		trackschemePanel.setTimepointRange( 0, maxTimepoint );
		trackschemePanel.graphChanged();

		// === BDV ===

		if ( segmentBrowser != null ) {
			segmentBrowser.unregister();
		}
		this.bdvRemoveAll();
		this.bdvRemoveAllOverlays();

		final RandomAccessibleInterval< DoubleType > rawData = model.getTr2dModel().getRawData();
		final int t = Integer.parseInt( this.txtCurFrame.getText() );
		this.bdvAdd( Views.hyperSlice( rawData, 2, t ), "RAW" );

		RandomAccessibleInterval< UnsignedShortType > segment_rai_overlay = Converters.convert(
				labelingPlus.getLabeling().getIndexImg(),
				new SelectedSegmentsConverter( labelingPlus, selectionModel ),
				new UnsignedShortType() );
		this.bdvAdd( segment_rai_overlay, "selected segments", 0, 2, new ARGBType( 0x00FF00 ), true );

		segment_rai_overlay = Converters.convert(
				labelingPlus.getLabeling().getIndexImg(),
				new HighlightedSegmentsConverter( labelingPlus, highlightModel ),
				new UnsignedShortType() );
		this.bdvAdd( segment_rai_overlay, "highlighted segments", 0, 1, new ARGBType( 0xFF00FF ), true );

		segment_rai_overlay = Converters.convert(
				labelingPlus.getLabeling().getIndexImg(),
				new FocusedSegmentsConverter( labelingPlus, focusModel ),
				new UnsignedShortType() );
		this.bdvAdd( segment_rai_overlay, "focused segments", 0, 1, new ARGBType( 0x0000FF ), true );

		highlightModel.listeners().add( () -> bdvHandlePanel.getBdvHandle().getViewerPanel().requestRepaint() );
		selectionModel.listeners().add( () -> bdvHandlePanel.getBdvHandle().getViewerPanel().requestRepaint() );
		focusModel.listeners().add( () -> bdvHandlePanel.getBdvHandle().getViewerPanel().requestRepaint() );

		// --- segment selection in BDV start ---

		final ColorTableConverter conv = new ColorTableConverter( labelingPlus );

		final SelectedSegmentsColorTable selectColorTable = new SelectedSegmentsColorTable( labelingPlus, conv, selectionModel );
		final HighlightedSegmentsColorTable highlightColorTable = new HighlightedSegmentsColorTable( labelingPlus, conv, highlightModel );
		final FocusedSegmentsColorTable focusColorTable = new FocusedSegmentsColorTable( labelingPlus, conv, focusModel );
		final SelectedSegmentsColorTable segmentsUnderMouseColorTable = new SelectedSegmentsColorTable( labelingPlus, conv, segmentsUnderMouse );

		conv.addColorTable( selectColorTable );
		conv.addColorTable( highlightColorTable );
		conv.addColorTable( focusColorTable );
		conv.addColorTable( segmentsUnderMouseColorTable );

		final ArrayList< SegmentsColorTable > virtualChannels = new ArrayList<>();
		virtualChannels.add( selectColorTable );
		virtualChannels.add( highlightColorTable );
		virtualChannels.add( focusColorTable );
		virtualChannels.add( segmentsUnderMouseColorTable );

		final List< BdvVirtualChannelSource > vchanSources = BdvFunctions.show(
				Converters.convert(
						labelingPlus.getLabeling().getIndexImg(),
						conv,
						new ARGBType() ),
				virtualChannels,
				"graph highlights (sel;high;focus;mouse)",
				Bdv.options().inputTriggerConfig( inputConf ).is2D().addTo( bdvGetHandlePanel() ) ); // .screenScales( new double[] { 1 } )
		final Bdv bdv = vchanSources.get( 0 );

		for ( int i = 0; i < virtualChannels.size(); ++i ) {
			virtualChannels.get( i ).setPlaceHolderOverlayInfo( vchanSources.get( i ).getPlaceHolderOverlayInfo() );
			virtualChannels.get( i ).setViewerPanel( bdv.getBdvHandle().getViewerPanel() );
			bdvSources.add( vchanSources.get( i ) ); // so they can be removed by bdvRemoveAll()
		}

		final BdvVirtualChannelSource selectionSource = vchanSources.get( 0 );
		final BdvVirtualChannelSource highlightSource = vchanSources.get( 1 );
		final BdvVirtualChannelSource focusSource = vchanSources.get( 2 );
		final BdvVirtualChannelSource segmentsUnderMouseSource = vchanSources.get( 3 );

		selectionSource.setDisplayRange( 0, 10 );
		selectionSource.setColor( new ARGBType( 0x00FF00 ) );
//		selectionSource.setActive( false );

		highlightSource.setDisplayRange( 0, 1 );
		highlightSource.setColor( new ARGBType( 0xFF00FF ) );
//		highlightSource.setActive( false );

		focusSource.setDisplayRange( 0, 1 );
		focusSource.setColor( new ARGBType( 0x0000FF ) );

		segmentsUnderMouseSource.setDisplayRange( 0, 10 );
		segmentsUnderMouseSource.setColor( new ARGBType( 0xFFFF00 ) );

		// add "browse segments" behaviour to bdv
		segmentBrowser = new SegmentBrowser( bdv, labelingPlus, modelGraph, segmentsUnderMouse, highlightModel, selectionModel, inputConf );

		// --- segment selection in BDV end ---
	}

	/**
	 * Returns {@code true} iff {@code a} is a subset of {@code b}.
	 *
	 * @param a
	 *            LabelData instance
	 * @param b
	 *            LabelData instance
	 * @return {@code true} iff {@code a} is a subset of {@code b}.
	 */
	public static boolean isSubset( final LabelData a, final LabelData b ) {
		final ArrayList< Integer > afs = a.getFragmentIndices();
		final ArrayList< Integer > bfs = b.getFragmentIndices();

		int bi = 0;
		A: for ( final int af : afs ) {
			for ( ; bi < bfs.size(); ++bi ) {
				final int bf = bfs.get( bi );
				if ( bf > af ) {
					return false;
				} else if ( bf == af ) {
					continue A;
				}
			}
			return false;
		}
		return true;
	}

	/**
	 * Iterates over all parents of <code>v</code> and returns the maximum
	 * timepoint found.
	 *
	 * @param v
	 *            SegmentVertex instance
	 * @return -1 for roots, otherwise the max timepoint of all vertices that
	 *         connect to v.
	 */
	private static int getMaxParentTimepoint( final SegmentVertex v ) {
		int ret = -1;
		for ( final SubsetEdge incomingEdge : v.incomingEdges() ) {
			ret = Math.max( ret, incomingEdge.getSource().getTimepoint() );
		}
		return ret;
	}

	private static InputTriggerConfig getKeyConfig( final TrackSchemeOptions optional ) {
		final InputTriggerConfig conf = optional.values.getInputTriggerConfig();
		return conf != null ? conf : new InputTriggerConfig();
	}

	static class SelectedSegmentsConverter implements Converter< IntType, UnsignedShortType >, SelectionListener {

		private final LabelingPlus labelingPlus;

		private final Selection< SegmentVertex, SubsetEdge > selectionModel;

		private int[] intensities;

		public SelectedSegmentsConverter(
				final LabelingPlus labelingPlus,
				final Selection< SegmentVertex, SubsetEdge > selectionModel ) {
			this.labelingPlus = labelingPlus;
			this.selectionModel = selectionModel;
			selectionModel.listeners().add( this );
			selectionChanged();
		}

		@Override
		public void convert( final IntType input, final UnsignedShortType output ) {
			output.set( intensities[ input.get() ] );
		}

		@Override
		public void selectionChanged() {
			final int numSets = labelingPlus.getLabeling().getMapping().numSets();
			if ( intensities == null || intensities.length < numSets )
				intensities = new int[ numSets ];
			Arrays.fill( intensities, 0 );

			final ArrayList< LabelingFragment > fragments = labelingPlus.getFragments();

			for ( final SegmentVertex v : selectionModel.getSelectedVertices() ) {
				if ( v == null ) continue;
				if ( v.getLabelData() != null ) {
					for ( final int i : v.getLabelData().getFragmentIndices() ) {
						++intensities[ fragments.get( i ).getLabelingMappingIndex() ];
					}
				}
			}
		}
	}

	static class HighlightedSegmentsConverter implements Converter< IntType, UnsignedShortType >, HighlightListener {

		private final LabelingPlus labelingPlus;

		private final HighlightModel< SegmentVertex, SubsetEdge > highlightModel;

		private int[] intensities;

		public HighlightedSegmentsConverter(
				final LabelingPlus labelingPlus,
				final HighlightModel< SegmentVertex, SubsetEdge > highlightModel ) {
			this.labelingPlus = labelingPlus;
			this.highlightModel = highlightModel;
			highlightModel.listeners().add( this );
			highlightChanged();
		}

		@Override
		public void convert( final IntType input, final UnsignedShortType output ) {
			output.set( intensities[ input.get() ] );
		}

		@Override
		public void highlightChanged() {
			final int numSets = labelingPlus.getLabeling().getMapping().numSets();
			if ( intensities == null || intensities.length < numSets )
				intensities = new int[ numSets ];
			Arrays.fill( intensities, 0 );

			final SegmentVertex v = highlightModel.getHighlightedVertex( null );
			if ( v != null ) {
				final ArrayList< LabelingFragment > fragments = labelingPlus.getFragments();
				for ( final int i : v.getLabelData().getFragmentIndices() )
					++intensities[ fragments.get( i ).getLabelingMappingIndex() ];
			}
		}
	}

	static class FocusedSegmentsConverter implements Converter< IntType, UnsignedShortType >, FocusListener {

		private final LabelingPlus labelingPlus;

		private final FocusModel< SegmentVertex, SubsetEdge > focusModel;

		private int[] intensities;

		public FocusedSegmentsConverter(
				final LabelingPlus labelingPlus,
				final FocusModel< SegmentVertex, SubsetEdge > focusModel ) {
			this.labelingPlus = labelingPlus;
			this.focusModel = focusModel;
			focusModel.listeners().add( this );
			focusChanged();
		}

		@Override
		public void convert( final IntType input, final UnsignedShortType output ) {
			output.set( intensities[ input.get() ] );
		}

		@Override
		public void focusChanged() {
			final int numSets = labelingPlus.getLabeling().getMapping().numSets();
			if ( intensities == null || intensities.length < numSets )
				intensities = new int[ numSets ];
			Arrays.fill( intensities, 0 );

			final SegmentVertex v = focusModel.getFocusedVertex( null );
			if ( v != null ) {
				final ArrayList< LabelingFragment > fragments = labelingPlus.getFragments();
				for ( final int i : v.getLabelData().getFragmentIndices() )
					++intensities[ fragments.get( i ).getLabelingMappingIndex() ];
			}
		}
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed( final ActionEvent e ) {
		boolean reprepAndRun = false;

		// FRAME NAVIGATION
		if ( e.getSource().equals( buttonFirst ) ) {
			emptyRedoStack();
			this.currentFrame = 0;
			setFrameToShow( this.currentFrame );
			selectionFromCurrentSolution();
		} else if ( e.getSource().equals( buttonPrev ) ) {
			emptyRedoStack();
			this.currentFrame = Math.max( 0, currentFrame - 1 );
			setFrameToShow( this.currentFrame );
			selectionFromCurrentSolution();
		} else if ( e.getSource().equals( buttonNext ) ) {
			emptyRedoStack();
			this.currentFrame = Math.min( model.getLabelingFrames().getNumFrames() - 1, currentFrame + 1 );
			setFrameToShow( this.currentFrame );
			selectionFromCurrentSolution();
		} else if ( e.getSource().equals( buttonLast ) ) {
			emptyRedoStack();
			this.currentFrame = model.getLabelingFrames().getNumFrames() - 1;
			setFrameToShow( this.currentFrame );
			selectionFromCurrentSolution();
		} else if ( e.getSource().equals( txtCurFrame ) ) {
			emptyRedoStack();
			this.currentFrame = Math.max(
					0,
					Math.min(
							model.getLabelingFrames().getNumFrames() - 1,
							Integer.parseInt( txtCurFrame.getText() ) ) );
			setFrameToShow( this.currentFrame );
			selectionFromCurrentSolution();

		// LEVERAGED EDITING BUTTONS
		} else if ( e.getSource().equals( bUndo ) ) {
			callUndo();
		} else if ( e.getSource().equals( bRedo ) ) {
			callRedo();
		} else if ( e.getSource().equals( bForceSelected ) ) {
			forceCurrentSelection();
			reprepAndRun = true;
		} else if ( e.getSource().equals( bForceAppearance ) ) {
			forceCurrentSelectionToAppear();
			reprepAndRun = true;
		} else if ( e.getSource().equals( bForceDisappearance ) ) {
			forceCurrentSelectionToDisappear();
			reprepAndRun = true;
		} else if ( e.getSource().equals( bForceMoveIn ) ) {
			forceCurrentSelectionToBeMovedTo();
			reprepAndRun = true;
		} else if ( e.getSource().equals( bForceDivisionIn ) ) {
			forceCurrentSelectionToBeDividedTo();
			reprepAndRun = true;
		} else if ( e.getSource().equals( bForceMoveOut ) ) {
			forceCurrentSelectionToBeMovedFrom();
			reprepAndRun = true;
		} else if ( e.getSource().equals( bForceDivisionOut ) ) {
			forceCurrentSelectionToBeDividedFrom();
			reprepAndRun = true;
		} else if ( e.getSource().equals( bAvoidSelected ) ) {
			avoidCurrentSelection();
			reprepAndRun = true;
		} else if ( e.getSource().equals( bForceSelectionExactly ) ) {
			final Tr2dSegmentationProblem segProblem = model.getTrackingProblem().getTimepoints().get( this.currentFrame );
			// avoid all...
			for ( final SegmentNode segNode : segProblem.getSegments() ) {
				segProblem.avoid( segNode );
			}
			// ...then force selected
			forceCurrentSelection();
			reprepAndRun = true;

		// SELECTION RELATED
		} else if ( e.getSource().equals( bSelectionFromSolution ) ) {
			selectionFromCurrentSolution();

			// ASSIGNMENT INSPECTION RELATED (DEBUG)
		} else if ( e.getSource().equals( bShowTrackingOverlay ) ) {
			showTrackingOverlay();
		} else if ( e.getSource().equals( bOverlayMovementInAssignmentsForSelection ) ) {
			final Tr2dInAssignmentsOverlayOnSelection overlay = new Tr2dInAssignmentsOverlayOnSelection( model, this.currentFrame, true, false );
			overlay.setSelectedSegmentNodes( getSelectedSegmentNodes() );
			bdvAdd( overlay, "incoming moves overlay" );

		} else if ( e.getSource().equals( bOverlayMovementOutAssignmentsForSelection ) ) {
			final Tr2dOutAssignmentsOverlayOnSelection overlay = new Tr2dOutAssignmentsOverlayOnSelection( model, this.currentFrame, true, false );
			overlay.setSelectedSegmentNodes( getSelectedSegmentNodes() );
			bdvAdd( overlay, "outgoing moves overlay" );

		} else if ( e.getSource().equals( bOverlayDivisionInAssignmentsForSelection ) ) {
			final Tr2dInAssignmentsOverlayOnSelection overlay = new Tr2dInAssignmentsOverlayOnSelection( model, this.currentFrame, false, true );
			overlay.setSelectedSegmentNodes( getSelectedSegmentNodes() );
			bdvAdd( overlay, "incoming divisions overlay" );

		} else if ( e.getSource().equals( bOverlayDivisionOutAssignmentsForSelection ) ) {
			final Tr2dOutAssignmentsOverlayOnSelection overlay = new Tr2dOutAssignmentsOverlayOnSelection( model, this.currentFrame, false, true );
			overlay.setSelectedSegmentNodes( getSelectedSegmentNodes() );
			bdvAdd( overlay, "outgoing divisions overlay" );

		} else if ( e.getSource().equals( bOverlayRemoveAll ) ) {
			bdvRemoveAllOverlays();
		}

		// Since this is the same for all LevEdits, I put it here
		if ( reprepAndRun ) {
			model.prepareFG();
			model.runInThread( true );
		}
	}

	/**
	 *
	 */
	private void pushCurrentStateOnUndoStack() {
		final Tr2dSegmentationProblem segProblem = model.getTrackingProblem().getTimepoints().get( this.currentFrame );
//		JOptionPane.showMessageDialog( null, "push2undo: " + segProblem.getEditState().getDebugString() );
		undoStack.push( new ValuePair< Integer, EditState >( this.currentFrame, new EditState( segProblem.getEditState() ) ) );
		bUndo.setEnabled( true );
	}

	/**
	 */
	private void pushCurrentStateOnRedoStack() {
		final Tr2dSegmentationProblem segProblem = model.getTrackingProblem().getTimepoints().get( this.currentFrame );
//		JOptionPane.showMessageDialog( null, "push2redo: " + segProblem.getEditState().getDebugString() );
		redoStack.push( new ValuePair< Integer, EditState >( this.currentFrame, new EditState( segProblem.getEditState() ) ) );
		bRedo.setEnabled( true );
	}

	/**
	 * Empties undo and redo stacks and disables corresponding buttons.
	 */
	public void emptyUndoRedoStacks() {
		emptyUndoStack();
		emptyRedoStack();
	}

	/**
	 *
	 */
	private void emptyUndoStack() {
		undoStack.removeAllElements();
		bUndo.setEnabled( false );
	}

	/**
	 *
	 */
	private void emptyRedoStack() {
		redoStack.removeAllElements();
		bRedo.setEnabled( false );
	}

	/**
	 *
	 */
	private void callUndo() {
		if ( !undoStack.isEmpty() ) {
			pushCurrentStateOnRedoStack();

			final Pair< Integer, EditState > poppedStatePair = undoStack.pop();
			if ( undoStack.isEmpty() ) {
				bUndo.setEnabled( false );
			}

			final int editTime = poppedStatePair.getA();
			final EditState oldState = poppedStatePair.getB();

			final Tr2dSegmentationProblem segProblem = model.getTrackingProblem().getTimepoints().get( editTime );
			segProblem.setEditState( oldState );

//			JOptionPane.showMessageDialog( null, "Undo to: " + oldState.getDebugString() );

			if ( this.currentFrame != editTime ) {
				setFrameToShow( editTime );
			}
			model.prepareFG();
			model.runInThread( true );
		}
	}

	private void callRedo() {
		if ( !redoStack.isEmpty() ) {
			pushCurrentStateOnUndoStack();

			final Pair< Integer, EditState > poppedStatePair = redoStack.pop();
			if ( redoStack.isEmpty() ) {
				bRedo.setEnabled( false );
			}

			final int editTime = poppedStatePair.getA();
			final EditState redoState = poppedStatePair.getB();

			final Tr2dSegmentationProblem segProblem = model.getTrackingProblem().getTimepoints().get( editTime );
			segProblem.setEditState( redoState );

//			JOptionPane.showMessageDialog( null, "Redo to: " + redoState.getDebugString() );

			if ( this.currentFrame != editTime ) {
				setFrameToShow( editTime );
			}
			model.prepareFG();
			model.runInThread( true );
		}
	}

	private void avoidCurrentSelection() {
		pushCurrentStateOnUndoStack();
		emptyRedoStack();
		final Tr2dSegmentationProblem segProblem = model.getTrackingProblem().getTimepoints().get( this.currentFrame );
		for ( final SegmentVertex selectedSegmentVertex : selectionModel.getSelectedVertices() ) {
			final LabelingSegment labelingSegment = selectedSegmentVertex.getLabelData().getSegment();
			final SegmentNode segVar = segProblem.getSegmentVar( labelingSegment );
			Tr2dLog.log.info( "Avoiding: " + segVar.toString() );
			segProblem.avoid( segVar );
		}
	}

	private void forceCurrentSelection() {
		pushCurrentStateOnUndoStack();
		emptyRedoStack();
		final Tr2dSegmentationProblem segProblem = model.getTrackingProblem().getTimepoints().get( this.currentFrame );
		for ( final SegmentVertex selectedSegmentVertex : selectionModel.getSelectedVertices() ) {
			final LabelingSegment labelingSegment = selectedSegmentVertex.getLabelData().getSegment();
			final SegmentNode segVar = segProblem.getSegmentVar( labelingSegment );
			Tr2dLog.log.info( "Forcing: " + segVar.toString() );
			segProblem.force( segVar );
		}
	}

	private void forceCurrentSelectionToBeDividedTo() {
		pushCurrentStateOnUndoStack();
		emptyRedoStack();
		final Tr2dSegmentationProblem segProblem = model.getTrackingProblem().getTimepoints().get( this.currentFrame );
		for ( final SegmentVertex selectedSegmentVertex : selectionModel.getSelectedVertices() ) {
			final LabelingSegment labelingSegment = selectedSegmentVertex.getLabelData().getSegment();
			final SegmentNode segVar = segProblem.getSegmentVar( labelingSegment );

			final ConflictSet confSet = segProblem.getConflictSetFor( segVar );
			if ( isSelected( confSet ) ) {
				Tr2dLog.log.info( "Forcing division to conflict set: " + confSet.toString() );
				segProblem.forceDivisionTo( confSet );
			} else {
				Tr2dLog.log.info( "Forcing division to single node: " + segVar.toString() );
    			segProblem.forceDivisionTo( segVar );
			}
		}
	}

	private void forceCurrentSelectionToBeDividedFrom() {
		pushCurrentStateOnUndoStack();
		emptyRedoStack();
		final Tr2dSegmentationProblem segProblem = model.getTrackingProblem().getTimepoints().get( this.currentFrame );
		for ( final SegmentVertex selectedSegmentVertex : selectionModel.getSelectedVertices() ) {
			final LabelingSegment labelingSegment = selectedSegmentVertex.getLabelData().getSegment();
			final SegmentNode segVar = segProblem.getSegmentVar( labelingSegment );

			final ConflictSet confSet = segProblem.getConflictSetFor( segVar );
			if ( isSelected( confSet ) ) {
				Tr2dLog.log.info( "Forcing division from conflict set: " + confSet.toString() );
				segProblem.forceDivisionFrom( confSet );
			} else {
				Tr2dLog.log.info( "Forcing division from single node: " + segVar.toString() );
				segProblem.forceDivisionFrom( segVar );
			}
		}
	}

	private void forceCurrentSelectionToBeMovedTo() {
		pushCurrentStateOnUndoStack();
		emptyRedoStack();
		final Tr2dSegmentationProblem segProblem = model.getTrackingProblem().getTimepoints().get( this.currentFrame );
		for ( final SegmentVertex selectedSegmentVertex : selectionModel.getSelectedVertices() ) {
			final LabelingSegment labelingSegment = selectedSegmentVertex.getLabelData().getSegment();
			final SegmentNode segVar = segProblem.getSegmentVar( labelingSegment );

			final ConflictSet confSet = segProblem.getConflictSetFor( segVar );
			if ( isSelected( confSet ) ) {
				Tr2dLog.log.info( "Forcing move to conflict set: " + confSet.toString() );
				segProblem.forceMoveTo( confSet );
			} else {
				Tr2dLog.log.info( "Forcing move to single node: " + segVar.toString() );
				segProblem.forceMoveTo( segVar );
			}
		}
	}

	private void forceCurrentSelectionToBeMovedFrom() {
		pushCurrentStateOnUndoStack();
		emptyRedoStack();
		final Tr2dSegmentationProblem segProblem = model.getTrackingProblem().getTimepoints().get( this.currentFrame );
		for ( final SegmentVertex selectedSegmentVertex : selectionModel.getSelectedVertices() ) {
			final LabelingSegment labelingSegment = selectedSegmentVertex.getLabelData().getSegment();
			final SegmentNode segVar = segProblem.getSegmentVar( labelingSegment );

			final ConflictSet confSet = segProblem.getConflictSetFor( segVar );
			if ( isSelected( confSet ) ) {
				Tr2dLog.log.info( "Forcing move from conflict set: " + confSet.toString() );
				segProblem.forceMoveFrom( confSet );
			} else {
				Tr2dLog.log.info( "Forcing move from single node: " + segVar.toString() );
				segProblem.forceMoveFrom( segVar );
			}
		}
	}

	private void forceCurrentSelectionToDisappear() {
		pushCurrentStateOnUndoStack();
		emptyRedoStack();
		final Tr2dSegmentationProblem segProblem = model.getTrackingProblem().getTimepoints().get( this.currentFrame );
		for ( final SegmentVertex selectedSegmentVertex : selectionModel.getSelectedVertices() ) {
			final LabelingSegment labelingSegment = selectedSegmentVertex.getLabelData().getSegment();
			final SegmentNode segVar = segProblem.getSegmentVar( labelingSegment );
			Tr2dLog.log.info( "Forcing disappearance of: " + segVar.toString() );
			segProblem.forceDisappearance( segVar );
		}
	}

	private void forceCurrentSelectionToAppear() {
		pushCurrentStateOnUndoStack();
		emptyRedoStack();
		final Tr2dSegmentationProblem segProblem = model.getTrackingProblem().getTimepoints().get( this.currentFrame );
		for ( final SegmentVertex selectedSegmentVertex : selectionModel.getSelectedVertices() ) {
			final LabelingSegment labelingSegment = selectedSegmentVertex.getLabelData().getSegment();
			final SegmentNode segVar = segProblem.getSegmentVar( labelingSegment );
			Tr2dLog.log.info( "Forcing appearance of: " + segVar.toString() );
			segProblem.forceAppearance( segVar );
		}
	}

	/**
	 * @param conflictSet
	 *            ConflictSet instance
	 * @return true, iff all nodes in the given conflict set are currently
	 *         selected
	 */
	private boolean isSelected( final ConflictSet conflictSet ) {
		if ( conflictSet.size() == 0 ) { return false; }
		for ( final SegmentNode node : conflictSet ) {
			if ( !selectionModel.isSelected( mapLabelingSegment2SegmentVertex.get( node.getSegment() ) ) ) { return false; }
		}
		return true;
	}

	/**
	 * Switches to the given frame.
	 * Switches to frame 0 if a number &lt;0 is given and to the last frame if
	 * the given number exceeds the number of frames.
	 *
	 * @param frameNumToShow
	 *            the frame number to be shown
	 */
	public void setFrameToShow( final int frameNumToShow ) {
		this.currentFrame = Math.max(
				0,
				Math.min(
						model.getLabelingFrames().getNumFrames() - 1,
						frameNumToShow ) );
		txtCurFrame.setText( "" + frameNumToShow );
		displayFrameData();
	}

	/**
	 * @see com.indago.ui.bdv.BdvOwner#bdvGetHandlePanel()
	 */
	@Override
	public BdvHandlePanel bdvGetHandlePanel() {
		return bdvHandlePanel;
	}

	/**
	 * @see com.indago.ui.bdv.BdvOwner#bdvSetHandlePanel(bdv.util.BdvHandlePanel)
	 */
	@Override
	public void bdvSetHandlePanel( final BdvHandlePanel bdvHandlePanel ) {
		this.bdvHandlePanel = bdvHandlePanel;
	}

	/**
	 * @see com.indago.ui.bdv.BdvOwner#bdvGetSources()
	 */
	@Override
	public List< BdvSource > bdvGetSources() {
		return bdvSources;
	}

	/**
	 * @see com.indago.ui.bdv.BdvOwner#bdvGetSourceFor(net.imglib2.RandomAccessibleInterval)
	 */
	@Override
	public < T extends RealType< T > & NativeType< T > > BdvSource bdvGetSourceFor( final RandomAccessibleInterval< T > img ) {
		final int idx = imgs.indexOf( img );
		if ( idx == -1 ) return null;
		return bdvGetSources().get( idx );
	}

	/**
	 * @see com.indago.ui.bdv.BdvWithOverlaysOwner#bdvGetOverlaySources()
	 */
	@Override
	public List< BdvSource > bdvGetOverlaySources() {
		return this.bdvOverlaySources;
	}

	/**
	 * @see com.indago.ui.bdv.BdvWithOverlaysOwner#bdvGetOverlays()
	 */
	@Override
	public List< BdvOverlay > bdvGetOverlays() {
		return this.overlays;
	}

	/**
	 * If a tracking solution exists, the selection of segments will be set to
	 * coincide with all active segments in this current tracking solution.
	 */
	public void selectionFromCurrentSolution() {
		if ( model.getSolution() != null ) { // there must be a current solution... :)
			if ( selectionModel == null ) return;
			selectionModel.clearSelection();
    		final Tr2dSegmentationProblem frameSegmentationModel = model.getTrackingProblem().getTimepoints().get( this.currentFrame );

    		final Assignment< IndicatorNode > pgSolution = model.getSolution();
			for ( final SegmentNode segNode : frameSegmentationModel.getSegments() ) {
    			final LabelingSegment labelingSegment = frameSegmentationModel.getLabelingSegment( segNode );
    			if ( pgSolution.getAssignment( segNode ) == 1 ) {
					selectionModel.setSelected( mapLabelingSegment2SegmentVertex.get( labelingSegment ), true );
    			} else {
					selectionModel.setSelected( mapLabelingSegment2SegmentVertex.get( labelingSegment ), false );
    			}
    		}

			// Add the tracking overlay for this time point
			showTrackingOverlay();
		} else {
			selectionModel.clearSelection();
			bdvRemoveAllOverlays();
		}
	}

	/**
	 * Shows the tracking overlay for the current time point.
	 */
	private void showTrackingOverlay() {
		bdvRemoveAllOverlays();
		final Tr2dTrackingOverlay overlay = new Tr2dTrackingOverlay( model, this.currentFrame );
		bdvAdd( overlay, "tracking overlay" );
	}

	// --- new stuff -- need refactoring ---

	static abstract class SegmentsColorTable implements ColorTable, VirtualChannel {

		protected final LabelingPlus labelingPlus;

		protected final ColorTableConverter converter;

		protected PlaceHolderOverlayInfo info;

		protected ViewerPanel viewer;

		protected int[] lut;

		public SegmentsColorTable(
				final LabelingPlus labelingPlus,
				final ColorTableConverter converter ) {
			this.labelingPlus = labelingPlus;
			this.converter = converter;
		}

		void setPlaceHolderOverlayInfo( final PlaceHolderOverlayInfo info ) {
			this.info = info;
		}

		void setViewerPanel( final ViewerPanel viewer ) {
			this.viewer = viewer;
		}

		@Override
		public synchronized int[] getLut() {
			if ( info != null && info.isVisible() )
				return lut;
			else
				return null;
		}

		@Override
		public synchronized void updateVisibility() {
			update();
		}

		@Override
		public synchronized void updateSetupParameters() {
			update();
		}

		protected void update() {
			final int numSets = labelingPlus.getLabeling().getMapping().numSets();
			if ( lut == null || lut.length < numSets )
				lut = new int[ numSets ];
			Arrays.fill( lut, 0 );
			fillLut();
			converter.update();
			if ( viewer != null )
				viewer.requestRepaint();
		}

		protected void convertLutToColors() {
			if ( info == null )
				return;

			final double max = info.getDisplayRangeMax();
			final double min = info.getDisplayRangeMin();
			final double scale = 1.0 / ( max - min );
			final int value = info.getColor().get();
			final int A = ARGBType.alpha( value );
			final double scaleR = ARGBType.red( value ) * scale;
			final double scaleG = ARGBType.green( value ) * scale;
			final double scaleB = ARGBType.blue( value ) * scale;
			final int black = ARGBType.rgba( 0, 0, 0, A );
			for ( int i = 0; i < lut.length; ++i ) {
				final double v = lut[ i ] - min;
				if ( v < 0 ) {
					lut[ i ] = black;
				} else {
					final int r0 = ( int ) ( scaleR * v + 0.5 );
					final int g0 = ( int ) ( scaleG * v + 0.5 );
					final int b0 = ( int ) ( scaleB * v + 0.5 );
					final int r = Math.min( 255, r0 );
					final int g = Math.min( 255, g0 );
					final int b = Math.min( 255, b0 );
					lut[ i ] = ARGBType.rgba( r, g, b, A );
				}
			}
		}

		protected abstract void fillLut();
	}

	static class SelectedSegmentsColorTable extends SegmentsColorTable implements SelectionListener {

		private final Selection< SegmentVertex, SubsetEdge > selectionModel;

		public SelectedSegmentsColorTable(
				final LabelingPlus labelingPlus,
				final ColorTableConverter converter,
				final Selection< SegmentVertex, SubsetEdge > selectionModel ) {
			super( labelingPlus, converter );
			this.selectionModel = selectionModel;
			selectionModel.listeners().add( this );
			update();
		}

		@Override
		public synchronized void selectionChanged() {
			update();
		}

		@Override
		protected void fillLut() {
			final ArrayList< LabelingFragment > fragments = labelingPlus.getFragments();
			for ( final SegmentVertex v : selectionModel.getSelectedVertices() ) {
				if ( v == null ) continue;
				for ( final int i : v.getLabelData().getFragmentIndices() )
					++lut[ fragments.get( i ).getLabelingMappingIndex() ];
			}
			convertLutToColors();
		}
	}

	static class HighlightedSegmentsColorTable extends SegmentsColorTable implements HighlightListener {

		private final HighlightModel< SegmentVertex, SubsetEdge > highlightModel;

		public HighlightedSegmentsColorTable(
				final LabelingPlus labelingPlus,
				final ColorTableConverter converter,
				final HighlightModel< SegmentVertex, SubsetEdge > highlightModel ) {
			super( labelingPlus, converter );
			this.highlightModel = highlightModel;
			highlightModel.listeners().add( this );
			update();
		}

		@Override
		public synchronized void highlightChanged() {
			update();
		}

		@Override
		protected void fillLut() {
			final SegmentVertex v = highlightModel.getHighlightedVertex( null );
			if ( v != null ) {
				final ArrayList< LabelingFragment > fragments = labelingPlus.getFragments();
				for ( final int i : v.getLabelData().getFragmentIndices() )
					++lut[ fragments.get( i ).getLabelingMappingIndex() ];
				convertLutToColors();
			}
		}
	}

	static class FocusedSegmentsColorTable extends SegmentsColorTable implements FocusListener {

		private final FocusModel< SegmentVertex, SubsetEdge > focusModel;

		public FocusedSegmentsColorTable(
				final LabelingPlus labelingPlus,
				final ColorTableConverter converter,
				final FocusModel< SegmentVertex, SubsetEdge > focusModel ) {
			super( labelingPlus, converter );
			this.focusModel = focusModel;
			focusModel.listeners().add( this );
			update();
		}

		@Override
		public synchronized void focusChanged() {
			update();
		}

		@Override
		protected void fillLut() {
			final SegmentVertex v = focusModel.getFocusedVertex( null );
			if ( v != null ) {
				final ArrayList< LabelingFragment > fragments = labelingPlus.getFragments();
				for ( final int i : v.getLabelData().getFragmentIndices() )
					++lut[ fragments.get( i ).getLabelingMappingIndex() ];
				convertLutToColors();
			}
		}
	}

	/**
	 * @see com.indago.tr2d.ui.listener.SolutionChangedListener#solutionChanged(com.indago.fg.Assignment)
	 */
	@Override
	public void solutionChanged( final Assignment< IndicatorNode > newAssignment ) {
		selectionFromCurrentSolution();
	}

	/**
	 * @return a List< SegmentNode > containing all currently selected segments.
	 */
	private List< SegmentNode > getSelectedSegmentNodes() {
		final List< SegmentNode > ret = new ArrayList<>();
		final Tr2dSegmentationProblem segProblem = model.getTrackingProblem().getTimepoints().get( this.currentFrame );
		for ( final SegmentVertex selectedSegmentVertex : selectionModel.getSelectedVertices() ) {
			final LabelingSegment labelingSegment = selectedSegmentVertex.getLabelData().getSegment();
			final SegmentNode segVar = segProblem.getSegmentVar( labelingSegment );
			ret.add( segVar );
		}
		return ret;
	}

	/**
	 * @see com.indago.tr2d.ui.listener.ModelInfeasibleListener#modelIsInfeasible()
	 */
	@Override
	public void modelIsInfeasible() {
		final String[] options = new String[] { "Undo", "Restart", "Cancel" };
		final int response = JOptionPane.showOptionDialog(
				this,
				"No solution can be found, the current model is infeasible.\nPlease choose your preferred action:",
				"Model infeasible",
				JOptionPane.DEFAULT_OPTION,
				JOptionPane.QUESTION_MESSAGE,
				null,
				options,
				options[ 0 ] );
		switch ( response ) {
		case 0:
			callUndo();
			model.prepareFG();
			model.runInThread( true );
			break;
		case 1:
			SwingUtilities.invokeLater( new Runnable() {
				@Override
				public void run() {
					emptyUndoRedoStacks();
					model.runInThread( true, true );
				}

			} );
			break;
		}
	}
}
