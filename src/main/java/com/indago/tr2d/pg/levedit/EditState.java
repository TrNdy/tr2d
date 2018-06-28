/**
 *
 */
package com.indago.tr2d.pg.levedit;

import java.util.HashSet;
import java.util.Set;

import com.indago.pg.segments.ConflictSet;
import com.indago.pg.segments.SegmentNode;

/**
 * @author jug
 */
public class EditState {

	protected final Set< SegmentNode > forcedSegmentNodes = new HashSet<>();
	protected final Set< SegmentNode > forcedSegmentNodeAppearances = new HashSet<>();
	protected final Set< SegmentNode > forcedSegmentNodeDisappearances = new HashSet<>();
	protected final Set< SegmentNode > forcedSegmentNodeMovesTo = new HashSet<>();
	protected final Set< SegmentNode > forcedSegmentNodeDivisionsTo = new HashSet<>();
	protected final Set< SegmentNode > forcedSegmentNodeMovesFrom = new HashSet<>();
	protected final Set< SegmentNode > forcedSegmentNodeDivisionsFrom = new HashSet<>();
	protected final Set< SegmentNode > avoidedSegmentNodes = new HashSet<>();

	private final Set< ConflictSet > forcedConflictSetDivisionsTo = new HashSet<>();
	private final Set< ConflictSet > forcedConflictSetDivisionsFrom = new HashSet<>();
	private final Set< ConflictSet > forcedConflictSetMovesTo = new HashSet<>();
	private final Set< ConflictSet > forcedConflictSetMovesFrom = new HashSet<>();

	public EditState() {}

	public EditState( final EditState state ) {
		this.forcedSegmentNodes.addAll( state.forcedSegmentNodes );
		this.forcedSegmentNodeAppearances.addAll( state.forcedSegmentNodeAppearances );
		this.forcedSegmentNodeDisappearances.addAll( state.forcedSegmentNodeDisappearances );

		this.forcedSegmentNodeMovesTo.addAll( state.forcedSegmentNodeMovesTo );
		this.forcedSegmentNodeDivisionsTo.addAll( state.forcedSegmentNodeDivisionsFrom );
		this.forcedSegmentNodeMovesFrom.addAll( state.forcedSegmentNodeMovesTo );
		this.forcedSegmentNodeDivisionsFrom.addAll( state.forcedSegmentNodeDivisionsFrom );

		this.forcedConflictSetDivisionsTo.addAll( state.forcedConflictSetDivisionsTo );
		this.forcedConflictSetDivisionsFrom.addAll( state.forcedConflictSetDivisionsFrom );
		this.forcedConflictSetMovesTo.addAll( state.forcedConflictSetMovesTo );
		this.forcedConflictSetMovesFrom.addAll( state.forcedConflictSetMovesFrom );

		this.avoidedSegmentNodes.addAll( state.avoidedSegmentNodes );
	}

	public Set< SegmentNode > getForcedSegmentNodes() {
		return forcedSegmentNodes;
	}

	public Set< SegmentNode > getForcedSegmentNodeAppearances() {
		return forcedSegmentNodeAppearances;
	}

	public Set< SegmentNode > getForcedSegmentNodeDisappearances() {
		return forcedSegmentNodeDisappearances;
	}

	public Set< SegmentNode > getForcedSegmentNodeMovesTo() {
		return forcedSegmentNodeMovesTo;
	}

	public Set< SegmentNode > getForcedSegmentNodeMovesFrom() {
		return forcedSegmentNodeMovesFrom;
	}

	public Set< SegmentNode > getForcedSegmentNodeDivisionsTo() {
		return forcedSegmentNodeDivisionsTo;
	}

	public Set< SegmentNode > getForcedSegmentNodeDivisionsFrom() {
		return forcedSegmentNodeDivisionsFrom;
	}

	public Set< SegmentNode > getAvoidedSegmentNodes() {
		return avoidedSegmentNodes;
	}

	public Set< ConflictSet > getForcedConflictSetMovesTo() {
		return forcedConflictSetMovesTo;
	}

	public Set< ConflictSet > getForcedConflictSetMovesFrom() {
		return forcedConflictSetMovesFrom;
	}

	public Set< ConflictSet > getForcedConflictSetDivisionsTo() {
		return forcedConflictSetDivisionsTo;
	}

	public Set< ConflictSet > getForcedConflictSetDivisionsFrom() {
		return forcedConflictSetDivisionsFrom;
	}

	/**
	 * @return A string showing how many edits are stored per type.
	 */
	public String getDebugString() {
		return String.format(
				"Number of edits stored: %d,%d,%d,%d,%d,%d",
				forcedSegmentNodes.size(),
				forcedSegmentNodeAppearances.size(),
				forcedSegmentNodeDisappearances.size(),
				forcedSegmentNodeMovesTo.size(),
				forcedSegmentNodeDivisionsTo.size(),
				avoidedSegmentNodes.size() );
	}

	/**
	 * @param segnode
	 *            the segnode to perform the action on
	 * @return true, iff the given segnode represents an avoided segment
	 */
	public boolean isAvoided( final SegmentNode segnode ) {
		return avoidedSegmentNodes.contains( segnode );
	}

	/**
	 * @param segnode
	 *            the segnode to perform the action on
	 * @return true, iff the given segnode represents a forced segment
	 */
	public boolean isForced( final SegmentNode segnode ) {
		return forcedSegmentNodes.contains( segnode );
	}

	/**
	 * @param segnode
	 *            the segnode to perform the action on
	 * @return true, iff the given segnode has movement forced towards it
	 *         or from a conflict set force it is contained in
	 */
	public boolean isMoveForcedTo( final SegmentNode segnode ) {
		boolean ret = forcedSegmentNodeMovesTo.contains( segnode );
		for ( final ConflictSet confset : getForcedConflictSetMovesTo() ) {
			ret |= confset.contains( segnode );
		}
		return ret;
	}

	/**
	 * @param segnode
	 *            the segnode to perform the action on
	 * @return true, iff the given segnode has movement forced away from it
	 *         or from a conflict set force it is contained in (towards future)
	 */
	public boolean isMoveForcedFrom( final SegmentNode segnode ) {
		boolean ret = forcedSegmentNodeMovesFrom.contains( segnode );
		for ( final ConflictSet confset : getForcedConflictSetMovesFrom() ) {
			ret |= confset.contains( segnode );
		}
		return ret;
	}

	/**
	 * @param segnode
	 *            the segnode to perform the action on
	 * @return true, iff the given segnode has division forced towards it
	 *         or from a conflict set force it is contained in
	 */
	public boolean isDivisionForcedTo( final SegmentNode segnode ) {
		boolean ret = forcedSegmentNodeDivisionsTo.contains( segnode );
		for ( final ConflictSet confset : getForcedConflictSetDivisionsTo() ) {
			ret |= confset.contains( segnode );
		}
		return ret;
	}

	/**
	 * @param segnode
	 *            the segnode to perform the action on
	 * @return true, iff the given segnode has division forced away from it
	 *         or from a conflict set force it is contained in (towards future)
	 */
	public boolean isDivisionForcedFrom( final SegmentNode segnode ) {
		boolean ret = forcedSegmentNodeDivisionsFrom.contains( segnode );
		for ( final ConflictSet confset : getForcedConflictSetDivisionsFrom() ) {
			ret |= confset.contains( segnode );
		}
		return ret;
	}

}
