package com.indago.demos.selectsegment;

import org.mastodon.graph.object.AbstractObjectVertex;
import org.mastodon.revised.model.HasLabel;
import org.mastodon.spatial.HasTimepoint;

import com.indago.data.segmentation.LabelData;


public class SegmentVertex extends AbstractObjectVertex< SegmentVertex, SubsetEdge > implements HasTimepoint, HasLabel {

	private final SegmentGraph graph;

	private LabelData labelData;

	private int timepoint;

	SegmentVertex( final SegmentGraph graph ) {
		this.graph = graph;
	}

	public SegmentVertex init( final LabelData labelData ) {
		this.labelData = labelData;
		this.timepoint = -1;
		graph.mapVertices.put( labelData, this );
		return this;
	}

	public LabelData getLabelData() {
		return labelData;
	}

	@Override
	public int getTimepoint() {
		return timepoint;
	}

	public void setTimepoint( final int timepoint ) {
		this.timepoint = timepoint;
	}

	@Override
	public String getLabel() {
		return toString();
	}

	@Override
	public void setLabel( final String label ) {}

	@Override
	public String toString() {
		return "v" + labelData.getId() + " t" + timepoint;
	}
}
