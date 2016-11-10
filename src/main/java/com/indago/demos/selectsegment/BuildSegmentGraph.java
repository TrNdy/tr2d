package com.indago.demos.selectsegment;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.mastodon.graph.GraphIdBimap;
import org.mastodon.graph.algorithm.ShortestPath;
import org.mastodon.graph.algorithm.traversal.BreadthFirstIterator;
import org.mastodon.graph.algorithm.traversal.GraphSearch.SearchDirection;
import org.mastodon.revised.trackscheme.DefaultModelFocusProperties;
import org.mastodon.revised.trackscheme.DefaultModelGraphProperties;
import org.mastodon.revised.trackscheme.DefaultModelHighlightProperties;
import org.mastodon.revised.trackscheme.DefaultModelNavigationProperties;
import org.mastodon.revised.trackscheme.DefaultModelSelectionProperties;
import org.mastodon.revised.trackscheme.ModelGraphProperties;
import org.mastodon.revised.trackscheme.TrackSchemeFocus;
import org.mastodon.revised.trackscheme.TrackSchemeGraph;
import org.mastodon.revised.trackscheme.TrackSchemeHighlight;
import org.mastodon.revised.trackscheme.TrackSchemeNavigation;
import org.mastodon.revised.trackscheme.TrackSchemeSelection;
import org.mastodon.revised.trackscheme.display.TrackSchemeNavigator.NavigatorEtiquette;
import org.mastodon.revised.trackscheme.display.TrackSchemeOptions;
import org.mastodon.revised.trackscheme.display.TrackSchemePanel;
import org.mastodon.revised.ui.grouping.GroupHandle;
import org.mastodon.revised.ui.grouping.GroupManager;
import org.mastodon.revised.ui.selection.FocusListener;
import org.mastodon.revised.ui.selection.FocusModel;
import org.mastodon.revised.ui.selection.HighlightListener;
import org.mastodon.revised.ui.selection.HighlightModel;
import org.mastodon.revised.ui.selection.NavigationHandler;
import org.mastodon.revised.ui.selection.Selection;
import org.mastodon.revised.ui.selection.SelectionListener;
import org.scijava.ui.behaviour.MouseAndKeyHandler;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.io.yaml.YamlConfigIO;
import org.scijava.ui.behaviour.util.InputActionBindings;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

import com.indago.data.segmentation.LabelData;
import com.indago.data.segmentation.LabelingFragment;
import com.indago.data.segmentation.LabelingPlus;
import com.indago.data.segmentation.XmlIoLabelingPlus;

import bdv.BehaviourTransformEventHandler;
import bdv.tools.InitializeViewerState;
import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvSource;
import bdv.util.BdvVirtualChannelSource;
import bdv.util.PlaceHolderOverlayInfo;
import bdv.util.VirtualChannels.VirtualChannel;
import bdv.viewer.ViewerPanel;
import gnu.trove.impl.Constants;
import gnu.trove.set.hash.TLongHashSet;
import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.ui.TransformEventHandler;
import net.imglib2.ui.util.GuiUtil;

public class BuildSegmentGraph
{

	static void display(
			final SegmentGraph modelGraph,
			final LabelingPlus labelingPlus,
			final TrackSchemeOptions optional ) {
		final GroupManager manager = new GroupManager();
		final GroupHandle trackSchemeGroupHandle = manager.createGroupHandle();

		final GraphIdBimap< SegmentVertex, SubsetEdge > idmap = modelGraph.getGraphIdBimap();

		final Selection< SegmentVertex, SubsetEdge > selectionModel = new Selection<>( modelGraph, idmap );
		final Selection< SegmentVertex, SubsetEdge > segmentsUnderMouse = new Selection<>( modelGraph, idmap );
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

		trackschemePanel.getNavigator().installActionBindings( keybindings, inputConf, NavigatorEtiquette.FINDER_LIKE );
		trackschemePanel.getNavigator().installBehaviourBindings( triggerbindings, inputConf );

		int maxTimepoint = 0;
		for ( final SegmentVertex v : modelGraph.vertices() )
			maxTimepoint = Math.max( v.getTimepoint(), maxTimepoint );
		trackschemePanel.setTimepointRange( 0, maxTimepoint );
		trackschemePanel.graphChanged();
		frame.setVisible( true );

		// === BDV ===

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
				"segments",
				Bdv.options().inputTriggerConfig( inputConf ).is2D().screenScales( new double[]{ 1 } ) );
		final Bdv bdv = vchanSources.get( 0 );

