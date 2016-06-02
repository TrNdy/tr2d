package com.indago.app.selectsegment;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.scijava.ui.behaviour.MouseAndKeyHandler;
import org.scijava.ui.behaviour.io.InputTriggerConfig;

import com.indago.data.segmentation.LabelData;

import bdv.BehaviourTransformEventHandler;
import bdv.viewer.InputActionBindings;
import bdv.viewer.TriggerBehaviourBindings;
import net.imglib2.ui.TransformEventHandler;
import net.imglib2.ui.util.GuiUtil;
import net.trackmate.graph.GraphChangeListener;
import net.trackmate.graph.GraphIdBimap;
import net.trackmate.graph.GraphListener;
import net.trackmate.graph.ListenableReadOnlyGraph;
import net.trackmate.graph.object.AbstractObjectEdge;
import net.trackmate.graph.object.AbstractObjectGraph;
import net.trackmate.graph.object.AbstractObjectVertex;
import net.trackmate.revised.model.HasLabel;
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
import net.trackmate.spatial.HasTimepoint;

public class SegmentGraph
//implements ListenableReadOnlyGraph< V, E > modelGraph
//< V extends Vertex< E > & HasTimepoint,
//E extends Edge< V > >
{

	// source ⊇ target
	static class SubsetEdge extends AbstractObjectEdge< SubsetEdge, SegmentVertex > {

		protected SubsetEdge( final SegmentVertex source, final SegmentVertex target ) {
			super( source, target );
		}
	}

	static class SegmentVertex extends AbstractObjectVertex< SegmentVertex, SubsetEdge > implements HasTimepoint, HasLabel {

		private LabelData labelData;

		private int timepoint;

		public SegmentVertex init( final LabelData labelData ) {
			this.labelData = labelData;
			this.timepoint = 0;
			return this;
		}

		public LabelData getLabelData() {
			return labelData;
		}

		@Override
		public int getTimepoint() {
			return timepoint;
		}

		@Override
		public String getLabel() {
			return "dummy";
		}

		@Override
		public void setLabel( final String label ) {}
	}

	static class SegmentGraphX
			extends AbstractObjectGraph< SegmentVertex, SubsetEdge >
			implements ListenableReadOnlyGraph< SegmentVertex, SubsetEdge > {

		public SegmentGraphX() {
			super( new Factory(), new HashSet<>(), new HashSet<>() );
		}

		private static class Factory implements AbstractObjectGraph.Factory< SegmentVertex, SubsetEdge > {

			@Override
			public SegmentVertex createVertex() {
				return new SegmentVertex();
			}

			@Override
			public SubsetEdge createEdge( final SegmentVertex source, final SegmentVertex target ) {
				return new SubsetEdge( source, target );
			}
		}

		@Override
		public boolean addGraphListener( final GraphListener< SegmentVertex, SubsetEdge > listener ) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean removeGraphListener( final GraphListener< SegmentVertex, SubsetEdge > listener ) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean addGraphChangeListener( final GraphChangeListener listener ) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean removeGraphChangeListener( final GraphChangeListener listener ) {
			// TODO Auto-generated method stub
			return false;
		}
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

	static void wrap( final SegmentGraphX modelGraph ) {
		final GroupManager manager = new GroupManager();
		final GroupHandle trackSchemeGroupHandle = manager.createGroupHandle();
		final TrackSchemeOptions optional = TrackSchemeOptions.options();

		final GraphIdBimap< SegmentVertex, SubsetEdge > idmap =
				new GraphIdBimap<>( new HashBimap<>( SegmentVertex.class ), new HashBimap<>( SubsetEdge.class ) );

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

		trackschemePanel.setTimepointRange( 0, 1 );
		trackschemePanel.graphChanged();
		frame.setVisible( true );
	}

	private static InputTriggerConfig getKeyConfig( final TrackSchemeOptions optional )
	{
		final InputTriggerConfig conf = optional.values.getInputTriggerConfig();
		return conf != null ? conf : new InputTriggerConfig();
	}

	public static void main( final String[] args ) throws IOException {

//		final String folder = "/Users/pietzsch/Desktop/data/tr2d/tr2d_project_folder/DebugStack03-crop/tracking/labeling_frames/";
//		final String fLabeling = folder + "labeling_frame0000.xml";
//
//		final LabelingPlus labelingPlus = new XmlIoLabelingPlus().load( fLabeling );
//
//		final LabelingSegment labelingSegment = labelingPlus.getSegments().get( 0 );
//		labelingPlus.getFragments();
//
//		final LabelData ld = labelingPlus.getLabeling().getMapping().getLabels().iterator().next();
//		ld.getSegment();
//
//		final LabelingFragment fragment = labelingPlus.getFragments().iterator().next();
//		fragment.getSegments();

		final SegmentGraphX graph = new SegmentGraphX();
		final SegmentVertex vertex = graph.addVertex().init( null );
		final SegmentVertex vertex2 = graph.addVertex().init( null );
		final SegmentVertex vertex3 = graph.addVertex().init( null );
		graph.addEdge( vertex, vertex2 );
		graph.addEdge( vertex, vertex3 );

		vertex.timepoint = 0;
		vertex2.timepoint = 1;
		vertex3.timepoint = 1;

		wrap( graph );
	}
}
