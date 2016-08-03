package com.indago.app.selectsegment;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.scijava.ui.behaviour.MouseAndKeyHandler;
import org.scijava.ui.behaviour.io.InputTriggerConfig;

import com.indago.data.segmentation.LabelData;
import com.indago.data.segmentation.LabelingFragment;
import com.indago.data.segmentation.LabelingPlus;
import com.indago.data.segmentation.XmlIoLabelingPlus;

import bdv.BehaviourTransformEventHandler;
import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvSource;
import bdv.viewer.InputActionBindings;
import bdv.viewer.TriggerBehaviourBindings;
import gnu.trove.impl.Constants;
import gnu.trove.set.hash.TLongHashSet;
import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.ui.TransformEventHandler;
import net.imglib2.ui.util.GuiUtil;
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

public class BuildSegmentGraph
{

	static void display(
			final SegmentGraph modelGraph,
			final LabelingPlus labelingPlus ) {
		final GroupManager manager = new GroupManager();
		final GroupHandle trackSchemeGroupHandle = manager.createGroupHandle();
		final TrackSchemeOptions optional = TrackSchemeOptions.options();

		final GraphIdBimap< SegmentVertex, SubsetEdge > idmap = modelGraph.getGraphIdBimap();

		final Selection< SegmentVertex, SubsetEdge > selectionModel = new Selection<>( modelGraph, idmap );
		final HighlightModel< SegmentVertex, SubsetEdge > highlightModel = new HighlightModel<>( idmap );
		final FocusModel< SegmentVertex, SubsetEdge > focusModel = new FocusModel<>( idmap );
		final NavigationHandler< SegmentVertex, SubsetEdge > navigationHandler = new NavigationHandler<>( trackSchemeGroupHandle );

		final InputTriggerConfig inputConf = getKeyConfig( optional );

		// === TrackScheme ===

		final ModelGraphProperties modelGraphProperties = new DefaultModelGraphProperties<>( modelGraph, idmap, selectionModel );
		final TrackSchemeGraph< SegmentVertex, SubsetEdge > trackSchemeGraph = new TrackSchemeGraph<>( modelGraph, idmap, modelGraphProperties );
		final TrackSchemePanel trackschemePanel = new TrackSchemePanel(
				trackSchemeGraph,
				new TrackSchemeHighlight( new DefaultModelHighlightProperties<>( modelGraph, idmap, highlightModel ), trackSchemeGraph ),
				new TrackSchemeFocus( new DefaultModelFocusProperties<>( modelGraph, idmap, focusModel ), trackSchemeGraph ),
				new TrackSchemeSelection( new DefaultModelSelectionProperties<>( modelGraph, idmap, selectionModel ) ),
				new TrackSchemeNavigation( new DefaultModelNavigationProperties<>( modelGraph, idmap, navigationHandler ), trackSchemeGraph ),
				optional );

		final JFrame frame = new JFrame( "graph", GuiUtil.getSuitableGraphicsConfiguration( GuiUtil.RGB_COLOR_MODEL ) );
		frame.getRootPane().setDoubleBuffered( true );
		frame.add( trackschemePanel, BorderLayout.CENTER );

		frame.pack();
		frame.setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE );
		frame.addWindowListener( new WindowAdapter() {
			@Override
			public void windowClosing( final WindowEvent e ) {
				trackschemePanel.stop();
			}
		} );

		final InputActionBindings keybindings = new InputActionBindings();
		SwingUtilities.replaceUIActionMap( frame.getRootPane(), keybindings.getConcatenatedActionMap() );
		SwingUtilities.replaceUIInputMap( frame.getRootPane(), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, keybindings.getConcatenatedInputMap() );

		final TriggerBehaviourBindings triggerbindings = new TriggerBehaviourBindings();
		final MouseAndKeyHandler mouseAndKeyHandler = new MouseAndKeyHandler();
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
		frame.setVisible( true );

		// === BDV ===

		final BdvSource selectionSource = BdvFunctions.show(
				Converters.convert(
						labelingPlus.getLabeling().getIndexImg(),
						new SelectedSegmentsConverter( labelingPlus, selectionModel ),
						new UnsignedShortType() ),
				"selected segments",
				Bdv.options().inputTriggerConfig( inputConf ).is2D() );
		final Bdv bdv = selectionSource;

		final BdvSource highlightSource = BdvFunctions.show(
				Converters.convert(
						labelingPlus.getLabeling().getIndexImg(),
						new HighlightedSegmentsConverter( labelingPlus, highlightModel ),
						new UnsignedShortType() ),
				"highlighted segment",
				Bdv.options().addTo( bdv ) );

		final BdvSource focusSource = BdvFunctions.show(
				Converters.convert(
						labelingPlus.getLabeling().getIndexImg(),
						new FocusedSegmentsConverter( labelingPlus, focusModel ),
						new UnsignedShortType() ),
				"focused segment",
				Bdv.options().addTo( bdv ) );

		selectionSource.setDisplayRange( 0, 2 );
		selectionSource.setColor( new ARGBType( 0x00FF00 ) );

		highlightSource.setDisplayRange( 0, 1 );
		highlightSource.setColor( new ARGBType( 0xFF00FF ) );

