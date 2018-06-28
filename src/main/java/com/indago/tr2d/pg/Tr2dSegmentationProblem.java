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

	public ConflictSet getConflictSetFor( final SegmentNode node ) {
		for ( final Collection< LabelingSegment > clique : conflictGraph.getConflictGraphCliques() ) {
			if ( clique.contains( node.getSegment() ) ) {
				final ConflictSet cs = new ConflictSet();
				for ( final LabelingSegment ls : clique ) {
					cs.add( segmentBimap.getA( ls ) );
				}
				return cs;
			}
		}
		return new ConflictSet();
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
	 *            SegmentNode instance
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
	 *            SegmentNode instance
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
	 * being moved to from the past.
	 * This method is smart enough to avoid obvious problems by removing
	 * conflicting constraints.
	 *
	 * @param segNode
	 *            SegmentNode instance
	 */
	public void forceMoveTo( final SegmentNode segNode ) {
		// to start: un-force all conflicting segment nodes
		forceAndClearConflicts( segNode );
		// add to right force-set
		edits.getForcedSegmentNodeMovesTo().add( segNode );
	}

	/**
	 * Forces that a member of the given ConflictSet will be part of any found
	 * solution my means of finding a movement towards any member from the past.
	 * This method is smart enough to avoid obvious problems by removing
	 * conflicting constraints.
	 *
	 * @param confSet
	 *            ConflictSet instance
	 */
	public void forceMoveTo( final ConflictSet confSet ) {
		// to start: un-force all conflicting segment nodes
		forceAndClearConflicts( confSet, true, false );
		// add to right force-set
		edits.getForcedConflictSetMovesTo().add( confSet );
	}

	/**
	 * Forces the given SegmentNode to be part of any found solution my means of
	 * being moved from it into the future.
	 * This method is smart enough to avoid obvious problems by removing
	 * conflicting constraints.
	 *
	 * @param segNode
	 *            SegmentNode instance
	 */
	public void forceMoveFrom( final SegmentNode segNode ) {
		// to start: un-force all conflicting segment nodes
		forceAndClearConflicts( segNode );
		// add to right force-set
		edits.getForcedSegmentNodeMovesFrom().add( segNode );
	}

	/**
	 * Forces that a member of the given ConflictSet will be part of any found
	 * solution my means of finding a division from any member towards the
	 * future.
	 * This method is smart enough to avoid obvious problems by removing
	 * conflicting constraints.
	 *
	 * @param confSet
	 *            ConflictSet instance
	 */
	public void forceMoveFrom( final ConflictSet confSet ) {
		// to start: un-force all conflicting segment nodes
		forceAndClearConflicts( confSet, false, true );
		// add to right force-set
		edits.getForcedConflictSetMovesFrom().add( confSet );
	}

	/**
	 * Forces the given SegmentNode to be part of any found solution my means of
	 * being divided to from the past.
	 * This method is smart enough to avoid obvious problems by removing
	 * conflicting constraints.
	 *
	 * @param segNode
	 *            SegmentNode instance
	 */
	public void forceDivisionTo( final SegmentNode segNode ) {
		// to start: un-force all conflicting segment nodes
		forceAndClearConflicts( segNode );
		// add to right force-set
		edits.getForcedSegmentNodeDivisionsTo().add( segNode );
	}

	/**
	 * Forces that a member of the given ConflictSet will be part of any found
	 * solution my means of finding a division towards any member from the past.
	 * This method is smart enough to avoid obvious problems by removing
	 * conflicting constraints.
	 *
	 * @param confSet
	 *            ConflictSet instance
	 */
	public void forceDivisionTo( final ConflictSet confSet ) {
		// to start: un-force all conflicting segment nodes
		forceAndClearConflicts( confSet, true, false );
		// add to right force-set
		edits.getForcedConflictSetDivisionsTo().add( confSet );
	}

	/**
	 * Forces that a member of the given ConflictSet will be part of any found
	 * solution my means of finding a division from any member towards the
	 * future.
	 * This method is smart enough to avoid obvious problems by removing
	 * conflicting constraints.
	 *
	 * @param confSet
	 *            ConflictSet instance
	 */
	public void forceDivisionFrom( final ConflictSet confSet ) {
		// to start: un-force all conflicting segment nodes
		forceAndClearConflicts( confSet, false, true );
		// add to right force-set
		edits.getForcedConflictSetDivisionsFrom().add( confSet );
	}

	/**
	 * Forces the given SegmentNode to be part of any found solution my means of
	 * being divided into the future.
	 * This method is smart enough to avoid obvious problems by removing
	 * conflicting constraints.
	 *
	 * @param segNode
	 *            SegmentNode instance
	 */
	public void forceDivisionFrom( final SegmentNode segNode ) {
		// to start: un-force all conflicting segment nodes
		forceAndClearConflicts( segNode );
		// add to right force-set
		edits.getForcedSegmentNodeDivisionsFrom().add( segNode );
	}

	/**
	 * Forces the given <code>SegmentNode</code> and removes all forces of
	 * conflicting nodes and (in-)assignments.
	 *
	 * @param segNode
	 *            SegmentNode instance
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
					edits.getForcedSegmentNodeMovesTo().remove( cliqueSegNode );
					edits.getForcedSegmentNodeDivisionsTo().remove( cliqueSegNode );
					edits.getForcedSegmentNodeMovesFrom().remove( cliqueSegNode );
					edits.getForcedSegmentNodeDivisionsFrom().remove( cliqueSegNode );
				}
			}
		}
	}

	/**
	 * Forces a member <code>ConflictSet</code> to be active and removes all
	 * individual forces of nodes in the given conflict set.
	 *
	 * @param confSet
	 *            ConflictSet instance
	 * @param removeSegmentInAssignments
	 *            guess
	 * @param removeSegmentOutAssignments
	 *            guess
	 */
	private void forceAndClearConflicts(
			final ConflictSet confSet,
			final boolean removeSegmentInAssignments,
			final boolean removeSegmentOutAssignments ) {

		for ( final SegmentNode node : confSet ) {
			edits.getAvoidedSegmentNodes().remove( node );
			edits.getForcedSegmentNodeAppearances().remove( node );

			if ( removeSegmentInAssignments ) {
				edits.getForcedSegmentNodeMovesTo().remove( node );
				edits.getForcedSegmentNodeDivisionsTo().remove( node );
			}
			if ( removeSegmentOutAssignments ) {
				edits.getForcedSegmentNodeMovesFrom().remove( node );
				edits.getForcedSegmentNodeDivisionsFrom().remove( node );
			}
		}
	}

	/**
	 * Avoids the given SegmentNode in any found solution.
	 * This method is smart enough to avoid obvious problems by removing
	 * conflicting constraints.
	 * 
	 * @param segNode
	 *            SegmentNode instance
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
	 * @see com.indago.pg.SegmentationProblem#getForcedSegmentNodeMovesTo()
	 */
	@Override
	public Set< SegmentNode > getForcedSegmentNodeMovesTo() {
		return edits.getForcedSegmentNodeMovesTo();
	}

	/**
	 * @see com.indago.pg.SegmentationProblem#getForcedSegmentNodeMovesFrom()
	 */
	@Override
	public Set< SegmentNode > getForcedSegmentNodeMovesFrom() {
		return edits.getForcedSegmentNodeMovesFrom();
	}

	/**
	 * @see com.indago.pg.SegmentationProblem#getForcedSegmentNodeDivisionsTo()
	 */
	@Override
	public Set< SegmentNode > getForcedSegmentNodeDivisionsTo() {
		return edits.getForcedSegmentNodeDivisionsTo();
	}

	/**
	 * @see com.indago.pg.SegmentationProblem#getForcedSegmentNodeDivisionsFrom()
	 */
	@Override
	public Set< SegmentNode > getForcedSegmentNodeDivisionsFrom() {
		return edits.getForcedSegmentNodeDivisionsTo();
	}

	/**
	 * @see com.indago.pg.SegmentationProblem#getForcedConflictSetMovesTo()
	 */
	@Override
	public Set< ConflictSet > getForcedConflictSetMovesTo() {
		return edits.getForcedConflictSetMovesTo();
	}

	/**
	 * @see com.indago.pg.SegmentationProblem#getForcedConflictSetMovesFrom()
	 */
	@Override
	public Set< ConflictSet > getForcedConflictSetMovesFrom() {
		return edits.getForcedConflictSetMovesFrom();
	}

	/**
	 * @see com.indago.pg.SegmentationProblem#getForcedConflictSetDivisionsTo()
	 */
	@Override
	public Set< ConflictSet > getForcedConflictSetDivisionsTo() {
		return edits.getForcedConflictSetDivisionsTo();
	}

	/**
	 * @see com.indago.pg.SegmentationProblem#getForcedConflictSetDivisionsFrom()
	 */
	@Override
	public Set< ConflictSet > getForcedConflictSetDivisionsFrom() {
		return edits.getForcedConflictSetDivisionsFrom();
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