		for ( int i = 0; i < virtualChannels.size(); ++i ) {
			virtualChannels.get( i ).setPlaceHolderOverlayInfo( vchanSources.get( i ).getPlaceHolderOverlayInfo() );
			virtualChannels.get( i ).setViewerPanel( bdv.getBdvHandle().getViewerPanel() );
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

		final ViewerPanel viewer = bdv.getBdvHandle().getViewerPanel();
		InitializeViewerState.initTransform( viewer );
		final AffineTransform3D t = new AffineTransform3D();
		viewer.getState().getViewerTransform( t );
		t.set( 0, 2, 3 );
		viewer.setCurrentViewerTransform( t );

		// debug... show slice of raw image...
//		final String fn = "/Users/pietzsch/Desktop/tr2d_test/raw_slice0000.tif";
		final String fn = "/Users/jug/MPI/ProjectHernan/Tr2dProjectPath/DebugStack03-crop/raw_slice0000.tif";
		final BdvSource raw = BdvFunctions.show(
				( RandomAccessibleInterval ) ImageJFunctions.wrap( new ImagePlus( fn ) ),
				"raw",
				BdvOptions.options().addTo( bdv ) );
		raw.setDisplayRange( 0, 1000 );

		// add "browse segments" behaviour to bdv
		new SegmentBrowser( bdv, labelingPlus, modelGraph, segmentsUnderMouse, highlightModel, selectionModel, inputConf );
	}

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

		protected void convertLutToColors()
		{
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
			for ( int i = 0; i < lut.length; ++i )
			{
				final double v = lut[ i ] - min;
				if ( v < 0 )
				{
					lut[ i ] = black;
				}
				else
				{
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
			selectionModel.addSelectionListener( this );
			update();
		}

		@Override
		public synchronized void selectionChanged() {
			update();
		}

		@Override
		protected void fillLut()
		{
			final ArrayList< LabelingFragment > fragments = labelingPlus.getFragments();
			for ( final SegmentVertex v : selectionModel.getSelectedVertices() )
				for ( final int i : v.getLabelData().getFragmentIndices() )
					++lut[ fragments.get( i ).getLabelingMappingIndex() ];
			convertLutToColors();
		}
	}

	static class HighlightedSegmentsColorTable extends SegmentsColorTable implements HighlightListener {

		private final HighlightModel< SegmentVertex, SubsetEdge > highlightModel ;

		public HighlightedSegmentsColorTable(
				final LabelingPlus labelingPlus,
				final ColorTableConverter converter,
				final HighlightModel< SegmentVertex, SubsetEdge > highlightModel ) {
			super( labelingPlus, converter );
			this.highlightModel = highlightModel;
			highlightModel.addHighlightListener( this );
			update();
		}

		@Override
		public synchronized void highlightChanged() {
			update();
		}

		@Override
		protected void fillLut()
		{
			final SegmentVertex v = highlightModel.getHighlightedVertex( null );
			if( v != null )
			{
				final ArrayList< LabelingFragment > fragments = labelingPlus.getFragments();
				for ( final int i : v.getLabelData().getFragmentIndices() )
					++lut[ fragments.get( i ).getLabelingMappingIndex() ];
				convertLutToColors();
			}
		}
	}

	static class FocusedSegmentsColorTable extends SegmentsColorTable implements FocusListener {

		private final FocusModel< SegmentVertex, SubsetEdge > focusModel ;

		public FocusedSegmentsColorTable(
				final LabelingPlus labelingPlus,
				final ColorTableConverter converter,
				final FocusModel< SegmentVertex, SubsetEdge > focusModel ) {
			super( labelingPlus, converter );
			this.focusModel = focusModel;
			focusModel.addFocusListener( this );
			update();
		}

		@Override
		public synchronized void focusChanged() {
			update();
		}

		@Override
		protected void fillLut()
		{
			final SegmentVertex v = focusModel.getFocusedVertex( null );
			if( v != null )
			{
				final ArrayList< LabelingFragment > fragments = labelingPlus.getFragments();
				for ( final int i : v.getLabelData().getFragmentIndices() )
					++lut[ fragments.get( i ).getLabelingMappingIndex() ];
				convertLutToColors();
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

//		final InputTriggerConfig config = new InputTriggerConfig( YamlConfigIO.read( "/Users/pietzsch/Desktop/tr2d.yaml" ) );
		final InputTriggerConfig config = new InputTriggerConfig( YamlConfigIO.read( "/Users/jug/MPI/ProjectHernan/Tr2dProjectPath/tr2d.yaml" ) );
//		final InputTriggerConfig config = new InputTriggerConfig();

		final String fLabeling = folder + "labeling_frame0000.xml";

		final LabelingPlus labelingPlus = new XmlIoLabelingPlus().load( fLabeling );

		final SegmentGraph graph = new SegmentGraph();
		final ShortestPath< SegmentVertex, SubsetEdge > sp = new ShortestPath<>( graph, SearchDirection.DIRECTED );

		// create vertices for all segments
		for ( final LabelData segment : labelingPlus.getLabeling().getMapping().getLabels() )
			graph.addVertex().init( segment );

		final CheckedPairs pairs = new CheckedPairs( graph.getGraphIdBimap() );
		// Build partial order graph
		for ( final LabelingFragment fragment : labelingPlus.getFragments() ) {
			final ArrayList< LabelData > conflictingSegments = fragment.getSegments();

			// connect regarding subset relation (while removing transitive edges)
			for ( final LabelData subset : conflictingSegments ) {
				final SegmentVertex subv = graph.getVertexForLabel( subset );
				for ( final LabelData superset : conflictingSegments ) {
					if ( subset.equals( superset ) )
						continue;
					final SegmentVertex superv = graph.getVertexForLabel( superset );

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

		display( graph, labelingPlus, TrackSchemeOptions.options().inputTriggerConfig( config ) );
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
