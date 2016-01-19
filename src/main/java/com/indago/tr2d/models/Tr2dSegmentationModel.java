package com.indago.tr2d.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.indago.data.segmentation.ConflictGraph;
import com.indago.data.segmentation.LabelingSegment;
import com.indago.fg.CostsFactory;
import com.indago.models.SegmentationModel;
import com.indago.models.assignments.AssignmentVars;
import com.indago.models.segments.ConflictSet;
import com.indago.models.segments.SegmentVar;
import com.indago.util.Bimap;

public class Tr2dSegmentationModel implements SegmentationModel {

	private final int time;
	private final CostsFactory< LabelingSegment > segmentCosts;

	private final Collection< SegmentVar > segments;
	private final ConflictGraph< LabelingSegment > conflictGraph;
	private final AssignmentVars inAssignments;
	private final AssignmentVars outAssignments;

	private final Bimap< SegmentVar, LabelingSegment > segmentBimap;

	public Tr2dSegmentationModel(
			final int time,
			final List< LabelingSegment > labelingSegments,
			final CostsFactory< LabelingSegment > segmentCosts,
			final ConflictGraph< LabelingSegment > conflictGraph ) {
		inAssignments = new AssignmentVars();
		outAssignments = new AssignmentVars();
		segmentBimap = new Bimap< >();

		this.time = time;
		this.segmentCosts = segmentCosts;
		this.conflictGraph = conflictGraph;

		segments = new ArrayList< SegmentVar >();
		createSegmentVars( labelingSegments );
	}

	/**
	 * @param labelingSegments
	 * @param segmentCosts
	 */
	private void createSegmentVars( final List< LabelingSegment > labelingSegments ) {
		for ( final LabelingSegment labelingSegment : labelingSegments ) {
			final SegmentVar segVar =
					new SegmentVar( labelingSegment, segmentCosts.getCost( labelingSegment ) );
			segments.add( segVar );
			segmentBimap.add( segVar, labelingSegment );
		}
	}

	/**
	 * @see com.indago.models.SegmentationModel#getTime()
	 */
	@Override
	public int getTime() {
		return time;
	}

	/**
	 * @see com.indago.models.SegmentationModel#getSegments()
	 */
	@Override
	public Collection< SegmentVar > getSegments() {
		return segments;
	}

	/**
	 * @see com.indago.models.SegmentationModel#getConflictSets()
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

	/**
	 * @see com.indago.models.SegmentationModel#getInAssignments()
	 */
	@Override
	public AssignmentVars getInAssignments() {
		return inAssignments;
	}

	/**
	 * @see com.indago.models.SegmentationModel#getOutAssignments()
	 */
	@Override
	public AssignmentVars getOutAssignments() {
		return outAssignments;
	}

	public SegmentVar getSegmentVar( final LabelingSegment segment ) {
		return segmentBimap.getA( segment );
	}

	public LabelingSegment getLabelingSegment( final SegmentVar segment ) {
		return segmentBimap.getB( segment );
	}
}