package com.indago.tr2d.ilp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;

import com.indago.fg.Assignment;
import com.indago.fg.Variable;
import com.indago.pg.assignments.AppearanceHypothesis;
import com.indago.pg.assignments.AssignmentNode;
import com.indago.pg.assignments.DisappearanceHypothesis;
import com.indago.pg.assignments.DivisionHypothesis;
import com.indago.pg.assignments.MovementHypothesis;
import com.indago.pg.segments.ConflictSet;
import com.indago.pg.segments.SegmentNode;
import com.indago.tr2d.pg.Tr2dSegmentationProblem;
import com.indago.tr2d.pg.Tr2dTrackingProblem;

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

	public SolveExternal( final File exchangeFolder ) throws IOException {
		if ( !(exchangeFolder.isDirectory() && exchangeFolder.canWrite()) ) {
			throw new IOException( "Given data exchange folder is not a directory or cannot be written to!" );
		}
		this.dataExchangeFolder = exchangeFolder;

		setStatus( STATUS_NONE );
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

	public static Assignment< Variable > staticSolve(
			final Tr2dTrackingProblem tr2dTraProblem,
			final File exchangeFolder ) throws IOException {
		final SolveExternal solver = new SolveExternal( exchangeFolder );
		final Assignment< Variable > assignment = solver.solve( tr2dTraProblem );
		return assignment;
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
	public Assignment< Variable > solve( final Tr2dTrackingProblem tr2dTraProblem ) throws IOException {
		// -------------------------------
		setStatus( STATUS_EXPORTING );
		exportTrackingInstance( tr2dTraProblem );

		// -------------------------------
		setStatus( STATUS_SOLVING );
		waitForSolution();

		// -------------------------------
		setStatus( STATUS_IMPORTING );
		importSolution();

		// -------------------------------
		setStatus( STATUS_DONE );
		return null;
	}

	private void exportTrackingInstance( final Tr2dTrackingProblem tr2dTraProblem ) {
		final boolean export_continuation_constraints = false; // currently not desired (agreement with Paul)

		final File exportFile = new File( dataExchangeFolder, "problem.jug" );

		final Map< SegmentNode, ValuePair< Integer, Integer > > mapSeg2Id = new HashMap<>();
		int next_segment_id = -1;
		final Map< AssignmentNode, ValuePair< Integer, Integer > > mapAss2Id = new HashMap<>();
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
					mapSeg2Id.put( segment, new ValuePair< Integer, Integer >( t.getTime(), ++next_segment_id ) );
					final String shortName = writeSegmentLine( t.getTime(), segment, next_segment_id, problemWriter );
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
						mapAss2Id.put( app, new ValuePair<>( t.getTime(), ++next_assignment_id ) );
						final String shortName = writeAppearanceLine( app, mapSeg2Id, problemWriter );
					}
					final Collection< DisappearanceHypothesis > disapps = segment.getOutAssignments().getDisappearances();
					for ( final DisappearanceHypothesis disapp : disapps ) {
						mapAss2Id.put( disapp, new ValuePair<>( t.getTime(), ++next_assignment_id ) );
						final String shortName = writeDisappearanceLine( disapp, mapSeg2Id, problemWriter );
					}
					final Collection< MovementHypothesis > moves = segment.getOutAssignments().getMoves();
					for ( final MovementHypothesis move : moves ) {
						mapAss2Id.put( move, new ValuePair<>( t.getTime(), ++next_assignment_id ) );
						final String shortName = writeMovementLine( move, mapSeg2Id, problemWriter );
					}
					final Collection< DivisionHypothesis > divs = segment.getOutAssignments().getDivisions();
					for ( final DivisionHypothesis div : divs ) {
						mapAss2Id.put( div, new ValuePair<>( t.getTime(), ++next_assignment_id ) );
						final String shortName = writeDivisionLine( div, mapSeg2Id, problemWriter );
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
						final ValuePair< Integer, Integer > timeAndId = mapSeg2Id.get( segnode );
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
								final ValuePair< Integer, Integer > timeAndId = mapAss2Id.get( ass );
								if ( timeAndId == null ) throw new IllegalStateException( "this should not be possible -- find bug!" );
								leftSegs.add( timeAndId );
							}
							final ValuePair< Integer, Integer > segTimeAndId = mapSeg2Id.get( segment );

							// CONT <time> <seg_id> <left_ass_ids as (time, id) pairs...>
							problemWriter.write( String.format( "CONT    %3d %4d ", segTimeAndId.a, segTimeAndId.b ) );
							for ( final ValuePair< Integer, Integer > leftTimeAndId : leftSegs ) {
								problemWriter.write( String.format( "%3d %4d", leftTimeAndId.a, leftTimeAndId.b ) );
							}
							problemWriter.write( "\n" );

							final List< ValuePair< Integer, Integer > > rightSegs = new ArrayList<>();
							for ( final AssignmentNode ass : segment.getOutAssignments().getAllAssignments() ) {
								final ValuePair< Integer, Integer > timeAndId = mapAss2Id.get( ass );
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

	private void waitForSolution() {
		final File solutionFile = new File( dataExchangeFolder, expectedSolutionFileName );
		while ( !solutionFile.exists() ) {
			try {
				TimeUnit.SECONDS.sleep( 1 );
			} catch ( final InterruptedException e ) {}
		}
	}

	private void importSolution() {
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

	public static class ExternelSolverResult implements Assignment< Variable > {

		public ExternelSolverResult() {
		}

		@Override
		public boolean isAssigned( final Variable var ) {
			return false;
		}

		@Override
		public int getAssignment( final Variable var ) {
			return -1;
		}
	}

	private String writeSegmentLine( final int t, final SegmentNode segment, final int id, final BufferedWriter writer ) throws IOException {
		// H <id> <cost> <com_x_pos> <com_y_pos>
		writer.write(
				String.format(
						"H %3d %4d %.16f (%.1f,%.1f)\n",
						t,
						id,
						segment.getCost(),
						segment.getSegment().getCenterOfMass().getFloatPosition( 0 ),
						segment.getSegment().getCenterOfMass().getFloatPosition( 1 ) ) );

		return String.format(
				"H-%d/%d",
				t,
				id );

	}

	private String writeAppearanceLine(
			final AppearanceHypothesis app,
			final Map< SegmentNode, ValuePair< Integer, Integer > > mapSeg2Id,
			final BufferedWriter writer )
			throws IOException {
		// APP <time> <segment_id> <cost>
		final ValuePair< Integer, Integer > timeAndId = mapSeg2Id.get( app.getDest() );
		writer.write(
				String.format(
						"APP     %3d %4d %.16f\n",
						timeAndId.a,
						timeAndId.b,
						app.getCost() ) );
		return String.format(
				"APP-%d/%d",
				timeAndId.a,
				timeAndId.b );
	}

	private String writeDisappearanceLine(
			final DisappearanceHypothesis disapp,
			final Map< SegmentNode, ValuePair< Integer, Integer > > mapSeg2Id,
			final BufferedWriter writer )
			throws IOException {
		// DISAPP <time> <segment_id> <cost>
		final ValuePair< Integer, Integer > timeAndId = mapSeg2Id.get( disapp.getSrc() );
		writer.write(
				String.format(
						"DISAPP  %3d %4d %.16f\n",
						timeAndId.a,
						timeAndId.b,
						disapp.getCost() ) );
		return String.format(
				"DISAPP-%d/%d",
				timeAndId.a,
				timeAndId.b );
	}

	private String writeMovementLine(
			final MovementHypothesis move,
			final Map< SegmentNode, ValuePair< Integer, Integer > > mapSeg2Id,
			final BufferedWriter writer )
			throws IOException {
		// MOVE <ass_id> <source_time> <source_segment_id> <dest_time> <dest_segment_id> <cost>
		final ValuePair< Integer, Integer > timeAndId4Src = mapSeg2Id.get( move.getSrc() );
		final ValuePair< Integer, Integer > timeAndId4Dest = mapSeg2Id.get( move.getDest() );
		writer.write(
				String.format(
						"MOVE    %3d %4d %3d %4d %.16f\n",
						timeAndId4Src.a,
						timeAndId4Src.b,
						timeAndId4Dest.a,
						timeAndId4Dest.b,
						move.getCost() ) );
		return String.format(
				"MOVE-%d/%d-%d/%d",
				timeAndId4Src.a,
				timeAndId4Src.b,
				timeAndId4Dest.a,
				timeAndId4Dest.b );
	}

	private String writeDivisionLine(
			final DivisionHypothesis div,
			final Map< SegmentNode, ValuePair< Integer, Integer > > mapSeg2Id,
			final BufferedWriter writer )
			throws IOException {
		// DIV <ass_id> <source_segment_id> <dest1_segment_id> <dest2_segment_id> <cost>
		final ValuePair< Integer, Integer > timeAndId4Src = mapSeg2Id.get( div.getSrc() );
		final ValuePair< Integer, Integer > timeAndId4Dest1 = mapSeg2Id.get( div.getDest1() );
		final ValuePair< Integer, Integer > timeAndId4Dest2 = mapSeg2Id.get( div.getDest2() );
		writer.write(
				String.format(
						"DIV     %3d %4d %3d %4d %3d %4d %.16f\n",
						timeAndId4Src.a,
						timeAndId4Src.b,
						timeAndId4Dest1.a,
						timeAndId4Dest1.b,
						timeAndId4Dest2.a,
						timeAndId4Dest2.b,
						div.getCost() ) );
		return String.format(
				"DIV-%d/%d-%d/%d-%d/%d",
				timeAndId4Src.a,
				timeAndId4Src.b,
				timeAndId4Dest1.a,
				timeAndId4Dest1.b,
				timeAndId4Dest2.a,
				timeAndId4Dest2.b );
	}
}
