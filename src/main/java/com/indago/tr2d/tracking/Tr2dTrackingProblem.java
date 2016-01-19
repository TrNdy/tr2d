package com.indago.tr2d.tracking;

import java.util.ArrayList;
import java.util.List;

import com.indago.fg.CostsFactory;
import com.indago.segment.LabelingSegment;
import com.indago.tracking.TrackingProblem;
import com.indago.tracking.map.AppearanceHypothesis;
import com.indago.tracking.map.DisappearanceHypothesis;
import com.indago.tracking.map.DivisionHypothesis;
import com.indago.tracking.map.MovementHypothesis;
import com.indago.tracking.seg.SegmentVar;

import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;

public class Tr2dTrackingProblem implements TrackingProblem {

	private final List< Tr2dSegmentationProblem > timepoints;
	private final CostsFactory< LabelingSegment > appearanceCosts;
	private final CostsFactory< Pair< LabelingSegment, LabelingSegment > > movementCosts;
	private final CostsFactory< Pair< LabelingSegment, Pair< LabelingSegment, LabelingSegment > > > divisionCosts;
	private final CostsFactory< LabelingSegment > disappearanceCosts;

	private double maxRelevantMovementCost = Double.MAX_VALUE;
	private double maxRelevantDivisionCost = Double.MAX_VALUE;

	public Tr2dTrackingProblem(
			final CostsFactory< LabelingSegment > appearanceCosts,
			final CostsFactory< Pair< LabelingSegment, LabelingSegment > > movementCosts,
			final double maxRelevantMovementCost,
			final CostsFactory< Pair< LabelingSegment, Pair< LabelingSegment, LabelingSegment > > > divisionCosts,
			final double maxRelevantDivisionCost,
			final CostsFactory< LabelingSegment > disappearanceCosts ) {
		timepoints = new ArrayList< >();
		this.appearanceCosts = appearanceCosts;
		this.movementCosts = movementCosts;
		this.divisionCosts = divisionCosts;
		this.disappearanceCosts = disappearanceCosts;
		this.maxRelevantMovementCost = maxRelevantMovementCost;
		this.maxRelevantDivisionCost = maxRelevantDivisionCost;
	}

	@Override
	public List< Tr2dSegmentationProblem > getTimepoints() {
		return timepoints;
	}

	/**
	 * @param segmentationProblem
	 */
	public void addSegmentationProblem( final Tr2dSegmentationProblem segmentationProblem ) {
		if ( timepoints.size() > 0 ) {
			addDisappearanceToLatestFrame();
		}
		timepoints.add( segmentationProblem );
		addAppearanceToLatestFrame();

		if ( timepoints.size() >= 2 ) {
			addMovesToLatestFramePair();
			addDivisionsToLatestFramePair();
		}
	}

	/**
	 *
	 */
	private void addAppearanceToLatestFrame() {
		final Tr2dSegmentationProblem segProblem = timepoints.get( timepoints.size() - 1 );
		for ( final SegmentVar segVar : segProblem.getSegments() ) {
			final AppearanceHypothesis appHyp =
					new AppearanceHypothesis( appearanceCosts
							.getCost( segProblem.getLabelingSegment( segVar ) ), segVar );
			segVar.getInAssignments().add( appHyp );
		}
	}

	/**
	 *
	 */
	private void addDisappearanceToLatestFrame() {
		final Tr2dSegmentationProblem segProblem = timepoints.get( timepoints.size() - 1 );
		for ( final SegmentVar segVar : segProblem.getSegments() ) {
			final DisappearanceHypothesis disappHyp =
					new DisappearanceHypothesis( disappearanceCosts
							.getCost( segProblem.getLabelingSegment( segVar ) ), segVar );
			segVar.getOutAssignments().add( disappHyp );
		}
	}

	/**
	 *
	 */
	private void addMovesToLatestFramePair() {
		final Tr2dSegmentationProblem segProblemL = timepoints.get( timepoints.size() - 2 );
		final Tr2dSegmentationProblem segProblemR = timepoints.get( timepoints.size() - 1 );
		for ( final SegmentVar segVarL : segProblemL.getSegments() ) {
			for ( final SegmentVar segVarR : segProblemR.getSegments() ) {
				final double cost = movementCosts.getCost(
						new ValuePair< LabelingSegment, LabelingSegment >(
								segProblemL.getLabelingSegment(	segVarL ),
								segProblemR.getLabelingSegment(	segVarR ) ) );
				if ( cost <= maxRelevantMovementCost ) {
					final MovementHypothesis moveHyp =
							new MovementHypothesis( cost, segVarL, segVarR );
					segVarL.getOutAssignments().add( moveHyp );
					segVarR.getInAssignments().add( moveHyp );
				}
			}
		}
	}

	/**
	 *
	 */
	private void addDivisionsToLatestFramePair() {
		final Tr2dSegmentationProblem segProblemL = timepoints.get( timepoints.size() - 2 );
		final Tr2dSegmentationProblem segProblemR = timepoints.get( timepoints.size() - 1 );
		for ( final SegmentVar segVarL : segProblemL.getSegments() ) {
			for ( final SegmentVar segVarR1 : segProblemR.getSegments() ) {
				for ( final SegmentVar segVarR2 : segProblemR.getSegments() ) {
					// avoid double enumerations
					if ( segVarR2.hashCode() < segVarR1.hashCode() ) {
						continue;
					}
					final double cost = divisionCosts.getCost(
							new ValuePair< LabelingSegment, Pair< LabelingSegment, LabelingSegment > >(
									segProblemL.getLabelingSegment( segVarL ),
									new ValuePair< LabelingSegment, LabelingSegment> (
											segProblemR.getLabelingSegment( segVarR1 ),
											segProblemR.getLabelingSegment( segVarR2 ) ) ) );
					if ( cost <= maxRelevantDivisionCost ) {
						final DivisionHypothesis moveHyp =
								new DivisionHypothesis( cost, segVarL, segVarR1, segVarR2 );
						segVarL.getOutAssignments().add( moveHyp );
						segVarR1.getInAssignments().add( moveHyp );
						segVarR2.getInAssignments().add( moveHyp );
					}
				}
			}
		}
	}

}
