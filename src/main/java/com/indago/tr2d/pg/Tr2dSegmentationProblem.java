package com.indago.tr2d.pg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.indago.costs.CostFactory;
import com.indago.data.segmentation.ConflictGraph;
import com.indago.data.segmentation.LabelingSegment;
import com.indago.pg.SegmentationProblem;
import com.indago.pg.assignments.AssignmentNodes;
import com.indago.pg.segments.ConflictSet;
import com.indago.pg.segments.SegmentNode;
import com.indago.tr2d.pg.levedit.EditState;
import com.indago.util.Bimap;

public class Tr2dSegmentationProblem implements SegmentationProblem {

	private final int time;
	private final CostFactory< LabelingSegment > segmentCosts;

	private final Collection< SegmentNode > segments;
	private final ConflictGraph< LabelingSegment > conflictGraph;
	private final AssignmentNodes inAssignments;
	private final AssignmentNodes outAssignments;

	// LEVERAGED EDITING STATE
	private EditState edits = new EditState();

	private final Bimap< SegmentNode, LabelingSegment > segmentBimap;

	public Tr2dSegmentationProblem(
			final int time,
			final List< LabelingSegment > labelingSegments,
			final CostFactory< LabelingSegment > segmentCosts,
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

	/**
	 * Forces the given SegmentNode to be part of any found solution.
	 * This method is smart enough to avoid obvious problems by removing
	 * all potentially conflicting constraints (on segment and assignment
	 * level).
	 *
	 * @see com.indago.pg.SegmentationProblem#force(com.indago.pg.segments.SegmentNode)
	 */
	@Override
	public void force( final SegmentNode segNode ) {
		forceAndClearConflicts( segNode );
	}

	/**
	 * Forces the given SegmentNode to be part of any found solution my means of
	 * appearing.
	 * This method is smart enough to avoid obvious problems by removing
	 * conflicting constraints.
	 *
	 * @param segNode
	 */
	public void forceAppearance( final SegmentNode segNode ) {
		// to start: un-force all conflicting segment nodes
		forceAndClearConflicts( segNode );
		// add to right force-set
		edits.getForcedSegmentNodeAppearances().add( segNode );
	}

	/**
	 * Forces the given SegmentNode to be part of any found solution my means of
	 * disappearing.
	 * This method is smart enough to avoid obvious problems by removing
	 * conflicting constraints.
	 *
	 * @param segNode
	 */
	public void forceDisappearance( final SegmentNode segNode ) {
		force( segNode );

		// first: un-force disappearance of all conflicting segment nodes
		final Collection< ? extends Collection< LabelingSegment > > cliques = conflictGraph.getConflictGraphCliques();
		for ( final Collection< LabelingSegment > clique : cliques ) {
			if ( clique.contains( segNode ) ) {
				for ( final LabelingSegment labelingSegment : clique ) {
					edits.getForcedSegmentNodeDisappearances().remove( labelingSegment );
				}
			}
		}

		edits.getForcedSegmentNodeDisappearances().add( segNode );
	}

	/**
	 * Forces the given SegmentNode to be part of any found solution my means of
	 * being moved to.
	 * This method is smart enough to avoid obvious problems by removing
	 * conflicting constraints.
	 *
	 * @param segNode
	 */
	public void forceMoveTo( final SegmentNode segNode ) {
		// to start: un-force all conflicting segment nodes
		forceAndClearConflicts( segNode );
		// add to right force-set
		edits.getForcedSegmentNodeMoves().add( segNode );
	}

	/**
	 * Forces the given SegmentNode to be part of any found solution my means of
	 * being divided to.
	 * This method is smart enough to avoid obvious problems by removing
	 * conflicting constraints.
	 *
	 * @param segNode
	 */
	public void forceDivisionTo( final SegmentNode segNode ) {
		// to start: un-force all conflicting segment nodes
		forceAndClearConflicts( segNode );
		// add to right force-set
		edits.getForcedSegmentNodeDivisions().add( segNode );
	}

	/**
	 * Forces the given <code>SegmentNode</code> and removes all forces of
	 * conflicting nodes and (in-)assignments.
	 *
	 * @param segNode
	 */
	private void forceAndClearConflicts( final SegmentNode segNode ) {
		// FORCES ON SEGMENT LEVEL
		// =======================

		// ensure not also being avoided
		edits.getAvoidedSegmentNodes().remove( segNode );

		// un-force all conflicting segment nodes
		Collection< ? extends Collection< LabelingSegment > > cliques = conflictGraph.getConflictGraphCliques();
		for ( final Collection< LabelingSegment > clique : cliques ) {
			if ( clique.contains( segmentBimap.getB( segNode ) ) ) {
				for ( final LabelingSegment labelingSegment : clique ) {
					edits.getForcedSegmentNodes().remove( segmentBimap.getA( labelingSegment ) );
				}
			}
		}

		// force the one!
		edits.getForcedSegmentNodes().add( segNode );

		// FORCES ON ASSIGNMENT LEVEL
		// ==========================
		cliques = conflictGraph.getConflictGraphCliques();
		for ( final Collection< LabelingSegment > clique : cliques ) {
			if ( clique.contains( segmentBimap.getB( segNode ) ) ) {
				for ( final LabelingSegment labelingSegment : clique ) {
					final SegmentNode cliqueSegNode = segmentBimap.getA( labelingSegment );
					edits.getForcedSegmentNodeAppearances().remove( cliqueSegNode );
					edits.getForcedSegmentNodeMoves().remove( cliqueSegNode );
					edits.getForcedSegmentNodeDivisions().remove( cliqueSegNode );
				}
			}
		}
	}

	/**
	 * Avoids the given SegmentNode in any found solution.
	 * This method is smart enough to avoid obvious problems by removing
	 * conflicting constraints.
	 *
	 * @see com.indago.pg.SegmentationProblem#avoid(com.indago.pg.segments.SegmentNode)
	 */
	@Override
	public void avoid( final SegmentNode segNode ) {
		// ensure not also being forced
		edits.getForcedSegmentNodes().remove( segNode );

		// avoid the one!
		edits.getAvoidedSegmentNodes().add( segNode );
	}

	/**
	 * @see com.indago.pg.SegmentationProblem#getForcedNodes()
	 */
	@Override
	public Set< SegmentNode > getForcedNodes() {
		return edits.getForcedSegmentNodes();
	}

	/**
	 * @see com.indago.pg.SegmentationProblem#getAvoidedNodes()
	 */
	@Override
	public Set< SegmentNode > getAvoidedNodes() {
		return edits.getAvoidedSegmentNodes();
	}

	/**
	 * @see com.indago.pg.SegmentationProblem#getForcedByAppearanceNodes()
	 */
	@Override
	public Set< SegmentNode > getForcedByAppearanceNodes() {
		return edits.getForcedSegmentNodeAppearances();
	}

	/**
	 * @see com.indago.pg.SegmentationProblem#getForcedByDisappearanceNodes()
	 */
	@Override
	public Set< SegmentNode > getForcedByDisappearanceNodes() {
		return edits.getForcedSegmentNodeDisappearances();
	}

	/**
	 * @see com.indago.pg.SegmentationProblem#getForcedByMoveNodes()
	 */
	@Override
	public Set< SegmentNode > getForcedByMoveNodes() {
		return edits.getForcedSegmentNodeMoves();
	}

	/**
	 * @see com.indago.pg.SegmentationProblem#getForcedByDivisionNodes()
	 */
	@Override
	public Set< SegmentNode > getForcedByDivisionNodes() {
		return edits.getForcedSegmentNodeDivisions();
	}

	/**
	 * @return the current EditState
	 */
	public EditState getEditState() {
		return edits;
	}

	/**
	 * @param state
	 *            the EditState to be set
	 */
	public void setEditState( final EditState state ) {
		this.edits = state;
	}
}