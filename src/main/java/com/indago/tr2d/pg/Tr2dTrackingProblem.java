package com.indago.tr2d.pg;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.function.Consumer;

import javax.swing.JOptionPane;

import com.indago.costs.CostFactory;
import com.indago.data.segmentation.ConflictGraph;
import com.indago.data.segmentation.LabelingSegment;
import com.indago.fg.Assignment;
import com.indago.pg.IndicatorNode;
import com.indago.pg.TrackingProblem;
import com.indago.pg.assignments.AppearanceHypothesis;
import com.indago.pg.assignments.AssignmentNode;
import com.indago.pg.assignments.DisappearanceHypothesis;
import com.indago.pg.assignments.DivisionHypothesis;
import com.indago.pg.assignments.MovementHypothesis;
import com.indago.pg.segments.ConflictSet;
import com.indago.pg.segments.SegmentNode;
import com.indago.tr2d.Tr2dLog;
import com.indago.tr2d.data.LabelingTimeLapse;
import com.indago.tr2d.ui.model.Tr2dFlowModel;
import com.indago.tr2d.ui.model.Tr2dTrackingModel;
import com.indago.util.Bimap;
import com.indago.util.TicToc;

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

	private final Tr2dTrackingModel trackingModel;
	private final Tr2dFlowModel flowModel;

	private final List< Tr2dSegmentationProblem > timepoints;
	private final CostFactory< LabelingSegment > appearanceCosts;
	private final CostFactory< Pair< Pair< LabelingSegment, LabelingSegment >, Pair< Double, Double > > > movementCosts;
	private final CostFactory< Pair< LabelingSegment, Pair< LabelingSegment, LabelingSegment > > > divisionCosts;
	private final CostFactory< LabelingSegment > disappearanceCosts;

	private final Tr2dTrackingProblemSerializer serializer = new Tr2dTrackingProblemSerializer();

	public Tr2dTrackingProblem(
			final Tr2dTrackingModel trackingModel,
			final Tr2dFlowModel flowModel,
			final CostFactory< LabelingSegment > appearanceCosts,
			final CostFactory< Pair< Pair< LabelingSegment, LabelingSegment >, Pair< Double, Double > > > movementCosts,
			final CostFactory< Pair< LabelingSegment, Pair< LabelingSegment, LabelingSegment > > > divisionCosts,
			final CostFactory< LabelingSegment > disappearanceCosts ) {
		this.trackingModel = trackingModel;
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

	public void addSegmentationProblem( final Tr2dSegmentationProblem segmentationProblem ) {
		if ( timepoints.size() == 0 ) {
			timepoints.add( segmentationProblem );
			addAppearanceToLatestFrame( true );
		} else {
			addDisappearanceToLatestFrame();
			timepoints.add( segmentationProblem );
			addAppearanceToLatestFrame( false );
		}

		if ( timepoints.size() >= 2 ) {
			addMovesToLatestFramePair();
			addDivisionsToLatestFramePair();
		}
	}

	private void addAppearanceToLatestFrame( final boolean isFirstFrame ) {
		final Tr2dSegmentationProblem segProblem = timepoints.get( timepoints.size() - 1 );
		for ( final SegmentNode segVar : segProblem.getSegments() ) {
			AppearanceHypothesis appHyp = null;
			if ( isFirstFrame ) {
				appHyp = new AppearanceHypothesis( 0, segVar );
			} else {
				appHyp = new AppearanceHypothesis( appearanceCosts.getCost( segProblem.getLabelingSegment( segVar ) ), segVar );
			}
			segVar.getInAssignments().add( appHyp );
		}
	}

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

	private void addMovesToLatestFramePair() {
		final Tr2dSegmentationProblem segProblemL = timepoints.get( timepoints.size() - 2 );
		final Tr2dSegmentationProblem segProblemR = timepoints.get( timepoints.size() - 1 );

		final RadiusNeighborSearchOnKDTree< SegmentNode > search = createRadiusNeighborSearch( segProblemR );

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

			final PriorityQueue< MovementHypothesis > prioQueue = new PriorityQueue<>( 100, Util.getCostComparatorForMovementHypothesis() );

			search.search( flow_pos, trackingModel.getMaxMovementSearchRadius(), false );
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

			final int assmtsToAdd = Math.min( trackingModel.getMaxMovementsToAddPerHypothesis(), prioQueue.size() );
			for ( int i = 0; i < assmtsToAdd; i++ ) {
				final MovementHypothesis moveHyp = prioQueue.poll();
				moveHyp.getSrc().getOutAssignments().add( moveHyp );
				moveHyp.getDest().getInAssignments().add( moveHyp );
			}
		}
	}

	private RadiusNeighborSearchOnKDTree< SegmentNode > createRadiusNeighborSearch( final Tr2dSegmentationProblem segProblem ) {
		List< SegmentNode > segmentList;
		if ( segProblem.getSegments() instanceof List )
			segmentList = ( List< SegmentNode > ) segProblem.getSegments();
		else
			segmentList = new ArrayList<>( segProblem.getSegments() );
		final ArrayList< RealLocalizable > positions = new ArrayList<>();
		for ( final SegmentNode n : segmentList )
			positions.add( n.getSegment().getCenterOfMass() );
		final KDTree< SegmentNode > kdtree = new KDTree<>( segmentList, positions );
		final RadiusNeighborSearchOnKDTree< SegmentNode > search = new RadiusNeighborSearchOnKDTree<>( kdtree );
		return search;
	}

	private void addDivisionsToLatestFramePair() {
		final Tr2dSegmentationProblem segProblemL = timepoints.get( timepoints.size() - 2 );
		final Tr2dSegmentationProblem segProblemR = timepoints.get( timepoints.size() - 1 );

		final RadiusNeighborSearchOnKDTree< SegmentNode > search = createRadiusNeighborSearch( segProblemR );

		for ( final SegmentNode segVarL : segProblemL.getSegments() ) {
			final RealLocalizable pos = segVarL.getSegment().getCenterOfMass();

			final PriorityQueue< DivisionHypothesis > prioQueue = new PriorityQueue<>( 100, Util.getCostComparatorForDivisionHypothesis() );

			search.search( pos, trackingModel.getMaxDivisionSearchRadius(), true );
			final int numNeighbors = search.numNeighbors();
			for ( int i = 0; i < numNeighbors; ++i ) {
				for ( int j = i + 1; j < numNeighbors; ++j ) {
					final SegmentNode segVarR1 = search.getSampler( i ).get();
					final SegmentNode segVarR2 = search.getSampler( j ).get();

					if ( segVarR1.getSegment().conflictsWith( segVarR2.getSegment() ) ) {
						continue; // do not add divisions towards conflicting hypotheses
					}

					final double cost = divisionCosts.getCost(
							new ValuePair< LabelingSegment, Pair< LabelingSegment, LabelingSegment > >(
									segVarL.getSegment(),
									new ValuePair< LabelingSegment, LabelingSegment> (
											segVarR1.getSegment(),
											segVarR2.getSegment() ) ) );
					prioQueue.add( new DivisionHypothesis( cost, segVarL, segVarR1, segVarR2 ) );
				}
			}

			final int assmtsToAdd = Math.min( trackingModel.getMaxDivisionsToAddPerHypothesis(), prioQueue.size() );
			for ( int i = 0; i < assmtsToAdd; i++ ) {
				final DivisionHypothesis divHyp = prioQueue.poll();
				divHyp.getSrc().getOutAssignments().add( divHyp );
				divHyp.getDest1().getInAssignments().add( divHyp );
				divHyp.getDest2().getInAssignments().add( divHyp );
			}
		}
	}

	public Tr2dTrackingProblemSerializer getSerializer() {
		return serializer;
	}

	static final class NodeId
	{
		private final int id;

		public NodeId( final int id ) {
			this.id = id;
		}

		@Override
		public boolean equals( final Object o ) {
			if ( this == o )
				return true;
			if ( ! ( o instanceof NodeId ) )
				return false;
			final NodeId other = ( NodeId ) o;
			return id == other.id;
		}

		@Override
		public int hashCode() {
			return id;
		}

		public int id() {
			return id;
		}
	}

	public static class Tr2dTrackingProblemSerializer {


		private final Bimap< SegmentNode, NodeId > bimapSeg2Id;
		private final Bimap< AssignmentNode, NodeId > bimapAss2Id;

		public Tr2dTrackingProblemSerializer() {
			bimapSeg2Id = new Bimap<>();
			bimapAss2Id = new Bimap<>();
		}

		private NodeId nodeId( final SegmentNode node )
		{
			return bimapSeg2Id.getB( node );
		}

		private NodeId nodeId( final AssignmentNode node )
		{
			return bimapAss2Id.getB( node );
		}

		private static void buildBimapSeg2Id( final Tr2dTrackingProblem ttp, final Bimap< SegmentNode, NodeId > map )
		{
			map.clear();
			final List< Tr2dSegmentationProblem > timePoints = ttp.getTimepoints();
			for ( final Tr2dSegmentationProblem timePoint : timePoints ) {
				final int t = timePoint.getTime();
				for ( final SegmentNode segment : timePoint.getSegments() ) {
					map.add( segment, new NodeId( segment.getSegment().getId() ) );
				}
			}
		}

		private static void buildBimapAss2Id( final Tr2dTrackingProblem ttp, final Bimap< AssignmentNode, NodeId > map )
		{
			map.clear();
			final List< Tr2dSegmentationProblem > timePoints = ttp.getTimepoints();
			for ( final Tr2dSegmentationProblem timePoint : timePoints ) {
				final int t = timePoint.getTime();

				// Adds an AssignmentNode to map with this timepoint and a new id
				final Consumer< AssignmentNode > addToMap = new Consumer< AssignmentNode >() {
					private int next_assignment_id = -1;

					@Override
					public void accept( final AssignmentNode node ) {
						map.add( node, new NodeId( ++next_assignment_id ) );
					}
				};

				// sort segments by segment id
				final ArrayList< SegmentNode > segments = new ArrayList<>( timePoint.getSegments() );
				segments.sort( Comparator.comparingInt( s -> s.getSegment().getId() ) );

				// for each segment hypothesis, add all assignments
				for ( final SegmentNode segment : segments ) {

					// sort appearances by destination segment id
					segment.getInAssignments().getAppearances()
							.stream()
							.sorted( Comparator.comparingInt( a -> a.getDest().getSegment().getId() ) )
							.forEach( addToMap );

					// sort disappearances by source segment id
					segment.getOutAssignments().getDisappearances()
							.stream()
							.sorted( Comparator.comparingInt( d -> d.getSrc().getSegment().getId() ) )
							.forEach( addToMap );

					// sort moves by destination segment id
					segment.getOutAssignments().getMoves()
							.stream()
							.sorted( Comparator.comparingInt( m -> m.getDest().getSegment().getId() ) )
							.forEach( addToMap );

					segment.getOutAssignments().getDivisions()
							.stream()
							.sorted( Comparator.comparingInt( ( final DivisionHypothesis d ) -> d.getDest1().getSegment().getId() )
									.thenComparingInt( d -> d.getDest2().getSegment().getId() ) )
							.forEach( addToMap );
				}
			}
		}




		public void savePgraph( final Tr2dTrackingProblem ttp, final File file ) throws IOException {
			final boolean export_continuation_constraints = false; // currently not desired (agreement with Paul)

			buildBimapSeg2Id( ttp, bimapSeg2Id );
			buildBimapAss2Id( ttp, bimapAss2Id );

			final int next_assignment_id = -1;

			try {
				final SimpleDateFormat sdfDate = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
				final Date now = new Date();
				final String strNow = sdfDate.format( now );

				final BufferedWriter problemWriter = new BufferedWriter( new FileWriter( file ) );
				problemWriter.write( "# Tr2d problem export from " + strNow + "\n" );

				final List< Tr2dSegmentationProblem > timePoints = ttp.getTimepoints();

				problemWriter.write( "\n# === SEGMENT HYPOTHESES =================================================\n" );
				for ( final Tr2dSegmentationProblem t : timePoints ) {
					problemWriter.write( String.format( "\n# t=%d\n", t.getTime() ) );

					// write all segment hypotheses
					for ( final SegmentNode segment : t.getSegments() ) {
						writeSegmentLine( t.getTime(), nodeId( segment ), segment, problemWriter );
					}
				}

				problemWriter.write( "\n# === ASSIGNMNETS ========================================================\n\n" );
				for ( final Tr2dSegmentationProblem t : timePoints ) {
					// for each segment hypothesis, write all assignments
					for ( final SegmentNode segment : t.getSegments() ) {

						final Collection< AppearanceHypothesis > apps = segment.getInAssignments().getAppearances();
						for ( final AppearanceHypothesis app : apps ) {
							writeAppearanceLine( nodeId( app ), app, bimapSeg2Id, problemWriter );
						}
						final Collection< DisappearanceHypothesis > disapps = segment.getOutAssignments().getDisappearances();
						for ( final DisappearanceHypothesis disapp : disapps ) {
							writeDisappearanceLine( nodeId( disapp ), disapp, bimapSeg2Id, problemWriter );
						}
						final Collection< MovementHypothesis > moves = segment.getOutAssignments().getMoves();
						for ( final MovementHypothesis move : moves ) {
							writeMovementLine( nodeId( move ), move, bimapSeg2Id, problemWriter );
						}
						final Collection< DivisionHypothesis > divs = segment.getOutAssignments().getDivisions();
						for ( final DivisionHypothesis div : divs ) {
							writeDivisionLine( nodeId( div ), div, bimapSeg2Id, problemWriter );
						}
						problemWriter.write( "\n" );
					}
				}

				problemWriter.write( "# === CONSTRAINTS ========================================================\n\n" );
				for ( final Tr2dSegmentationProblem t : timePoints ) {
					for ( final ConflictSet cs : t.getConflictSets() ) {
						final List< NodeId> timeAndIdPairs = new ArrayList<>();
						final Iterator< SegmentNode > it = cs.iterator();
						while ( it.hasNext() ) {
							final SegmentNode segnode = it.next();
							final NodeId timeAndId = nodeId( segnode );
							if ( timeAndId == null ) throw new IllegalStateException( "this should not be possible -- find bug!" );
							timeAndIdPairs.add( timeAndId );
						}
						// CONFSET <id...>
						problemWriter.write( "CONFSET " );
						boolean first = true;
						for ( final NodeId timeAndId : timeAndIdPairs ) {
							if ( !first ) problemWriter.write( " + " );
							problemWriter.write( String.format( "%4d ", timeAndId.id() ) );
							first = false;
						}
						problemWriter.write( " <= 1\n" );

						// - - - - - - - - - - - - - - - - - - - - -
						// We agreed with Paul and Bogdan that this
						// is explicitly clear and therefore not ne-
						// cessary to be exported.
						if ( export_continuation_constraints ) {
							final Collection< SegmentNode > segments = t.getSegments();
							for ( final SegmentNode segment : segments ) {
								final List< NodeId > leftSegs = new ArrayList<>();
								for ( final AssignmentNode ass : segment.getInAssignments().getAllAssignments() ) {
									final NodeId timeAndId = nodeId( ass );
									if ( timeAndId == null ) throw new IllegalStateException( "this should not be possible -- find bug!" );
									leftSegs.add( timeAndId );
								}
								final NodeId segTimeAndId = nodeId( segment );

								// CONT <time> <seg_id> <left_ass_ids as (time, id) pairs...>
								problemWriter.write( String.format( "CONT    %4d ", segTimeAndId.id() ) );
								for ( final NodeId leftTimeAndId : leftSegs ) {
									problemWriter.write( String.format( "%4d", leftTimeAndId.id() ) );
								}
								problemWriter.write( "\n" );

								final List< NodeId > rightSegs = new ArrayList<>();
								for ( final AssignmentNode ass : segment.getOutAssignments().getAllAssignments() ) {
									final NodeId timeAndId = nodeId( ass );
									if ( timeAndId == null ) throw new IllegalStateException( "this should not be possible -- find bug!" );
									rightSegs.add( timeAndId );
								}
								// CONT <time> <seg_id> <right_ass_ids as (time, id) pairs...>
								problemWriter.write( String.format( "CONT    %4d ", segTimeAndId.id() ) );
								for ( final NodeId rightTimeAndId : rightSegs ) {
									problemWriter.write( String.format( "%4d", rightTimeAndId.id() ) );
								}
								problemWriter.write( "\n" );
							}
						}
					}
				}

				problemWriter.close();

			} catch ( final IOException e ) {
				JOptionPane
						.showMessageDialog(
								null,
								"Cannot write in selected export folder... cancel export!",
								"File Error",
								JOptionPane.ERROR_MESSAGE );
				e.printStackTrace();
			}
		}

		public void saveSolution( final Tr2dTrackingProblem ttp, final Assignment< IndicatorNode > pgAssignment, final File file ) {
			try {
				final BufferedWriter solutionWriter = new BufferedWriter( new FileWriter( file ) );

				final SimpleDateFormat sdfDate = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
				final Date now = new Date();
				final String strNow = sdfDate.format( now );
				solutionWriter.write( "# Tr2d solution export from " + strNow + "\n" );
				solutionWriter.write( "# -----------------------------------------------------------------------\n\n" );

				solutionWriter.write( "\n# === SEGMENT HYPOTHESES =================================================\n" );
				for ( final Tr2dSegmentationProblem t : ttp.getTimepoints() ) {
					solutionWriter.write( String.format( "\n# t=%d\n", t.getTime() ) );

					for ( final SegmentNode segmentNode : t.getSegments() ) {
						if ( pgAssignment.getAssignment( segmentNode ) == 1 ) {
							final NodeId key = nodeId( segmentNode );
							solutionWriter.write( String.format( "H %3d %4d\n", t.getTime(), key.id() ) );
						}
					}
				}
//				final Collection< SegmentNode > segNodes = this.bimapSeg2Id.valuesAs();
//				for ( final SegmentNode segmentNode : segNodes ) {
//					if ( pgAssignment.getAssignment( segmentNode ) == 1 ) {
//						final ValuePair< Integer, Integer > key = this.bimapSeg2Id.getB( segmentNode );
//						solutionWriter.write( String.format( "H %3d %4d\n", key.a, key.b ) );
//					}
//				}

					solutionWriter.write( "\n# === ASSIGNMNETS ========================================================\n\n" );
				for ( final Tr2dSegmentationProblem t : ttp.getTimepoints() ) {
					for ( final SegmentNode segment : t.getSegments() ) {

						final Collection< AppearanceHypothesis > apps = segment.getInAssignments().getAppearances();
						for ( final AppearanceHypothesis app : apps ) {
							if ( pgAssignment.getAssignment( app ) == 1 ) {
								final NodeId key = nodeId( app );
								solutionWriter.write( String.format( "APP %4d\n", key.id() ) );
							}						}
						final Collection< DisappearanceHypothesis > disapps = segment.getOutAssignments().getDisappearances();
						for ( final DisappearanceHypothesis disapp : disapps ) {
							if ( pgAssignment.getAssignment( disapp ) == 1 ) {
								final NodeId key = nodeId( disapp );
								solutionWriter.write( String.format( "DISAPP %4d\n", key.id() ) );
							}
						}
						final Collection< MovementHypothesis > moves = segment.getOutAssignments().getMoves();
						for ( final MovementHypothesis move : moves ) {
							if ( pgAssignment.getAssignment( move ) == 1 ) {
								final NodeId key = nodeId( move );
								solutionWriter.write( String.format( "MOVE %4d\n", key.id() ) );
							}
						}
						final Collection< DivisionHypothesis > divs = segment.getOutAssignments().getDivisions();
						for ( final DivisionHypothesis div : divs ) {
							if ( pgAssignment.getAssignment( div ) == 1 ) {
								final NodeId key = nodeId( div );
								solutionWriter.write( String.format( "DIV %4d\n", key.id() ) );
							}
						}
					}
				}

//				final Collection< AssignmentNode > assNodes = this.bimapAss2Id.valuesAs();
//				for ( final AssignmentNode assignmentNode : assNodes ) {
//					if ( pgAssignment.getAssignment( assignmentNode ) == 1 ) {
//						final NodeId key = nodeId( assignmentNode );
//						solutionWriter.write( String.format( "A %3d %4d\n", key.timepoint(), key.id() ) );
//					}
//				}

				solutionWriter.close();
			} catch ( final IOException ioe ) {
				Tr2dLog.solverlog.error( "Problem Graph solution could not be stored to " + file.getAbsolutePath() );
				ioe.printStackTrace();
			}
		}

		private static void writeSegmentLine( final int t, final NodeId segid, final SegmentNode segment, final BufferedWriter writer )
				throws IOException {
			// H <time> <id> <cost> (<com_x_pos> <com_y_pos>)
			writer.write(
					String.format(
							"H %3d %4d %.16f (%.1f,%.1f)\n",
							t,
							segid.id(),
							segment.getCost(),
							segment.getSegment().getCenterOfMass().getFloatPosition( 0 ),
							segment.getSegment().getCenterOfMass().getFloatPosition( 1 ) ) );
		}

		private static void writeAppearanceLine(
				final NodeId assid,
				final AppearanceHypothesis app,
				final Bimap< SegmentNode, NodeId > bimapSeg2Id,
				final BufferedWriter writer )
				throws IOException {
			// APP <id> <segment_id> <cost>
			writer.write(
					String.format(
							"APP     %4d %4d %.16f\n",
							assid.id(),
							bimapSeg2Id.getB( app.getDest() ).id(),
							app.getCost() ) );
		}

		private static void writeDisappearanceLine(
				final NodeId assid,
				final DisappearanceHypothesis disapp,
				final Bimap< SegmentNode, NodeId > bimapSeg2Id,
				final BufferedWriter writer )
				throws IOException {
			// DISAPP <id> <segment_id> <cost>
			writer.write(
					String.format(
							"DISAPP  %4d %4d %.16f\n",
							assid.id(),
							bimapSeg2Id.getB( disapp.getSrc() ).id(),
							disapp.getCost() ) );
		}

		private static void writeMovementLine(
				final NodeId assid,
				final MovementHypothesis move,
				final Bimap< SegmentNode, NodeId > bimapSeg2Id,
				final BufferedWriter writer )
				throws IOException {
			// MOVE <ass_id> <source_segment_id> <dest_segment_id> <cost>
			writer.write(
					String.format(
							"MOVE    %4d %4d %4d %.16f\n",
							assid.id(),
							bimapSeg2Id.getB( move.getSrc() ).id(),
							bimapSeg2Id.getB( move.getDest() ).id(),
							move.getCost() ) );
		}

		private static void writeDivisionLine(
				final NodeId assid,
				final DivisionHypothesis div,
				final Bimap< SegmentNode, NodeId > bimapSeg2Id,
				final BufferedWriter writer )
				throws IOException {
			// DIV <ass_id> <source_segment_id> <dest1_segment_id> <dest2_segment_id> <cost>
			final int srcId = bimapSeg2Id.getB( div.getSrc() ).id();
			final NodeId timeAndId4Dest1 = bimapSeg2Id.getB( div.getDest1() );
			final NodeId timeAndId4Dest2 = bimapSeg2Id.getB( div.getDest2() );
			writer.write(
					String.format(
							"DIV     %4d %4d %4d %4d %.16f\n",
							assid.id(),
							bimapSeg2Id.getB( div.getSrc() ).id(),
							bimapSeg2Id.getB( div.getDest1() ).id(),
							bimapSeg2Id.getB( div.getDest2() ).id(),
							div.getCost() ) );
		}

		public Bimap< SegmentNode, NodeId > getBimapSeg2Id() {
			return bimapSeg2Id;
		}

		public Bimap< AssignmentNode, NodeId > getBimapAss2Id() {
			return bimapAss2Id;
		}

		/**
		 * @return true, if PGraph could be loaded.
		 * @throws IOException
		 */
		public static boolean loadPGraph( final Tr2dTrackingProblem ttp, final File file ) throws IOException {
			final TicToc tictoc = new TicToc();

			final LabelingTimeLapse labelingFrames = ttp.trackingModel.getLabelingFrames();

//			fireNextProgressPhaseEvent( "Building tracking problem (PG)...", labelingFrames.getNumFrames() );
			for ( int frameId = 0; frameId < labelingFrames.getNumFrames(); frameId++ ) {
				Tr2dLog.log.info(
						String.format( "Loading frame %d of %d...", frameId + 1, labelingFrames.getNumFrames() ) );

				// =============================
				// build Tr2dSegmentationProblem
				// =============================
				tictoc.tic( "Constructing Tr2dSegmentationProblem..." );
				final List< LabelingSegment > segments =
						labelingFrames.getLabelingSegmentsForFrame( frameId );
				final ConflictGraph< LabelingSegment > conflictGraph =
						labelingFrames.getConflictGraph( frameId );
				final Tr2dSegmentationProblem segmentationProblem =
						new Tr2dSegmentationProblem( frameId, segments, ttp.trackingModel.getSegmentCosts(), conflictGraph );
				tictoc.toc( "done!" );

				// =============================
				// add it to Tr2dTrackingProblem
				// =============================
				tictoc.tic( "Connect it to Tr2dTrackingProblem..." );
				ttp.trackingModel.getTr2dTraProblem().addSegmentationProblem( segmentationProblem );
				tictoc.toc( "done!" );

//				fireProgressEvent();
			}
			ttp.trackingModel.getTr2dTraProblem().addDummyDisappearance();

			Tr2dLog.log.info( "Tracking graph was loaded/build sucessfully!" );


//			final FileReader fr = new FileReader( file );
//			final BufferedReader br = new BufferedReader( fr );
//
//			String line = br.readLine();
//			while ( line != null ) {
//				if ( line.trim().startsWith( "#" ) || line.trim().equals( "" ) ) {
//					line = br.readLine();
//					continue;
//				}
//
//				final Scanner scanner = new Scanner( line );
//				scanner.useDelimiter( "\\s+" );
//
//				try {
//					final String type = scanner.next();
//					final int t = scanner.nextInt();
//					final int id = scanner.nextInt();
//
//					switch ( type ) {
//					case "H":
//						final double cost = scanner.nextDouble();
//						final int tpsize = ttp.getTimepoints().size();
//						while (t>tpsize) {
//							ttp.getTimepoints().add(
//								new Tr2dSegmentationProblem(
//										tpsize,
//										new ArrayList<>(),
//										ttp.trackingModel.getSegmentCosts(),
//										ttp.trackingModel.getLabelingFrames().getConflictGraph( t )
//										)
//								);
//							tpsize++;
//						}
//						ttp.getTimepoints().get( t ).
//						break;
//					case "APP":
//						break;
//					case "DISAPP":
//						break;
//					case "MOVE":
//						break;
//					case "DIV":
//						break;
//					default:
//						Tr2dLog.solverlog.warn( "Saved PGraph contained a unknown line: " + line );
//					}
//				} catch ( final InputMismatchException ime ) {
//					Tr2dLog.solverlog.error( String.format( "Saved PGraph currupted (wrong format given): '%s'", line ) );
//					ime.printStackTrace();
//					return false;
//				} catch ( final NoSuchElementException nsee ) {
//					Tr2dLog.solverlog.error( String.format( "Saved PGraph currupted (missing data): '%s'", line ) );
//					nsee.printStackTrace();
//					return false;
//				}
//
//				scanner.close();
//				line = br.readLine();
//			}
			return true;
		}
	}

	public static class Tr2dTrackingProblemResult implements Assignment< IndicatorNode > {

		private final Map< IndicatorNode, Boolean > assignment;

		private final BufferedReader brSolution;

		private final Tr2dTrackingProblem tr2dTraProblem;

		public Tr2dTrackingProblemResult(
				final Tr2dTrackingProblem tr2dTraProblem,
				final File solutionFile ) throws IOException {

			this.assignment = new HashMap<>();

			this.tr2dTraProblem = tr2dTraProblem;
			final FileReader frSolution = new FileReader( solutionFile );
			brSolution = new BufferedReader( frSolution );

			zeroInitializeAssignment();
			importSolution();
		}

		private void zeroInitializeAssignment() {
			assignment.clear();

			final Collection< SegmentNode > segNodes = tr2dTraProblem.getSerializer().getBimapSeg2Id().valuesAs();
			for ( final SegmentNode segmentNode : segNodes ) {
				assignment.put( segmentNode, Boolean.FALSE );
			}

			final Collection< AssignmentNode > assNodes = tr2dTraProblem.getSerializer().getBimapAss2Id().valuesAs();
			for ( final AssignmentNode assignmentNode : assNodes ) {
				assignment.put( assignmentNode, Boolean.FALSE );
			}
		}

		private void importSolution() throws IOException {
			int trueSegs = 0;
			int trueAssmts = 0;

			String line = brSolution.readLine();
			while ( line != null ) {
				if ( line.trim().startsWith( "#" ) || line.trim().equals( "" ) ) {
					line = brSolution.readLine();
					continue;
				}

				final Scanner scanner = new Scanner( line );
				scanner.useDelimiter( "\\s+" );

				try {
					final String type = scanner.next();
					final int id = scanner.nextInt();

					switch ( type ) {
					case "H":
						final SegmentNode segNode = tr2dTraProblem.getSerializer().getBimapSeg2Id().getA( new NodeId( id ) );
						if ( segNode == null ) {
							Tr2dLog.solverlog.warn( String.format( "Segmentation hypothesis with ID %d not found in Seg2Id bimap!", id ) );
							break;
						}
						final Boolean previous = assignment.put( segNode, Boolean.TRUE );
						if ( previous == null )
							Tr2dLog.solverlog.warn(
									String.format( "Seg that was not previously in assignment: %d", id ) );
						trueSegs++;
						break;
					case "APP":
					case "DISAPP":
					case "MOVE":
					case "DIV":
					case "A": // one to rule them all ;)
						final AssignmentNode assNode = tr2dTraProblem.getSerializer().getBimapAss2Id().getA( new NodeId( id ) );
						if ( assNode == null ) {
							Tr2dLog.solverlog.warn( String.format( "Assignment hypothesis with ID %d not found in Ass2Id bimap!", id ) );
							break;
						}
						final Boolean previous2 = assignment.put( assNode, Boolean.TRUE );
						if ( previous2 == null )
							Tr2dLog.solverlog.warn(
									String.format( "Assmnt that was not previously in assignment: %d", id ) );
						trueAssmts++;
						break;
					default:
						Tr2dLog.solverlog.warn( "Solution of external solver contained a unknown line: " + line );
					}
				} catch ( final InputMismatchException ime ) {
					Tr2dLog.solverlog.error( String.format( "External solution currupted (wrong format given): '%s'", line ) );
					ime.printStackTrace();
				} catch ( final NoSuchElementException nsee ) {
					Tr2dLog.solverlog.error( String.format( "External solution currupted (missing data): '%s'", line ) );
					nsee.printStackTrace();
				}

				scanner.close();
				line = brSolution.readLine();
			}

			Tr2dLog.solverlog.info( String.format( "Imported true assignments (seg,ass): %d, %d", trueSegs, trueAssmts ) );
		}

		@Override
		public boolean isAssigned( final IndicatorNode node ) {
			return assignment.containsKey( node );
		}

		@Override
		public int getAssignment( final IndicatorNode node ) {
			return assignment.get( node ) ? 1 : 0;
		}
	}
}
