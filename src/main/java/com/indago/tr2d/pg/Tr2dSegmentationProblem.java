package com.indago.tr2d.pg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

	private final Set< SegmentNode > forcedSegmentNodes = new HashSet<>();
	private final Set< SegmentNode > avoidedSegmentNodes = new HashSet<>();

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
//		System.out.println( "==(t,#)==>>>> " + time + ", " + labelingSegments.size() );
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
	 * conflicting constraints.
	 *
	 * @see com.indago.pg.SegmentationProblem#force(com.indago.pg.segments.SegmentNode)
	 */
	@Override
	public void force( final SegmentNode segNode ) {
		// ensure not also being avoided
		avoidedSegmentNodes.remove( segNode );

		// unforce all conflicting segment nodes
		final Collection< ? extends Collection< LabelingSegment > > cliques = conflictGraph.getConflictGraphCliques();
		for ( final Collection< LabelingSegment > clique : cliques ) {
			if ( clique.contains( segNode ) ) {
				for ( final LabelingSegment labelingSegment : clique ) {
					forcedSegmentNodes.remove( labelingSegment );
				}
			}
		}

		// force the one!
		forcedSegmentNodes.add( segNode );
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
		forcedSegmentNodes.remove( segNode );

		// unavoid all conflicting segment nodes
		final Collection< ? extends Collection< LabelingSegment > > cliques = conflictGraph.getConflictGraphCliques();
		for ( final Collection< LabelingSegment > clique : cliques ) {
			if ( clique.contains( segNode ) ) {
				for ( final LabelingSegment labelingSegment : clique ) {
					avoidedSegmentNodes.remove( labelingSegment );
				}
			}
		}

		// avoid the one!
		avoidedSegmentNodes.add( segNode );
	}

	/**
	 * @see com.indago.pg.SegmentationProblem#getForcedNodes()
	 */
	@Override
	public Set< SegmentNode > getForcedNodes() {
		return forcedSegmentNodes;
	}

	/**
	 * @see com.indago.pg.SegmentationProblem#getAvoidedNodes()
	 */
	@Override
	public Set< SegmentNode > getAvoidedNodes() {
		return avoidedSegmentNodes;
	}
}