package com.indago.app.selectsegment;

import java.util.HashSet;

import net.trackmate.graph.GraphChangeListener;
import net.trackmate.graph.GraphIdBimap;
import net.trackmate.graph.GraphListener;
import net.trackmate.graph.ListenableReadOnlyGraph;
import net.trackmate.graph.object.AbstractObjectGraph;

public class SegmentGraph
		extends AbstractObjectGraph< SegmentVertex, SubsetEdge >
		implements ListenableReadOnlyGraph< SegmentVertex, SubsetEdge > {

	private final GraphIdBimap< SegmentVertex, SubsetEdge > idmap;

	public SegmentGraph() {
		super( new Factory(), new HashSet< >(), new HashSet< >() );
		idmap = new GraphIdBimap<>( new HashBimap<>( SegmentVertex.class ), new HashBimap<>( SubsetEdge.class ) );
	}

	public GraphIdBimap< SegmentVertex, SubsetEdge > getGraphIdBimap() {
		return idmap;
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
}
