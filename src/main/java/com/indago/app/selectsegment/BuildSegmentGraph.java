package com.indago.app.selectsegment;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
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
import bdv.viewer.InputActionBindings;
import bdv.viewer.TriggerBehaviourBindings;
import gnu.trove.impl.Constants;
import gnu.trove.set.hash.TLongHashSet;
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
import net.trackmate.revised.ui.selection.FocusModel;
import net.trackmate.revised.ui.selection.HighlightModel;
import net.trackmate.revised.ui.selection.NavigationHandler;
import net.trackmate.revised.ui.selection.Selection;

public class BuildSegmentGraph
{

	static void wrap( final SegmentGraph modelGraph ) {
		final GroupManager manager = new GroupManager();
		final GroupHandle trackSchemeGroupHandle = manager.createGroupHandle();
		final TrackSchemeOptions optional = TrackSchemeOptions.options();

		final GraphIdBimap< SegmentVertex, SubsetEdge > idmap = modelGraph.getGraphIdBimap();

		final Selection< SegmentVertex, SubsetEdge > selectionModel = new Selection<>( modelGraph, idmap );
		final HighlightModel< SegmentVertex, SubsetEdge > highlightModel = new HighlightModel<>( idmap );
		final FocusModel< SegmentVertex, SubsetEdge > focusModel = new FocusModel<>( idmap );
		final NavigationHandler< SegmentVertex, SubsetEdge > navigationHandler = new NavigationHandler<>( trackSchemeGroupHandle );

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
		frame.addWindowListener( new WindowAdapter()
		{
			@Override
			public void windowClosing( final WindowEvent e )
			{
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

		final InputTriggerConfig inputConf = getKeyConfig( optional );
		trackschemePanel.getNavigator().installActionBindings( keybindings, inputConf );
		trackschemePanel.getSelectionBehaviours().installBehaviourBindings( triggerbindings, inputConf );

		int maxTimepoint = 0;
		for ( final SegmentVertex v : modelGraph.vertices() )
			maxTimepoint = Math.max( v.getTimepoint(), maxTimepoint );
		trackschemePanel.setTimepointRange( 0, maxTimepoint );
		trackschemePanel.graphChanged();
		frame.setVisible( true );
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

		final String folder = "/Users/pietzsch/Desktop/data/tr2d/tr2d_project_folder/DebugStack03-crop/tracking/labeling_frames/";
//		final String folder = "/Users/jug/MPI/ProjectHernan/Tr2dProjectPath/DebugStack03-crop/tracking/labeling_frames/";

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
					new InverseBreadthFirstIterator< >( superv, graph ).forEachRemaining( v -> ancestors.add( v ) );
					final ArrayList< SubsetEdge > remove = new ArrayList< >();
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
			root.timepoint = 0;
			final BreadthFirstIterator< SegmentVertex, SubsetEdge > bfi =
					new BreadthFirstIterator<>( root, graph );
			while ( bfi.hasNext() ) {
				final SegmentVertex v = bfi.next();
				v.timepoint = getMaxParentTimepoint( v ) + 1;
			}
		}

		for ( final SubsetEdge e : graph.edges() )
			System.out.println( e );

		wrap( graph );
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
			ret = Math.max( ret, incomingEdge.getSource().timepoint );
		}
		return ret;
	}
}
