package com.indago.tr2d.ilp;

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
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;

import com.indago.fg.Assignment;
import com.indago.pg.IndicatorNode;
import com.indago.pg.assignments.AppearanceHypothesis;
import com.indago.pg.assignments.AssignmentNode;
import com.indago.pg.assignments.DisappearanceHypothesis;
import com.indago.pg.assignments.DivisionHypothesis;
import com.indago.pg.assignments.MovementHypothesis;
import com.indago.pg.segments.ConflictSet;
import com.indago.pg.segments.SegmentNode;
import com.indago.tr2d.Tr2dLog;
import com.indago.tr2d.pg.Tr2dSegmentationProblem;
import com.indago.tr2d.pg.Tr2dTrackingProblem;
import com.indago.util.Bimap;

import net.imglib2.util.ValuePair;

public class SolveExternal {

	File dataExchangeFolder;

	static final String STATUS_NONE = "initialized";
	static final String STATUS_EXPORTING = "exporting";
	static final String STATUS_SOLVING = "waiting for solver";
	static final String STATUS_IMPORTING = "importing";
	static final String STATUS_DONE = "done";
	String status;
	File statusFile;

	private final String expectedSolutionFileName = "solution.sol";

	private ExternalSolverResult pgSolution;
	final Bimap< SegmentNode, ValuePair< Integer, Integer > > bimapSeg2Id;
	final Bimap< AssignmentNode, ValuePair< Integer, Integer > > bimapAss2Id;


	public SolveExternal( final File exchangeFolder ) throws IOException {
		if ( !(exchangeFolder.isDirectory() && exchangeFolder.canWrite()) ) {
			throw new IOException( "Given data exchange folder is not a directory or cannot be written to!" );
		}
		this.dataExchangeFolder = exchangeFolder;

		setStatus( STATUS_NONE );

		bimapSeg2Id = new Bimap<>();
		bimapAss2Id = new Bimap<>();
	}

	private void setStatus( final String statusToSet ) throws IOException {
		if ( statusFile == null ) {
			statusFile = new File( dataExchangeFolder, expectedSolutionFileName );
		}
		if ( statusFile.exists() ) {
			statusFile.delete();
		}
		statusFile = new File( dataExchangeFolder, "status.jug" );
		final BufferedWriter statusWriter = new BufferedWriter( new FileWriter( statusFile ) );
		statusWriter.write( statusToSet );
		statusWriter.close();
	}

	public static Assignment< IndicatorNode > staticSolve(
			final Tr2dTrackingProblem tr2dTraProblem,
			final File exchangeFolder ) throws IOException {
		final SolveExternal solver = new SolveExternal( exchangeFolder );
		return solver.solve( tr2dTraProblem );
	}

	/**
	 * Solves a given factor graph.
	 *
	 * @param tr2dTraProblem
	 *
	 * @param mfg
	 *            the factor graph to be solved.
	 * @return an <code>Assignment</code> containing the solution.
	 */
	public Assignment< IndicatorNode > solve( final Tr2dTrackingProblem tr2dTraProblem ) throws IOException {
		// -------------------------------
		setStatus( STATUS_EXPORTING );
		exportTrackingInstance( tr2dTraProblem );

		// -------------------------------
		setStatus( STATUS_SOLVING );
		final File solutionFile = waitForSolution();

		// -------------------------------
		setStatus( STATUS_IMPORTING );
		pgSolution = new ExternalSolverResult( tr2dTraProblem, bimapSeg2Id, bimapAss2Id, solutionFile );

		// -------------------------------
		setStatus( STATUS_DONE );
		return pgSolution;
	}

