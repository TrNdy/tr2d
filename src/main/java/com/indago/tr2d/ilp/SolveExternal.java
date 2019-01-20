package com.indago.tr2d.ilp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.indago.fg.Assignment;
import com.indago.pg.IndicatorNode;
import com.indago.tr2d.pg.Tr2dTrackingProblem;
import com.indago.tr2d.pg.Tr2dTrackingProblem.Tr2dTrackingProblemResult;

public class SolveExternal {

	File dataExchangeFolder;

	static final String STATUS_NONE = "initialized";
	static final String STATUS_EXPORTING = "exporting";
	static final String STATUS_SOLVING = "waiting for solver";
	static final String STATUS_IMPORTING = "importing";
	static final String STATUS_DONE = "done";
	String status;
	File statusFile;

	private final String expectedSolutionFileName = "tracking.sol";

	private Tr2dTrackingProblemResult pgSolution;


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
		final File exportFile = new File( dataExchangeFolder, "problem.jug" );
		tr2dTraProblem.getSerializer().savePgraph( tr2dTraProblem, exportFile );

		// -------------------------------
		setStatus( STATUS_SOLVING );
		final File solutionFile = waitForSolution();

		// -------------------------------
		setStatus( STATUS_IMPORTING );
		pgSolution = new Tr2dTrackingProblemResult( tr2dTraProblem, solutionFile );

		// -------------------------------
		setStatus( STATUS_DONE );
		return pgSolution;
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
}
