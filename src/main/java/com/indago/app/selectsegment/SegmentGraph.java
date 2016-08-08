package com.indago.app.selectsegment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.indago.data.segmentation.LabelData;

import net.trackmate.graph.GraphChangeListener;
import net.trackmate.graph.GraphIdBimap;
import net.trackmate.graph.GraphListener;
import net.trackmate.graph.ListenableReadOnlyGraph;
import net.trackmate.graph.object.AbstractObjectGraph;

public class SegmentGraph
		extends AbstractObjectGraph< SegmentVertex, SubsetEdge >
		implements ListenableReadOnlyGraph< SegmentVertex, SubsetEdge > {

	private final GraphIdBimap< SegmentVertex, SubsetEdge > idmap;

	final Map< LabelData, SegmentVertex > mapVertices;

	public SegmentGraph() {
		this( new Factory() );
	}

	private SegmentGraph( final Factory factory ) {
		super( factory, new HashSet<>(), new HashSet<>() );
		factory.setGraph( this );
		idmap = new GraphIdBimap<>( new HashBimap<>( SegmentVertex.class ), new HashBimap<>( SubsetEdge.class ) );
		mapVertices = new HashMap<>();
	}

	public GraphIdBimap< SegmentVertex, SubsetEdge > getGraphIdBimap() {
		return idmap;
	}

	public SegmentVertex getVertexForLabel( final LabelData label ) {
		return mapVertices.get( label );
	}

	@Override
	public void remove( final SegmentVertex vertex ) {
		super.remove( vertex );
		mapVertices.remove( vertex.getLabelData() );
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

		private SegmentGraph graph;

		public void setGraph( final SegmentGraph graph )
		{
			this.graph = graph;
		}

		@Override
		public SegmentVertex createVertex() {
			return new SegmentVertex( graph );
		}

		@Override
		public SubsetEdge createEdge( final SegmentVertex source, final SegmentVertex target ) {
			return new SubsetEdge( source, target );
		}
	}
}
