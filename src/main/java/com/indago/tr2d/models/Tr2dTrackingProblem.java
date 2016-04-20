package com.indago.tr2d.models;

import java.util.ArrayList;
import java.util.List;

import com.indago.data.segmentation.LabelingSegment;
import com.indago.models.TrackingProblem;
import com.indago.models.assignments.AppearanceHypothesis;
import com.indago.models.assignments.DisappearanceHypothesis;
import com.indago.models.assignments.DivisionHypothesis;
import com.indago.models.assignments.MovementHypothesis;
import com.indago.models.segments.SegmentNode;
import com.indago.old_fg.CostsFactory;

import net.imglib2.KDTree;
import net.imglib2.RealLocalizable;
import net.imglib2.neighborsearch.RadiusNeighborSearchOnKDTree;
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
		if ( timepoints.size() == 0 ) {
			timepoints.add( segmentationProblem );
			addAppearanceToLatestFrame( false );
		} else {
			addDisappearanceToLatestFrame();
			timepoints.add( segmentationProblem );
			addAppearanceToLatestFrame( true );
		}

		if ( timepoints.size() >= 2 ) {
			addMovesToLatestFramePair();
			addDivisionsToLatestFramePair();
		}
	}

	/**
	 *
	 */
	private void addAppearanceToLatestFrame( final boolean isFirstFrame ) {
		final Tr2dSegmentationProblem segProblem = timepoints.get( timepoints.size() - 1 );
		for ( final SegmentNode segVar : segProblem.getSegments() ) {
			AppearanceHypothesis appHyp = null;
			if ( isFirstFrame ) {
				appHyp = new AppearanceHypothesis( appearanceCosts.getCost( segProblem.getLabelingSegment( segVar ) ), segVar );
			} else {
				appHyp = new AppearanceHypothesis( 0, segVar );
			}
			segVar.getInAssignments().add( appHyp );
		}
	}

	/**
	 *
	 */
	private void addDisappearanceToLatestFrame() {
		final Tr2dSegmentationProblem segProblem = timepoints.get( timepoints.size() - 1 );
		for ( final SegmentNode segVar : segProblem.getSegments() ) {
			final DisappearanceHypothesis disappHyp =
					new DisappearanceHypothesis( disappearanceCosts
							.getCost( segProblem.getLabelingSegment( segVar ) ), segVar );
			segVar.getOutAssignments().add( disappHyp );
		}
	}

	/**
	 * Last frame needs to get disappearances in order for continuity
	 * constraints to work out.
	 * This is usually the last method to be called when creating the model.
	 */
	public void addDummyDisappearanceToFinishModel() {
		final Tr2dSegmentationProblem segProblem = timepoints.get( timepoints.size() - 1 );
		for ( final SegmentNode segVar : segProblem.getSegments() ) {
			final DisappearanceHypothesis disappHyp = new DisappearanceHypothesis( 0, segVar );
			segVar.getOutAssignments().add( disappHyp );
		}
	}

	/**
	 *
	 */
	private void addMovesToLatestFramePair() {
		final Tr2dSegmentationProblem segProblemL = timepoints.get( timepoints.size() - 2 );
		final Tr2dSegmentationProblem segProblemR = timepoints.get( timepoints.size() - 1 );

		List< SegmentNode > segmentList;
		if ( segProblemR.getSegments() instanceof List )
			segmentList = ( List< SegmentNode > ) segProblemR.getSegments();
		else
			segmentList = new ArrayList<>( segProblemR.getSegments() );
		final ArrayList< RealLocalizable > positions = new ArrayList<>();
		for ( final SegmentNode n : segmentList )
			positions.add( n.getSegment().getCenterOfMass() );
		final KDTree< SegmentNode > kdtree = new KDTree<>( segmentList, positions );
		final RadiusNeighborSearchOnKDTree< SegmentNode > search = new RadiusNeighborSearchOnKDTree<>( kdtree );

		for ( final SegmentNode segVarL : segProblemL.getSegments() ) {
			final RealLocalizable pos = segVarL.getSegment().getCenterOfMass();
			final double radius = 20; // TODO
			search.search( pos, radius, false );
			final int numNeighbors = search.numNeighbors();
			for ( int i = 0; i < numNeighbors; ++i ) {
				final SegmentNode segVarR = search.getSampler( i ).get();

				final double cost = movementCosts.getCost(
						new ValuePair< LabelingSegment, LabelingSegment >(
								segVarL.getSegment(),
								segVarR.getSegment() ) );
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

		List< SegmentNode > segmentList;
		if ( segProblemR.getSegments() instanceof List )
			segmentList = ( List< SegmentNode > ) segProblemR.getSegments();
		else
			segmentList = new ArrayList<>( segProblemR.getSegments() );
		final ArrayList< RealLocalizable > positions = new ArrayList<>();
		for ( final SegmentNode n : segmentList )
			positions.add( n.getSegment().getCenterOfMass() );
		final KDTree< SegmentNode > kdtree = new KDTree<>( segmentList, positions );
		final RadiusNeighborSearchOnKDTree< SegmentNode > search = new RadiusNeighborSearchOnKDTree<>( kdtree );

		for ( final SegmentNode segVarL : segProblemL.getSegments() ) {
			final RealLocalizable pos = segVarL.getSegment().getCenterOfMass();
			final double radius = 30; // TODO
			search.search( pos, radius, false );
			final int numNeighbors = search.numNeighbors();
			for ( int i = 0; i < numNeighbors; ++i ) {
				for ( int j = i + 1; j < numNeighbors; ++j ) {
					final SegmentNode segVarR1 = search.getSampler( i ).get();
					final SegmentNode segVarR2 = search.getSampler( j ).get();
					final double cost = divisionCosts.getCost(
							new ValuePair< LabelingSegment, Pair< LabelingSegment, LabelingSegment > >(
									segVarL.getSegment(),
									new ValuePair< LabelingSegment, LabelingSegment> (
											segVarR1.getSegment(),
											segVarR2.getSegment() ) ) );
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
