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
import java.util.Date;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Scanner;

import javax.swing.JOptionPane;

import com.indago.costs.CostFactory;
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
import com.indago.tr2d.ui.model.Tr2dFlowModel;
import com.indago.tr2d.ui.model.Tr2dTrackingModel;
import com.indago.util.Bimap;

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

	/**
	 * Saves the problem graph to file
	 *
	 * @param file
	 *            the file to save to
	 */
	public void saveToFile( final File exportFile ) throws IOException {
		this.serializer.save( this, exportFile );
	}

	public Tr2dTrackingProblemSerializer getSerializer() {
		return serializer;
	}

	public static class Tr2dTrackingProblemSerializer {

		private final Bimap< SegmentNode, ValuePair< Integer, Integer > > bimapSeg2Id;
		private final Bimap< AssignmentNode, ValuePair< Integer, Integer > > bimapAss2Id;
		private Map< IndicatorNode, Boolean > assignment;

		public Tr2dTrackingProblemSerializer() {
			bimapSeg2Id = new Bimap<>();
			bimapAss2Id = new Bimap<>();
		}

		public void save( final Tr2dTrackingProblem ttp, final File file ) throws IOException {
			final boolean export_continuation_constraints = false; // currently not desired (agreement with Paul)

			int next_segment_id = -1;
			int next_assignment_id = -1;

			try {
				final SimpleDateFormat sdfDate = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
				final Date now = new Date();
				final String strNow = sdfDate.format( now );

				final BufferedWriter problemWriter = new BufferedWriter( new FileWriter( file ) );
				problemWriter.write( "# Tr2d problem export from " + strNow + "\n" );

				final List< Tr2dSegmentationProblem > timePoints = ttp.getTimepoints();

				problemWriter.write( "\n# === SEGMENT HYPOTHESES =================================================\n" );
				for ( final Tr2dSegmentationProblem t : timePoints ) {
					next_segment_id = -1;
					problemWriter.write( String.format( "\n# t=%d\n", t.getTime() ) );

					// write all segment hypotheses
					final Collection< SegmentNode > segments = t.getSegments();
					for ( final SegmentNode segment : segments ) {
						getBimapSeg2Id().add( segment, new ValuePair< Integer, Integer >( t.getTime(), ++next_segment_id ) );
						writeSegmentLine( t.getTime(), segment, next_segment_id, problemWriter );
					}
				}

				problemWriter.write( "\n# === ASSIGNMNETS ========================================================\n\n" );
				for ( final Tr2dSegmentationProblem t : timePoints ) {
					next_assignment_id = -1;
					final Collection< SegmentNode > segments = t.getSegments();
					// for each segmetn hyp, write all assignments
					for ( final SegmentNode segment : segments ) {

						final Collection< AppearanceHypothesis > apps = segment.getInAssignments().getAppearances();
						for ( final AppearanceHypothesis app : apps ) {
							final ValuePair< Integer, Integer > id = new ValuePair<>( t.getTime(), ++next_assignment_id );
							getBimapAss2Id().add( app, id );
							writeAppearanceLine( app, id, getBimapSeg2Id(), problemWriter );
						}
						final Collection< DisappearanceHypothesis > disapps = segment.getOutAssignments().getDisappearances();
						for ( final DisappearanceHypothesis disapp : disapps ) {
							final ValuePair< Integer, Integer > id = new ValuePair<>( t.getTime(), ++next_assignment_id );
							getBimapAss2Id().add( disapp, id );
							writeDisappearanceLine( disapp, id, getBimapSeg2Id(), problemWriter );
						}
						final Collection< MovementHypothesis > moves = segment.getOutAssignments().getMoves();
						for ( final MovementHypothesis move : moves ) {
							final ValuePair< Integer, Integer > id = new ValuePair<>( t.getTime(), ++next_assignment_id );
							getBimapAss2Id().add( move, id );
						}
						final Collection< DivisionHypothesis > divs = segment.getOutAssignments().getDivisions();
						for ( final DivisionHypothesis div : divs ) {
							final ValuePair< Integer, Integer > id = new ValuePair<>( t.getTime(), ++next_assignment_id );
							getBimapAss2Id().add( div, id );
							writeDivisionLine( div, id, getBimapSeg2Id(), problemWriter );
						}
						problemWriter.write( "\n" );
					}
				}

				problemWriter.write( "# === CONSTRAINTS ========================================================\n\n" );
				for ( final Tr2dSegmentationProblem t : timePoints ) {
					for ( final ConflictSet cs : t.getConflictSets() ) {
						final List< ValuePair< Integer, Integer > > timeAndIdPairs = new ArrayList<>();
						final Iterator< SegmentNode > it = cs.iterator();
						while ( it.hasNext() ) {
							final SegmentNode segnode = it.next();
							final ValuePair< Integer, Integer > timeAndId = getBimapSeg2Id().getB( segnode );
							if ( timeAndId == null ) throw new IllegalStateException( "this should not be possible -- find bug!" );
							timeAndIdPairs.add( timeAndId );
						}
						// CONFSET <t id...>
						problemWriter.write( "CONFSET " );
						boolean first = true;
						for ( final ValuePair< Integer, Integer > timeAndId : timeAndIdPairs ) {
							if ( !first ) problemWriter.write( " + " );
							problemWriter.write( String.format( "%3d %4d ", timeAndId.a, timeAndId.b ) );
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
								final List< ValuePair< Integer, Integer > > leftSegs = new ArrayList<>();
								for ( final AssignmentNode ass : segment.getInAssignments().getAllAssignments() ) {
									final ValuePair< Integer, Integer > timeAndId = getBimapAss2Id().getB( ass );
									if ( timeAndId == null ) throw new IllegalStateException( "this should not be possible -- find bug!" );
									leftSegs.add( timeAndId );
								}
								final ValuePair< Integer, Integer > segTimeAndId = getBimapSeg2Id().getB( segment );

								// CONT <time> <seg_id> <left_ass_ids as (time, id) pairs...>
								problemWriter.write( String.format( "CONT    %3d %4d ", segTimeAndId.a, segTimeAndId.b ) );
								for ( final ValuePair< Integer, Integer > leftTimeAndId : leftSegs ) {
									problemWriter.write( String.format( "%3d %4d", leftTimeAndId.a, leftTimeAndId.b ) );
								}
								problemWriter.write( "\n" );

								final List< ValuePair< Integer, Integer > > rightSegs = new ArrayList<>();
								for ( final AssignmentNode ass : segment.getOutAssignments().getAllAssignments() ) {
									final ValuePair< Integer, Integer > timeAndId = getBimapAss2Id().getB( ass );
									if ( timeAndId == null ) throw new IllegalStateException( "this should not be possible -- find bug!" );
									rightSegs.add( timeAndId );
								}
								// CONT <time> <seg_id> <right_ass_ids as (time, id) pairs...>
								problemWriter.write( String.format( "CONT    %3d %4d ", segTimeAndId.a, segTimeAndId.b ) );
								for ( final ValuePair< Integer, Integer > rightTimeAndId : rightSegs ) {
									problemWriter.write( String.format( "%3d %4d", rightTimeAndId.a, rightTimeAndId.b ) );
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

		public void saveSolution( final Assignment< IndicatorNode > pgAssignment, final File file ) {
			try {
				final BufferedWriter solutionWriter = new BufferedWriter( new FileWriter( file ) );

				final SimpleDateFormat sdfDate = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
				final Date now = new Date();
				final String strNow = sdfDate.format( now );
				solutionWriter.write( "# Tr2d solution export from " + strNow + "\n" );
				solutionWriter.write( "# -------------------------------------------\n\n" );

				final Collection< SegmentNode > segNodes = this.bimapSeg2Id.valuesAs();
				for ( final SegmentNode segmentNode : segNodes ) {
					if ( pgAssignment.getAssignment( segmentNode ) == 1 ) {
						final ValuePair< Integer, Integer > key = this.bimapSeg2Id.getB( segmentNode );
						solutionWriter.write( String.format( "H %3d %4d\n", key.a, key.b ) );
					}
				}
				final Collection< AssignmentNode > assNodes = this.bimapAss2Id.valuesAs();
				for ( final AssignmentNode assignmentNode : assNodes ) {
					if ( pgAssignment.getAssignment( assignmentNode ) == 1 ) {
						final ValuePair< Integer, Integer > key = this.bimapAss2Id.getB( assignmentNode );
						solutionWriter.write( String.format( "A %3d %4d\n", key.a, key.b ) );
					}
				}

				solutionWriter.close();
			} catch ( final IOException ioe ) {
				Tr2dLog.solverlog.error( "Problem Graph solution could not be stored to " + file.getAbsolutePath() );
				ioe.printStackTrace();
			}
		}

		private void writeSegmentLine( final int t, final SegmentNode segment, final int id, final BufferedWriter writer ) throws IOException {
			// H <id> <cost> <com_x_pos> <com_y_pos>
			writer.write(
					String.format(
							"H %3d %4d %.16f (%.1f,%.1f)\n",
							t,
							id,
							segment.getCost(),
							segment.getSegment().getCenterOfMass().getFloatPosition( 0 ),
							segment.getSegment().getCenterOfMass().getFloatPosition( 1 ) ) );
		}

		private void writeAppearanceLine(
				final AppearanceHypothesis app,
				final ValuePair< Integer, Integer > segid,
				final Bimap< SegmentNode, ValuePair< Integer, Integer > > bimapSeg2Id,
				final BufferedWriter writer )
				throws IOException {
			// APP <time> <segment_id> <cost>
			final int srcId = bimapSeg2Id.getB( app.getDest() ).getB();
			writer.write(
					String.format(
							"APP     %3d %4d   %4d %.16f\n",
							segid.a,
							segid.b,
							srcId,
							app.getCost() ) );
		}

		private void writeDisappearanceLine(
				final DisappearanceHypothesis disapp,
				final ValuePair< Integer, Integer > assid,
				final Bimap< SegmentNode, ValuePair< Integer, Integer > > bimapSeg2Id,
				final BufferedWriter writer )
				throws IOException {
			// DISAPP <time> <segment_id> <cost>
			final int srcId = bimapSeg2Id.getB( disapp.getSrc() ).getB();
			writer.write(
					String.format(
							"DISAPP  %3d %4d   %4d %.16f\n",
							assid.a,
							assid.b,
							srcId,
							disapp.getCost() ) );
		}

		private void writeMovementLine(
				final MovementHypothesis move,
				final ValuePair< Integer, Integer > assid,
				final Bimap< SegmentNode, ValuePair< Integer, Integer > > bimapSeg2Id,
				final BufferedWriter writer )
				throws IOException {
			// MOVE <ass_id> <source_time> <source_segment_id> <dest_time> <dest_segment_id> <cost>
			final int srcId = bimapSeg2Id.getB( move.getSrc() ).getB();
			final ValuePair< Integer, Integer > timeAndId4Dest = bimapSeg2Id.getB( move.getDest() );
			writer.write(
					String.format(
							"MOVE    %3d %4d   %4d %3d %4d %.16f\n",
							assid.a,
							assid.b,
							srcId,
							timeAndId4Dest.a,
							timeAndId4Dest.b,
							move.getCost() ) );
		}

		private void writeDivisionLine(
				final DivisionHypothesis div,
				final ValuePair< Integer, Integer > assid,
				final Bimap< SegmentNode, ValuePair< Integer, Integer > > bimapSeg2Id,
				final BufferedWriter writer )
				throws IOException {
			// DIV <ass_id> <source_segment_id> <dest1_segment_id> <dest2_segment_id> <cost>
			final int srcId = bimapSeg2Id.getB( div.getSrc() ).getB();
			final ValuePair< Integer, Integer > timeAndId4Dest1 = bimapSeg2Id.getB( div.getDest1() );
			final ValuePair< Integer, Integer > timeAndId4Dest2 = bimapSeg2Id.getB( div.getDest2() );
			writer.write(
					String.format(
							"DIV     %3d %4d   %4d %3d %4d %3d %4d %.16f\n",
							assid.a,
							assid.b,
							srcId,
							timeAndId4Dest1.a,
							timeAndId4Dest1.b,
							timeAndId4Dest2.a,
							timeAndId4Dest2.b,
							div.getCost() ) );
		}

		public Bimap< SegmentNode, ValuePair< Integer, Integer > > getBimapSeg2Id() {
			return bimapSeg2Id;
		}

		public Bimap< AssignmentNode, ValuePair< Integer, Integer > > getBimapAss2Id() {
			return bimapAss2Id;
		}
	}

	public static class Tr2dTrackingProblemResult implements Assignment< IndicatorNode > {

		private final Map< IndicatorNode, Boolean > assignment;

		private final File solutionFile;
		private final BufferedReader brSolution;

		private final Tr2dTrackingProblem tr2dTraProblem;

		public Tr2dTrackingProblemResult(
				final Tr2dTrackingProblem tr2dTraProblem,
				final File solutionFile ) throws IOException {

			this.assignment = new HashMap<>();

			this.tr2dTraProblem = tr2dTraProblem;
			this.solutionFile = solutionFile;
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
			String line = brSolution.readLine();
			while ( line != null ) {
				if ( line.trim().startsWith( "#" ) || line.trim().equals( "" ) ) {
					line = brSolution.readLine();
					continue;
				}

				final Scanner scanner = new Scanner( line );
				scanner.useDelimiter( "\\s*" );

				try {
					final String type = scanner.next();

					final int t = scanner.nextInt();
					final int id = scanner.nextInt();

					switch ( type ) {
					case "H":
						final SegmentNode segNode = tr2dTraProblem.getSerializer().getBimapSeg2Id().getA( new ValuePair<>( t, id ) );
						if ( segNode != null ) assignment.put( segNode, Boolean.TRUE );
						break;
					case "APP":
					case "DISAPP":
					case "MOVE":
					case "DIV":
					case "A":
						final AssignmentNode assNode = tr2dTraProblem.getSerializer().getBimapAss2Id().getA( new ValuePair<>( t, id ) );
						if ( assNode != null ) assignment.put( assNode, Boolean.TRUE );
						break;
					default:
						Tr2dLog.solverlog.warn( "Solution of external solver contained a unknown line: " + line );
					}
				} catch ( final InputMismatchException ime ) {
					Tr2dLog.solverlog.error( "External solution currupted (wrong format given): " + line );
					ime.printStackTrace();
				} catch ( final NoSuchElementException nsee ) {
					Tr2dLog.solverlog.error( "External solution currupted (missing data): " + line );
					nsee.printStackTrace();
				}

				scanner.close();
				line = brSolution.readLine();
			}
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
