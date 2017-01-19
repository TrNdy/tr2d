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
	protected final Set< SegmentNode > forcedSegmentNodeMoves = new HashSet<>();
	protected final Set< SegmentNode > forcedSegmentNodeDivisions = new HashSet<>();
	protected final Set< SegmentNode > avoidedSegmentNodes = new HashSet<>();

	public EditState() {}

	public EditState( final EditState state ) {
		this.forcedSegmentNodes.addAll( state.forcedSegmentNodes );
		this.forcedSegmentNodeAppearances.addAll( state.forcedSegmentNodeAppearances );
		this.forcedSegmentNodeDisappearances.addAll( state.forcedSegmentNodeDisappearances );
		this.forcedSegmentNodeMoves.addAll( state.forcedSegmentNodeMoves );
		this.forcedSegmentNodeDivisions.addAll( state.forcedSegmentNodeDivisions );
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

	public Set< SegmentNode > getForcedSegmentNodeMoves() {
		return forcedSegmentNodeMoves;
	}

	public Set< SegmentNode > getForcedSegmentNodeDivisions() {
		return forcedSegmentNodeDivisions;
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
				forcedSegmentNodeMoves.size(),
				forcedSegmentNodeDivisions.size(),
				avoidedSegmentNodes.size() );
	}
}
