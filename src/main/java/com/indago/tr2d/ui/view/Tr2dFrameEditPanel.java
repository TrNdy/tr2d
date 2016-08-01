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

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.scijava.ui.behaviour.MouseAndKeyHandler;
import org.scijava.ui.behaviour.io.InputTriggerConfig;

import com.indago.app.selectsegment.InverseBreadthFirstIterator;
import com.indago.app.selectsegment.SegmentGraph;
import com.indago.app.selectsegment.SegmentVertex;
import com.indago.app.selectsegment.SubsetEdge;
import com.indago.data.segmentation.LabelData;
import com.indago.data.segmentation.LabelingFragment;
import com.indago.data.segmentation.LabelingPlus;
import com.indago.data.segmentation.LabelingSegment;
import com.indago.fg.Assignment;
import com.indago.pg.IndicatorNode;
import com.indago.pg.segments.SegmentNode;
import com.indago.tr2d.pg.Tr2dSegmentationProblem;
import com.indago.tr2d.ui.model.Tr2dTrackingModel;
import com.indago.tr2d.ui.view.bdv.BdvWithOverlaysOwner;

import bdv.BehaviourTransformEventHandler;
import bdv.util.Bdv;
import bdv.util.BdvHandlePanel;
import bdv.util.BdvOverlay;
import bdv.util.BdvSource;
import bdv.viewer.InputActionBindings;
import bdv.viewer.TriggerBehaviourBindings;
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
import net.imglib2.view.Views;
import net.miginfocom.swing.MigLayout;
import net.trackmate.graph.GraphIdBimap;
import net.trackmate.graph.algorithm.ShortestPath;
import net.trackmate.graph.algorithm.traversal.BreadthFirstIterator;
import net.trackmate.revised.trackscheme.DefaultModelFocusProperties;
import net.trackmate.revised.trackscheme.DefaultModelGraphProperties;
import net.trackmate.revised.trackscheme.DefaultModelHighlightProperties;
import net.trackmate.revised.trackscheme.DefaultModelNavigationProperties;
import net.trackmate.revised.trackscheme.DefaultModelSelectionProperties;
import net.trackmate.revised.trackscheme.ModelGraphProperties;
import net.trackmate.revised.trackscheme.TrackSchemeFocus;
import net.trackmate.revised.trackscheme.TrackSchemeGraph;
import net.trackmate.revised.trackscheme.TrackSchemeHighlight;
import net.trackmate.revised.trackscheme.TrackSchemeNavigation;
import net.trackmate.revised.trackscheme.TrackSchemeSelection;
import net.trackmate.revised.trackscheme.display.TrackSchemeOptions;
import net.trackmate.revised.trackscheme.display.TrackSchemePanel;
import net.trackmate.revised.ui.grouping.GroupHandle;
import net.trackmate.revised.ui.grouping.GroupManager;
import net.trackmate.revised.ui.selection.FocusListener;
import net.trackmate.revised.ui.selection.FocusModel;
import net.trackmate.revised.ui.selection.HighlightListener;
import net.trackmate.revised.ui.selection.HighlightModel;
import net.trackmate.revised.ui.selection.NavigationHandler;
import net.trackmate.revised.ui.selection.Selection;
import net.trackmate.revised.ui.selection.SelectionListener;

/**
 * @author jug
 */
public class Tr2dFrameEditPanel extends JPanel implements ActionListener, BdvWithOverlaysOwner {

	private static final long serialVersionUID = -9051482691122695949L;

	private final Tr2dTrackingModel model;

	private int currentFrame;

	// === UI Stuff ====================================================================
	private JPanel helperPanel;
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
	private final GroupManager manager;
	private final GroupHandle trackSchemeGroupHandle;
	private final TrackSchemeOptions optional;
	private final InputTriggerConfig inputConf;

	private final InputActionBindings keybindings;
	private final TriggerBehaviourBindings triggerbindings;
	private final MouseAndKeyHandler mouseAndKeyHandler;