	private void exportTrackingInstance( final Tr2dTrackingProblem tr2dTraProblem ) {
		final boolean export_continuation_constraints = false; // currently not desired (agreement with Paul)

		final File exportFile = new File( dataExchangeFolder, "problem.jug" );

		int next_segment_id = -1;
		int next_assignment_id = -1;

		try {
			final SimpleDateFormat sdfDate = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
			final Date now = new Date();
			final String strNow = sdfDate.format( now );

			final BufferedWriter problemWriter = new BufferedWriter( new FileWriter( exportFile ) );
			problemWriter.write( "# Tr2d problem export from " + strNow + "\n" );

			final List< Tr2dSegmentationProblem > timePoints = tr2dTraProblem.getTimepoints();

			problemWriter.write( "\n# === SEGMENT HYPOTHESES =================================================\n" );
			for ( final Tr2dSegmentationProblem t : timePoints ) {
				next_segment_id = -1;
				problemWriter.write( String.format( "\n# t=%d\n", t.getTime() ) );

				// write all segment hypotheses
				final Collection< SegmentNode > segments = t.getSegments();
				for ( final SegmentNode segment : segments ) {
					bimapSeg2Id.add( segment, new ValuePair< Integer, Integer >( t.getTime(), ++next_segment_id ) );
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
						bimapAss2Id.add( app, id );
						writeAppearanceLine( app, id, bimapSeg2Id, problemWriter );
					}
					final Collection< DisappearanceHypothesis > disapps = segment.getOutAssignments().getDisappearances();
					for ( final DisappearanceHypothesis disapp : disapps ) {
						final ValuePair< Integer, Integer > id = new ValuePair<>( t.getTime(), ++next_assignment_id );
						bimapAss2Id.add( disapp, id );
						writeDisappearanceLine( disapp, id, bimapSeg2Id, problemWriter );
					}
					final Collection< MovementHypothesis > moves = segment.getOutAssignments().getMoves();
					for ( final MovementHypothesis move : moves ) {
						final ValuePair< Integer, Integer > id = new ValuePair<>( t.getTime(), ++next_assignment_id );
						bimapAss2Id.add( move, id );
					}
					final Collection< DivisionHypothesis > divs = segment.getOutAssignments().getDivisions();
					for ( final DivisionHypothesis div : divs ) {
						final ValuePair< Integer, Integer > id = new ValuePair<>( t.getTime(), ++next_assignment_id );
						bimapAss2Id.add( div, id );
						writeDivisionLine( div, id, bimapSeg2Id, problemWriter );
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
						final ValuePair< Integer, Integer > timeAndId = bimapSeg2Id.getB( segnode );
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
								final ValuePair< Integer, Integer > timeAndId = bimapAss2Id.getB( ass );
								if ( timeAndId == null ) throw new IllegalStateException( "this should not be possible -- find bug!" );
								leftSegs.add( timeAndId );
							}
							final ValuePair< Integer, Integer > segTimeAndId = bimapSeg2Id.getB( segment );

							// CONT <time> <seg_id> <left_ass_ids as (time, id) pairs...>
							problemWriter.write( String.format( "CONT    %3d %4d ", segTimeAndId.a, segTimeAndId.b ) );
							for ( final ValuePair< Integer, Integer > leftTimeAndId : leftSegs ) {
								problemWriter.write( String.format( "%3d %4d", leftTimeAndId.a, leftTimeAndId.b ) );
							}
							problemWriter.write( "\n" );

							final List< ValuePair< Integer, Integer > > rightSegs = new ArrayList<>();
							for ( final AssignmentNode ass : segment.getOutAssignments().getAllAssignments() ) {
								final ValuePair< Integer, Integer > timeAndId = bimapAss2Id.getB( ass );
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
					.showMessageDialog( null, "Cannot write in selected export folder... cancel export!", "File Error", JOptionPane.ERROR_MESSAGE );
			e.printStackTrace();
		}
	}

	private File waitForSolution() {
		final File solutionFile = new File( dataExchangeFolder, expectedSolutionFileName );
		while ( !solutionFile.exists() ) {
			try {
				TimeUnit.SECONDS.sleep( 1 );
			} catch ( final InterruptedException e ) {}
		}
		return solutionFile;
	}

	/**
	 * Retrieves the energy corresponding to the latest computed solution.
	 *
	 * @return returns latest computed energy, or <code>Double.NaN</code> if not
	 *         applicable.
	 */
	public double getLatestEnergy() {
		return Double.NaN;
	}

	public static class ExternalSolverResult implements Assignment< IndicatorNode > {

		private final Map< IndicatorNode, Boolean > assignment;

		private final File solutionFile;
		private final BufferedReader brSolution;

		private final Tr2dTrackingProblem tr2dTraProblem;
		private final Bimap< SegmentNode, ValuePair< Integer, Integer > > bimapSeg2Id;
		private final Bimap< AssignmentNode, ValuePair< Integer, Integer > > bimapAss2Id;

		public ExternalSolverResult(
				final Tr2dTrackingProblem tr2dTraProblem,
				final Bimap< SegmentNode, ValuePair< Integer, Integer > > mapSeg2Id,
				final Bimap< AssignmentNode, ValuePair< Integer, Integer > > mapAss2Id,
				final File solutionFile ) throws IOException {

			this.assignment = new HashMap<>();

			this.tr2dTraProblem = tr2dTraProblem;
			this.bimapSeg2Id = mapSeg2Id;
			this.bimapAss2Id = mapAss2Id;

			this.solutionFile = solutionFile;
			final FileReader frSolution = new FileReader( solutionFile );
			brSolution = new BufferedReader( frSolution );

			zeroInitializeAssignment();
			importSolution();
		}

		private void zeroInitializeAssignment() {
			assignment.clear();

			final Collection< SegmentNode > segNodes = bimapSeg2Id.valuesAs();
			for ( final SegmentNode segmentNode : segNodes ) {
				assignment.put( segmentNode, Boolean.FALSE );
			}

			final Collection< AssignmentNode > assNodes = bimapAss2Id.valuesAs();
			for ( final AssignmentNode assignmentNode : assNodes ) {
				assignment.put( assignmentNode, Boolean.FALSE );
			}
		}

		private void importSolution() throws IOException {
			String line = brSolution.readLine();
			while ( line != null ) {
				final Scanner scanner = new Scanner( line );
				scanner.useDelimiter( " " );

				try {
					final String type = scanner.next();

					final int t = scanner.nextInt();
					final int id = scanner.nextInt();

					switch ( type ) {
					case "H":
						final SegmentNode segNode = bimapSeg2Id.getA( new ValuePair<>( t, id ) );
						if ( segNode != null ) assignment.put( segNode, Boolean.TRUE );
						break;
					case "APP":
					case "DISAPP":
					case "MOVE":
					case "DIV":
						final AssignmentNode assNode = bimapAss2Id.getA( new ValuePair<>( t, id ) );
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
}
