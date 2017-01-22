/**
 *
 */
package com.indago.tr2d.pg.levedit;

import java.util.HashSet;
import java.util.Set;

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

	public EditState() {}

	public EditState( final EditState state ) {
		this.forcedSegmentNodes.addAll( state.forcedSegmentNodes );
		this.forcedSegmentNodeAppearances.addAll( state.forcedSegmentNodeAppearances );
		this.forcedSegmentNodeDisappearances.addAll( state.forcedSegmentNodeDisappearances );
		this.forcedSegmentNodeMovesTo.addAll( state.forcedSegmentNodeMovesTo );
		this.forcedSegmentNodeDivisionsTo.addAll( state.forcedSegmentNodeDivisionsFrom );
		this.forcedSegmentNodeMovesFrom.addAll( state.forcedSegmentNodeMovesTo );
		this.forcedSegmentNodeDivisionsFrom.addAll( state.forcedSegmentNodeDivisionsFrom );
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
}