	private SegmentGraph segmentGraph = new SegmentGraph();
	private Map< LabelData, SegmentVertex > mapLabelData2SegmentVertex;
	private Map< LabelingSegment, SegmentVertex > mapLabelingSegment2SegmentVertex;

	private Selection< SegmentVertex, SubsetEdge > selectionModel;
	private HighlightModel< SegmentVertex, SubsetEdge > highlightModel;
	private FocusModel< SegmentVertex, SubsetEdge > focusModel;
	private NavigationHandler< SegmentVertex, SubsetEdge > navigationHandler;

	private TrackSchemePanel trackschemePanel;

	private JButton bForceSelected;
	private JButton bAvoidSelected;
	private JButton bForceSelectionExactly;

	private JButton bSelectionFromSolution;

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

		manager = new GroupManager();
		trackSchemeGroupHandle = manager.createGroupHandle();
		optional = TrackSchemeOptions.options();
		inputConf = getKeyConfig( optional );

		imgs = new ArrayList<>();
		keybindings = new InputActionBindings();
		triggerbindings = new TriggerBehaviourBindings();
		mouseAndKeyHandler = new MouseAndKeyHandler();

		buildGui();

		this.currentFrame = 0;
		displayFrameData();
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
		bAvoidSelected = new JButton( "avoid selected" );
		bAvoidSelected.addActionListener( this );
		bAvoidSelected = new JButton( "avoid selected" );
		bAvoidSelected.addActionListener( this );
		bForceSelectionExactly = new JButton( "all as selected" );
		bForceSelectionExactly.addActionListener( this );
		panelLeveragedEditing.add( bForceSelected, "wrap" );
		panelLeveragedEditing.add( bAvoidSelected, "wrap" );
		panelLeveragedEditing.add( bForceSelectionExactly, "wrap" );

		layout = new MigLayout();
		final JPanel panelSelection = new JPanel( layout );
		panelSelection.setBorder( BorderFactory.createTitledBorder( "selection" ) );
		bSelectionFromSolution = new JButton( "from solution" );
		bSelectionFromSolution.addActionListener( this );
		panelSelection.add( bSelectionFromSolution, "wrap" );

//		bRun = new JButton( "track" );
//		bRun.addActionListener( this );

		controls.add( panelLeveragedEditing, "wrap" );
		controls.add( panelSelection, "wrap" );
//		controls.add( bRun, "growx, wrap" );

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
		txtCurFrame.setText( "" + ( this.currentFrame + 1 ) );
		txtCurFrame.addActionListener( this );

		final JLabel lblNumFrames = new JLabel( "of " + model.getLabelingFrames().getNumFrames() );

		final JPanel panelFrameSwitcher = new JPanel();
		panelFrameSwitcher.add( buttonFirst );
		panelFrameSwitcher.add( buttonPrev );
		panelFrameSwitcher.add( txtCurFrame );
		panelFrameSwitcher.add( lblNumFrames );
		panelFrameSwitcher.add( buttonNext );
		panelFrameSwitcher.add( buttonLast );

		helperPanel = new JPanel( new BorderLayout() );
		helperPanel.setPreferredSize( new Dimension( 1000, 400 ) );
		SwingUtilities.replaceUIActionMap( helperPanel, keybindings.getConcatenatedActionMap() );
		SwingUtilities.replaceUIInputMap( helperPanel, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, keybindings.getConcatenatedInputMap() );

		bdvHandlePanel = new BdvHandlePanel( ( Frame ) this.getTopLevelAncestor(), Bdv.options().is2D() );
		final JSplitPane split = new JSplitPane( JSplitPane.VERTICAL_SPLIT, bdvHandlePanel.getViewerPanel(), helperPanel );
		split.setResizeWeight( 1 );
		split.setOneTouchExpandable( true );

		// ASSEMBLY
		panelRightSide.add( panelFrameSwitcher, BorderLayout.NORTH );
		panelRightSide.add( split, BorderLayout.CENTER );

