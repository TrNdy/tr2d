package com.indago.tr2d.tracking;

import java.util.ArrayList;
import java.util.Collection;

import com.indago.tracking.SegmentationProblem;
import com.indago.tracking.map.AssignmentVars;
import com.indago.tracking.seg.ConflictSet;
import com.indago.tracking.seg.SegmentVar;

public class Tr2dSegmentationProblem implements SegmentationProblem {

	int time;
	private final Collection< SegmentVar > segments;
	private final Collection< ConflictSet > conflictSets;
	private final AssignmentVars inAssignments;
	private final AssignmentVars outAssignments;

	public Tr2dSegmentationProblem( final int time ) {
		this.time = time;
		segments = new ArrayList< SegmentVar >();
		conflictSets = new ArrayList< ConflictSet >();
		inAssignments = new AssignmentVars();
		outAssignments = new AssignmentVars();
	}

	/**
	 * @see com.indago.tracking.SegmentationProblem#getTime()
	 */
	@Override
	public int getTime() {
		return time;
	}

	/**
	 * @see com.indago.tracking.SegmentationProblem#getSegments()
	 */
	@Override
	public Collection< SegmentVar > getSegments() {
		return segments;
	}

	/**
	 * @see com.indago.tracking.SegmentationProblem#getConflictSets()
	 */
	@Override
	public Collection< ConflictSet > getConflictSets() {
		return conflictSets;
	}

	/**
	 * @see com.indago.tracking.SegmentationProblem#getInAssignments()
	 */
	@Override
	public AssignmentVars getInAssignments() {
		return inAssignments;
	}

	/**
	 * @see com.indago.tracking.SegmentationProblem#getOutAssignments()
	 */
	@Override
	public AssignmentVars getOutAssignments() {
		return outAssignments;
	}

}