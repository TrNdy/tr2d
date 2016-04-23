package com.indago.tr2d.pg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.indago.data.segmentation.ConflictGraph;
import com.indago.data.segmentation.LabelingSegment;
import com.indago.old_fg.CostsFactory;
import com.indago.pg.SegmentationProblem;
import com.indago.pg.assignments.AssignmentNodes;
import com.indago.pg.segments.ConflictSet;
import com.indago.pg.segments.SegmentNode;
import com.indago.util.Bimap;

public class Tr2dSegmentationProblem implements SegmentationProblem {

	private final int time;
	private final CostsFactory< LabelingSegment > segmentCosts;

	private final Collection< SegmentNode > segments;
	private final ConflictGraph< LabelingSegment > conflictGraph;
	private final AssignmentNodes inAssignments;
	private final AssignmentNodes outAssignments;

	private final Bimap< SegmentNode, LabelingSegment > segmentBimap;

	public Tr2dSegmentationProblem(
			final int time,
			final List< LabelingSegment > labelingSegments,
			final CostsFactory< LabelingSegment > segmentCosts,
			final ConflictGraph< LabelingSegment > conflictGraph ) {
		inAssignments = new AssignmentNodes();
		outAssignments = new AssignmentNodes();
		segmentBimap = new Bimap< >();

		this.time = time;
		this.segmentCosts = segmentCosts;
		this.conflictGraph = conflictGraph;

		segments = new ArrayList< SegmentNode >();
		createSegmentVars( labelingSegments );
	}

	/**
	 * @param labelingSegments
	 * @param segmentCosts
	 */
	private void createSegmentVars( final List< LabelingSegment > labelingSegments ) {
		for ( final LabelingSegment labelingSegment : labelingSegments ) {
			final SegmentNode segVar =
					new SegmentNode( labelingSegment, segmentCosts.getCost( labelingSegment ) );
			segments.add( segVar );
			segmentBimap.add( segVar, labelingSegment );
		}
	}

	/**
	 * @see com.indago.pg.SegmentationProblem#getTime()
	 */
	@Override
	public int getTime() {
		return time;
	}

	/**
	 * @see com.indago.pg.SegmentationProblem#getSegments()
	 */
	@Override
	public Collection< SegmentNode > getSegments() {
		return segments;
	}

	/**
	 * @see com.indago.pg.SegmentationProblem#getConflictSets()
	 */
	@Override
	public Collection< ConflictSet > getConflictSets() {
		final ArrayList< ConflictSet > ret = new ArrayList< ConflictSet >();
		for ( final Collection< LabelingSegment > clique : conflictGraph.getConflictGraphCliques() ) {
			final ConflictSet cs = new ConflictSet();
			for ( final LabelingSegment ls : clique ) {
				cs.add( segmentBimap.getA( ls ) );
			}
			ret.add( cs );
		}
		return ret;
	}

	public SegmentNode getSegmentVar( final LabelingSegment segment ) {
		return segmentBimap.getA( segment );
	}

	public LabelingSegment getLabelingSegment( final SegmentNode segment ) {
		return segmentBimap.getB( segment );
	}
}