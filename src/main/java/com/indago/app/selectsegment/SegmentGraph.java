package com.indago.app.selectsegment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.indago.data.segmentation.LabelData;
import com.indago.data.segmentation.LabelingFragment;
import com.indago.data.segmentation.LabelingPlus;
import com.indago.data.segmentation.XmlIoLabelingPlus;

import net.trackmate.collection.RefList;
import net.trackmate.graph.algorithm.ShortestPath;
import net.trackmate.graph.algorithm.traversal.BreadthFirstIterator;
import net.trackmate.graph.algorithm.traversal.InverseBreadthFirstIterator;
import net.trackmate.graph.object.AbstractObjectEdge;
import net.trackmate.graph.object.AbstractObjectGraph;
import net.trackmate.graph.object.AbstractObjectVertex;
import net.trackmate.spatial.HasTimepoint;

public class SegmentGraph
//implements ListenableReadOnlyGraph< V, E > modelGraph
//< V extends Vertex< E > & HasTimepoint,
//E extends Edge< V > >
{

	// source ⊇ target
	static class SubsetEdge extends AbstractObjectEdge< SubsetEdge, SegmentVertex >
	{
		protected SubsetEdge( final SegmentVertex source, final SegmentVertex target )
		{
			super( source, target );
		}
	}

	static class SegmentVertex extends AbstractObjectVertex< SegmentVertex, SubsetEdge > implements HasTimepoint
	{
		private LabelData label;

		private int timepoint;

		public SegmentVertex init( final LabelData label )
		{
			this.label = label;
			this.timepoint = 0;
			return this;
		}

		@Override
		public int getTimepoint() {
			return timepoint;
		}

		public LabelData getLabel()
		{
			return label;
		}
	}

	static class SegmentGraphX extends AbstractObjectGraph< SegmentVertex, SubsetEdge >
	{
		public SegmentGraphX()
		{
			super( new Factory(), new HashSet<>(), new HashSet<>() );
		}

		private static class Factory implements AbstractObjectGraph.Factory< SegmentVertex, SubsetEdge >
		{
			@Override
			public SegmentVertex createVertex()
			{
				return new SegmentVertex();
			}

			@Override
			public SubsetEdge createEdge( final SegmentVertex source, final SegmentVertex target )
			{
				return new SubsetEdge( source, target );
			}
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

	public static void main( final String[] args ) throws IOException {

//		final String folder = "/Users/pietzsch/Desktop/data/tr2d/tr2d_project_folder/DebugStack03-crop/tracking/labeling_frames/";
		final String folder = "/Users/jug/MPI/ProjectHernan/Tr2dProjectPath/DebugStack03-crop/tracking/labeling_frames/";

		final String fLabeling = folder + "labeling_frame0000.xml";

		final LabelingPlus labelingPlus = new XmlIoLabelingPlus().load( fLabeling );

		final SegmentGraphX graph = new SegmentGraphX();
		final ShortestPath< SegmentVertex, SubsetEdge > sp = new ShortestPath<>( graph, true );

		final Map< LabelData, SegmentVertex > mapVertices = new HashMap<>();

		// Build partial order graph
		final Iterator< LabelingFragment > fragmentIterator = labelingPlus.getFragments().iterator();
		while ( fragmentIterator.hasNext() ) {
			final LabelingFragment fragment = fragmentIterator.next();
			final ArrayList< LabelData > conflictingSegments = fragment.getSegments();

			for ( final LabelData segment : conflictingSegments ) {

				// add new vertices to graph
				if ( !mapVertices.containsKey( segment ) ) {
					final SegmentVertex newVertex = graph.addVertex().init( segment );
					mapVertices.put( segment, newVertex );

					// connect regarding subset relation (while removing transitive edges)
					for ( final LabelData conflictingSegment : conflictingSegments ) {
						if ( segment.equals( conflictingSegment ) ) continue;
						if ( !mapVertices.containsKey( conflictingSegment ) ) continue;

						final SegmentVertex subsetVertex = mapVertices.get( conflictingSegment );

						// segment < conflictingSegment  (other direction happens in a later iteration)
						if ( isSubset( segment, conflictingSegment ) ) {
							final RefList< SegmentVertex > inversePath = sp.findPath( newVertex, subsetVertex );
							if ( inversePath == null ) {
								// no path exists --> add edge
								graph.addEdge( newVertex, subsetVertex );
							} else {
								// path exists --> add edge, but remove all edges from ancestors to descendants

								final BreadthFirstIterator< SegmentVertex, SubsetEdge > bfi =
										new BreadthFirstIterator<>( subsetVertex, graph );
								final Set< SegmentVertex > descendants = new HashSet<>();
								while ( bfi.hasNext() ) {
									descendants.add( bfi.next() );
								}

								final InverseBreadthFirstIterator< SegmentVertex, SubsetEdge > ibfi =
										new InverseBreadthFirstIterator<>( newVertex, graph );
								final Set< SegmentVertex > ancestors = new HashSet<>();
								while ( ibfi.hasNext() ) {
									ancestors.add( ibfi.next() );
								}

								// for all edges leaving any vertex in ancestors: delete if target is within descendants
								for ( final SegmentVertex a : ancestors ) {
									for ( final SubsetEdge edge : a.outgoingEdges() ) {
										if ( descendants.contains( edge.getTarget() ) ) {
											graph.remove( edge );
										}
									}
								}
							}
						}
					}
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
