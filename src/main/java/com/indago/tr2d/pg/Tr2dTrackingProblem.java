package com.indago.tr2d.pg;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import com.indago.costs.CostFactory;
import com.indago.data.segmentation.LabelingSegment;
import com.indago.pg.TrackingProblem;
import com.indago.pg.assignments.AppearanceHypothesis;
import com.indago.pg.assignments.DisappearanceHypothesis;
import com.indago.pg.assignments.DivisionHypothesis;
import com.indago.pg.assignments.MovementHypothesis;
import com.indago.pg.segments.SegmentNode;
import com.indago.tr2d.ui.model.Tr2dFlowModel;

import net.imglib2.KDTree;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.neighborsearch.RadiusNeighborSearchOnKDTree;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;

/**
 * @author jug
 */
public class Tr2dTrackingProblem implements TrackingProblem {

	private final Tr2dFlowModel flowModel;

	private final List< Tr2dSegmentationProblem > timepoints;
	private final CostFactory< LabelingSegment > appearanceCosts;
	private final CostFactory< Pair< Pair< LabelingSegment, LabelingSegment >, Pair< Double, Double > > > movementCosts;
	private final CostFactory< Pair< LabelingSegment, Pair< LabelingSegment, LabelingSegment > > > divisionCosts;
	private final CostFactory< LabelingSegment > disappearanceCosts;

	private final int maxMovementsToAddPerHypothesis = 5; //TODO make this some user configurable parameter
	private final int maxDivisionsToAddPerHypothesis = 5; //TODO make this some user configurable parameter

	public Tr2dTrackingProblem(
			final Tr2dFlowModel flowModel,
			final CostFactory< LabelingSegment > appearanceCosts,
			final CostFactory< Pair< Pair< LabelingSegment, LabelingSegment >, Pair< Double, Double > > > movementCosts,
			final CostFactory< Pair< LabelingSegment, Pair< LabelingSegment, LabelingSegment > > > divisionCosts,
			final CostFactory< LabelingSegment > disappearanceCosts ) {
		this.flowModel = flowModel;
		timepoints = new ArrayList< >();
		this.appearanceCosts = appearanceCosts;
		this.movementCosts = movementCosts;
		this.divisionCosts = divisionCosts;
		this.disappearanceCosts = disappearanceCosts;
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
	public void addDummyDisappearance() {
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

			// retrieve flow vector at desired location
			final int t = segProblemL.getTime();
			final int x = ( int ) segVarL.getSegment().getCenterOfMass().getFloatPosition( 0 );
			final int y = ( int ) segVarL.getSegment().getCenterOfMass().getFloatPosition( 1 );
			final ValuePair< Double, Double > flow_vec = flowModel.getFlowVector( t, x, y );

			final RealLocalizable pos = segVarL.getSegment().getCenterOfMass();
			final RealPoint flow_pos = new RealPoint(
					pos.getDoublePosition( 0 ) + flow_vec.getA(),
					pos.getDoublePosition( 1 ) + flow_vec.getB() );

			final double radius = 35; // TODO needs to be smarter and NOT fixed to 35 pixels distance
			final PriorityQueue< MovementHypothesis > prioQueue = new PriorityQueue<>( 100, Util.getCostComparatorForMovementHypothesis() );

			search.search( flow_pos, radius, false );
			final int numNeighbors = search.numNeighbors();
			for ( int i = 0; i < numNeighbors; ++i ) {
				final SegmentNode segVarR = search.getSampler( i ).get();

				final double cost_flow = movementCosts.getCost(
						new ValuePair<> (
    						new ValuePair< LabelingSegment, LabelingSegment >(
    								segVarL.getSegment(),
    								segVarR.getSegment() ),
							flow_vec ) );
				prioQueue.add( new MovementHypothesis( cost_flow, segVarL, segVarR ) );
			}
			for ( int i = 0; i < Math.min( maxMovementsToAddPerHypothesis, prioQueue.size() ); i++ ) {
				final MovementHypothesis moveHyp = prioQueue.poll();
				moveHyp.getSrc().getOutAssignments().add( moveHyp );
				moveHyp.getDest().getInAssignments().add( moveHyp );
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

			final double radius = 60; // TODO needs to be smarter and NOT fixed to 60 pixels distance
			final PriorityQueue< DivisionHypothesis > prioQueue = new PriorityQueue<>( 100, Util.getCostComparatorForDivisionHypothesis() );

			search.search( pos, radius, true );
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
					prioQueue.add( new DivisionHypothesis( cost, segVarL, segVarR1, segVarR2 ) );
				}
			}
			for ( int i = 0; i < Math.min( maxDivisionsToAddPerHypothesis, prioQueue.size() ); i++ ) {
				final DivisionHypothesis divHyp = prioQueue.poll();
				divHyp.getSrc().getOutAssignments().add( divHyp );
				divHyp.getDest1().getInAssignments().add( divHyp );
				divHyp.getDest2().getInAssignments().add( divHyp );
			}
		}
	}

	/**
	 * Saves the problem graph to file
	 *
	 * @param file
	 */
	public void saveToFile( final File file ) throws IOException {
		for ( final Tr2dSegmentationProblem segProblem : timepoints ) {

		}
	}

}