		final JSplitPane splitPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, controls, panelRightSide );
		this.add( splitPane, BorderLayout.CENTER );
	}

	private void displayFrameData() {
		final LabelingPlus labelingPlus = model.getLabelingFrames().getLabelingPlusForFrame( this.currentFrame );

		if ( labelingPlus != null ) {
			segmentGraph = new SegmentGraph();
			final ShortestPath< SegmentVertex, SubsetEdge > sp = new ShortestPath<>( segmentGraph, true );

			// create vertices for all segments
			mapLabelData2SegmentVertex = new HashMap<>();
			mapLabelingSegment2SegmentVertex = new HashMap<>();
			for ( final LabelData labelData : labelingPlus.getLabeling().getMapping().getLabels() ) {
				mapLabelData2SegmentVertex.put( labelData, segmentGraph.addVertex().init( labelData ) );
				mapLabelingSegment2SegmentVertex.put( labelData.getSegment(), segmentGraph.addVertex().init( labelData ) );
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

		selectionModel = new Selection<>( modelGraph, idmap );
		highlightModel = new HighlightModel<>( idmap );
		focusModel = new FocusModel<>( idmap );
		if ( navigationHandler != null )
			trackSchemeGroupHandle.remove( navigationHandler );
		navigationHandler = new NavigationHandler<>( trackSchemeGroupHandle );

		// === TrackScheme ===

		if ( trackschemePanel != null ) {
			helperPanel.remove( trackschemePanel );
//			trackschemePanel.getDisplay().removeHandler( mouseAndKeyHandler );
		}

		final ModelGraphProperties modelGraphProperties = new DefaultModelGraphProperties<>( modelGraph, idmap, selectionModel );
		final TrackSchemeGraph< SegmentVertex, SubsetEdge > trackSchemeGraph = new TrackSchemeGraph<>( modelGraph, idmap, modelGraphProperties );
		trackschemePanel =
				new TrackSchemePanel(
						trackSchemeGraph,
						new TrackSchemeHighlight(
								new DefaultModelHighlightProperties<>( modelGraph, idmap, highlightModel ),
								trackSchemeGraph ),
						new TrackSchemeFocus(
								new DefaultModelFocusProperties<>( modelGraph, idmap, focusModel ),
								trackSchemeGraph ),
						new TrackSchemeSelection(
								new DefaultModelSelectionProperties<>( modelGraph, idmap, selectionModel ) ),
						new TrackSchemeNavigation(
								new DefaultModelNavigationProperties<>( modelGraph, idmap, navigationHandler ),
								trackSchemeGraph ),
						optional );
		helperPanel.add( trackschemePanel, BorderLayout.CENTER );

		mouseAndKeyHandler.setInputMap( triggerbindings.getConcatenatedInputTriggerMap() );
		mouseAndKeyHandler.setBehaviourMap( triggerbindings.getConcatenatedBehaviourMap() );
		trackschemePanel.getDisplay().addHandler( mouseAndKeyHandler );

		final TransformEventHandler< ? > tfHandler = trackschemePanel.getDisplay().getTransformEventHandler();
		if ( tfHandler instanceof BehaviourTransformEventHandler )
			( ( BehaviourTransformEventHandler< ? > ) tfHandler ).install( triggerbindings );

		trackschemePanel.getNavigator().installActionBindings( keybindings, inputConf );
		trackschemePanel.getSelectionBehaviours().installBehaviourBindings( triggerbindings, inputConf );

		int maxTimepoint = 0;
		for ( final SegmentVertex v : modelGraph.vertices() )
			maxTimepoint = Math.max( v.getTimepoint(), maxTimepoint );
		trackschemePanel.setTimepointRange( 0, maxTimepoint );
		trackschemePanel.graphChanged();

		// === BDV ===

		this.bdvRemoveAll();
		this.bdvRemoveAllOverlays();

		final RandomAccessibleInterval< DoubleType > rawData = model.getTr2dModel().getRawData();
		final int t = Integer.parseInt( this.txtCurFrame.getText() ) - 1;
		this.bdvAdd( Views.hyperSlice( rawData, 2, t ), "RAW" );

		RandomAccessibleInterval< UnsignedShortType > overlay = Converters.convert(
				labelingPlus.getLabeling().getIndexImg(),
				new SelectedSegmentsConverter( labelingPlus, selectionModel ),
				new UnsignedShortType() );
		this.bdvAdd( overlay, "selected segments", 0, 2, new ARGBType( 0x00FF00 ), true );

		overlay = Converters.convert(
				labelingPlus.getLabeling().getIndexImg(),
				new HighlightedSegmentsConverter( labelingPlus, highlightModel ),
				new UnsignedShortType() );
		this.bdvAdd( overlay, "highlighted segment", 0, 1, new ARGBType( 0xFF00FF ), true );

		overlay = Converters.convert(
				labelingPlus.getLabeling().getIndexImg(),
				new FocusedSegmentsConverter( labelingPlus, focusModel ),
				new UnsignedShortType() );
		this.bdvAdd( overlay, "focused segment", 0, 1, new ARGBType( 0x0000FF ), true );

		highlightModel.addHighlightListener( () -> bdvHandlePanel.getBdvHandle().getViewerPanel().requestRepaint() );
		selectionModel.addSelectionListener( () -> bdvHandlePanel.getBdvHandle().getViewerPanel().requestRepaint() );
		focusModel.addFocusListener( () -> bdvHandlePanel.getBdvHandle().getViewerPanel().requestRepaint() );
	}

	/**
	 * Returns {@code true} iff {@code a} is a subset of {@code b}.
	 *
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
	 * @return
	 */
	private static int getMaxParentTimepoint( final SegmentVertex v ) {
		int ret = 0;
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
			selectionModel.addSelectionListener( this );
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

			for ( final SegmentVertex v : selectionModel.getSelectedVertices() )
				for ( final int i : v.getLabelData().getFragmentIndices() )
					++intensities[ fragments.get( i ).getLabelingMappingIndex() ];
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
			highlightModel.addHighlightListener( this );
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
			focusModel.addFocusListener( this );
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
		// FRAME NAVIGATION
		if ( e.getSource().equals( buttonFirst ) ) {
			this.currentFrame = 0;
			setFrameToShow( this.currentFrame );
			selectionFromCurrentSolution();
		} else if ( e.getSource().equals( buttonPrev ) ) {
			this.currentFrame = Math.max( 0, currentFrame - 1 );
			setFrameToShow( this.currentFrame );
			selectionFromCurrentSolution();
		} else if ( e.getSource().equals( buttonNext ) ) {
			this.currentFrame = Math.min( model.getLabelingFrames().getNumFrames() - 1, currentFrame + 1 );
			setFrameToShow( this.currentFrame );
			selectionFromCurrentSolution();
		} else if ( e.getSource().equals( buttonLast ) ) {
			this.currentFrame = model.getLabelingFrames().getNumFrames() - 1;
			setFrameToShow( this.currentFrame );
			selectionFromCurrentSolution();
		} else if ( e.getSource().equals( txtCurFrame ) ) {
			this.currentFrame = Math.max(
					0,
					Math.min(
							model.getLabelingFrames().getNumFrames() - 1,
							Integer.parseInt( txtCurFrame.getText() ) - 1 ) );
			setFrameToShow( this.currentFrame );
			selectionFromCurrentSolution();

		// LEVERAGED EDITING BUTTONS
		} else if ( e.getSource().equals( bForceSelected ) ) {
			forceCurrentSelection();
			model.prepareFG();
			model.runInThread();
		} else if ( e.getSource().equals( bAvoidSelected ) ) {
			avoidCurrentSelection();
			model.prepareFG();
			model.runInThread();
		} else if ( e.getSource().equals( bForceSelectionExactly ) ) {
			final Tr2dSegmentationProblem segProblem = model.getTrackingProblem().getTimepoints().get( this.currentFrame );
			// avoid all...
			for ( final SegmentNode segNode : segProblem.getSegments() ) {
				segProblem.avoid( segNode );
			}
			// ...then force selected
			forceCurrentSelection();
			model.prepareFG();
			model.runInThread();

		// SELECTION RELATED
		} else if ( e.getSource().equals( bSelectionFromSolution ) ) {
			selectionFromCurrentSolution();
		}
	}

	/**
	 *
	 */
	private void avoidCurrentSelection() {
		final Tr2dSegmentationProblem segProblem = model.getTrackingProblem().getTimepoints().get( this.currentFrame );
		for ( final SegmentVertex selectedSegmentVertex : selectionModel.getSelectedVertices() ) {
			final LabelingSegment labelingSegment = selectedSegmentVertex.getLabelData().getSegment();
			final SegmentNode segVar = segProblem.getSegmentVar( labelingSegment );
			System.out.println( "Avoiding: " + segVar.toString() );
			segProblem.avoid( segVar );
		}
	}

	/**
	 *
	 */
	private void forceCurrentSelection() {
		final Tr2dSegmentationProblem segProblem = model.getTrackingProblem().getTimepoints().get( this.currentFrame );
		for ( final SegmentVertex selectedSegmentVertex : selectionModel.getSelectedVertices() ) {
			final LabelingSegment labelingSegment = selectedSegmentVertex.getLabelData().getSegment();
			final SegmentNode segVar = segProblem.getSegmentVar( labelingSegment );
			System.out.println( "Forcing: " + segVar.toString() );
			segProblem.force( segVar );
		}
	}

	/**
	 * Switches to the given frame.
	 * Switches to frame 0 if a number <0 is given and to the last frame if the
	 * given number exceeds the number of frames.
	 */
	public void setFrameToShow( final int frameNumToShow ) {
		this.currentFrame = Math.max(
				0,
				Math.min(
						model.getLabelingFrames().getNumFrames() - 1,
						frameNumToShow ) );
		txtCurFrame.setText( "" + ( frameNumToShow + 1 ) );
		displayFrameData();
	}

	/**
	 * @see com.indago.tr2d.ui.view.bdv.BdvOwner#bdvGetHandlePanel()
	 */
	@Override
	public BdvHandlePanel bdvGetHandlePanel() {
		return bdvHandlePanel;
	}

	/**
	 * @see com.indago.tr2d.ui.view.bdv.BdvOwner#bdvSetHandlePanel(bdv.util.BdvHandlePanel)
	 */
	@Override
	public void bdvSetHandlePanel( final BdvHandlePanel bdvHandlePanel ) {
		this.bdvHandlePanel = bdvHandlePanel;
	}

	/**
	 * @see com.indago.tr2d.ui.view.bdv.BdvOwner#bdvGetSources()
	 */
	@Override
	public List< BdvSource > bdvGetSources() {
		return bdvSources;
	}

	/**
	 * @see com.indago.tr2d.ui.view.bdv.BdvOwner#bdvGetSourceFor(net.imglib2.RandomAccessibleInterval)
	 */
	@Override
	public < T extends RealType< T > & NativeType< T > > BdvSource bdvGetSourceFor( final RandomAccessibleInterval< T > img ) {
		final int idx = imgs.indexOf( img );
		if ( idx == -1 ) return null;
		return bdvGetSources().get( idx );
	}

	/**
	 * @see com.indago.tr2d.ui.view.bdv.BdvWithOverlaysOwner#bdvGetOverlaySources()
	 */
	@Override
	public List< BdvSource > bdvGetOverlaySources() {
		return this.bdvOverlaySources;
	}

	/**
	 * @see com.indago.tr2d.ui.view.bdv.BdvWithOverlaysOwner#bdvGetOverlays()
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
		if (model.getTrackingProblem() != null) { // there must be a current solution... :)
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
		}
	}
}