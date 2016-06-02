package com.indago.app.selectsegment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import com.indago.data.segmentation.LabelData;
import com.indago.data.segmentation.LabelingPlus;
import com.indago.data.segmentation.LabelingSegment;
import com.indago.data.segmentation.XmlIoLabelingPlus;

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

		final String folder = "/Users/pietzsch/Desktop/data/tr2d/tr2d_project_folder/DebugStack03-crop/tracking/labeling_frames/";
		final String fLabeling = folder + "labeling_frame0000.xml";

		final LabelingPlus labelingPlus = new XmlIoLabelingPlus().load( fLabeling );

		final LabelingSegment labelingSegment = labelingPlus.getSegments().get( 0 );
		labelingPlus.getFragments();
	}
}