		focusSource.setDisplayRange( 0, 1 );
		focusSource.setColor( new ARGBType( 0x0000FF ) );

		highlightModel.addHighlightListener( () -> bdv.getBdvHandle().getViewerPanel().requestRepaint() );
		selectionModel.addSelectionListener( () -> bdv.getBdvHandle().getViewerPanel().requestRepaint() );
		focusModel.addFocusListener( () -> bdv.getBdvHandle().getViewerPanel().requestRepaint() );



		// debug... show slice of raw image...
//		String fn = "/Users/pietzsch/Desktop/tr2d_test/raw_slice0000.tif";
		final String fn = "/Users/jug/MPI/ProjectHernan/Tr2dProjectPath/DebugStack03-crop/raw.tif";
		final BdvSource raw = BdvFunctions.show(
				( RandomAccessibleInterval ) ImageJFunctions.wrap( new ImagePlus( fn ) ),
				"raw",
				BdvOptions.options().addTo( bdv ) );
		raw.setDisplayRange( 0, 1000 );
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

		private final HighlightModel< SegmentVertex, SubsetEdge > highlightModel ;

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
			if( v != null )
			{
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
		public void focusChanged()
		{
			final int numSets = labelingPlus.getLabeling().getMapping().numSets();
			if ( intensities == null || intensities.length < numSets )
				intensities = new int[ numSets ];
			Arrays.fill( intensities, 0 );

			final SegmentVertex v = focusModel.getFocusedVertex( null );
			if( v != null )
			{
				final ArrayList< LabelingFragment > fragments = labelingPlus.getFragments();
				for ( final int i : v.getLabelData().getFragmentIndices() )
					++intensities[ fragments.get( i ).getLabelingMappingIndex() ];
			}
		}
	}

	private static InputTriggerConfig getKeyConfig( final TrackSchemeOptions optional )
	{
		final InputTriggerConfig conf = optional.values.getInputTriggerConfig();
		return conf != null ? conf : new InputTriggerConfig();
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

	public static void main( final String[] args ) throws IOException {

		System.setProperty( "apple.laf.useScreenMenuBar", "true" );

//		final String folder = "/Users/pietzsch/Desktop/data/tr2d/tr2d_project_folder/DebugStack03-crop/tracking/labeling_frames/";
		final String folder = "/Users/jug/MPI/ProjectHernan/Tr2dProjectPath/DebugStack03-crop/tracking/labeling_frames/";

		final String fLabeling = folder + "labeling_frame0000.xml";

		final LabelingPlus labelingPlus = new XmlIoLabelingPlus().load( fLabeling );

		final SegmentGraph graph = new SegmentGraph();
		final ShortestPath< SegmentVertex, SubsetEdge > sp = new ShortestPath<>( graph, true );

		// create vertices for all segments
		final Map< LabelData, SegmentVertex > mapVertices = new HashMap<>();
		for ( final LabelData segment : labelingPlus.getLabeling().getMapping().getLabels() )
			mapVertices.put( segment, graph.addVertex().init( segment ) );

		final CheckedPairs pairs = new CheckedPairs( graph.getGraphIdBimap() );
		// Build partial order graph
		for ( final LabelingFragment fragment : labelingPlus.getFragments() ) {
			final ArrayList< LabelData > conflictingSegments = fragment.getSegments();

			// connect regarding subset relation (while removing transitive edges)
			for ( final LabelData subset : conflictingSegments ) {
				final SegmentVertex subv = mapVertices.get( subset );
				for ( final LabelData superset : conflictingSegments ) {
					if ( subset.equals( superset ) )
						continue;
					final SegmentVertex superv = mapVertices.get( superset );

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
					new BreadthFirstIterator<>( subv, graph ).forEachRemaining( v -> descendants.add( v ) );
					new InverseBreadthFirstIterator<>( superv, graph ).forEachRemaining( v -> ancestors.add( v ) );
					final ArrayList< SubsetEdge > remove = new ArrayList<>();
					for ( final SegmentVertex a : ancestors )
						for ( final SubsetEdge edge : a.outgoingEdges() )
							if ( descendants.contains( edge.getTarget() ) )
								remove.add( edge );
					remove.forEach( edge -> graph.remove( edge ) );

					// Add the edge, finally.
					graph.addEdge( superv, subv );
				}
			}
		}

		// Find all roots
		final Set< SegmentVertex > roots = new HashSet<>();
		for ( final SegmentVertex v : graph.vertices() ) {
			if ( v.incomingEdges().isEmpty() ) {
				roots.add( v );
			}
		}
		// For each root perform BFS and push level number (timepoint) through graph.
		for ( final SegmentVertex root : roots ) {
			root.setTimepoint( 0 );
			final BreadthFirstIterator< SegmentVertex, SubsetEdge > bfi =
					new BreadthFirstIterator<>( root, graph );
			while ( bfi.hasNext() ) {
				final SegmentVertex v = bfi.next();
				v.setTimepoint( getMaxParentTimepoint( v ) + 1 );
			}
		}

		display( graph, labelingPlus );
	}

	/**
	 * Iterates over all parents of <code>v</code> and returns the maximum
	 * timepoint found.
	 *
	 * @param v
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
}
