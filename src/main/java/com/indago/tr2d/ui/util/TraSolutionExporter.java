/**
 *
 */
package com.indago.tr2d.ui.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.indago.fg.Assignment;
import com.indago.fg.MappedFactorGraph;
import com.indago.io.DataMover;
import com.indago.pg.IndicatorNode;
import com.indago.pg.assignments.AppearanceHypothesis;
import com.indago.pg.assignments.DisappearanceHypothesis;
import com.indago.pg.assignments.DivisionHypothesis;
import com.indago.pg.assignments.MovementHypothesis;
import com.indago.pg.segments.SegmentNode;
import com.indago.tr2d.Tr2dLog;
import com.indago.tr2d.pg.Tr2dSegmentationProblem;
import com.indago.tr2d.ui.model.Tr2dTrackingModel;

import ij.IJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.roi.IterableRegion;
import net.imglib2.roi.Regions;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;


/**
 * @author mangalp, jug
 */
public class TraSolutionExporter {

	private static class TraLineData {

		private int id = -1;
		private int tStart = -1;
		private int tEnd = -1;
		private int parentId = 0;

		public TraLineData( int id, int t ) {
			this.id = id;
			this.tStart = t;
		}

		public int getEnd() {
			return tEnd;
		}

		public void setEnd( int t ) {
			this.tEnd = t;
		}

		public void setParentId( int id ) {
			this.parentId = id;
		}

		public String getLineToWrite() {
			return String.format( "%d %d %d %d", id, tStart, tEnd, parentId );
		}
	}

	private static Map< Integer, TraLineData > trackletInfo;

	public static void exportTraData(
			final Tr2dTrackingModel trackingModel,
			final Assignment< IndicatorNode > solution,
			File projectFolderBasePath ) throws IOException {

		trackletInfo = new HashMap<>();

		//create text file to write TRA info into
		final File exportFile = new File( projectFolderBasePath.getAbsolutePath(), "res_track.txt" );

		//call collectTraData
		RandomAccessibleInterval< IntType > traImages = collectTraData( trackingModel, solution );
		for ( int image = 0; image < traImages.dimension( 2 ); image++ ) {
			IntervalView< IntType > res = Views.hyperSlice( traImages, 2, image );
			IJ.save(
					ImageJFunctions.wrap( res, "tracking solution" ).duplicate(),
					projectFolderBasePath.getAbsolutePath() + "/mask" + String
							.format( "%03d", image ) + ".tif" );
		}

		for ( int i = 1; i < Collections.max( trackletInfo.keySet() ) + 1; i++ ) {
			if ( trackletInfo.get( i ).getEnd() == -1 ) {
				trackletInfo.get( i ).setEnd( ( int ) trackingModel.getTr2dModel().getRawData().dimension( 2 ) - 1 );
			}
		}

		//write all lines in textfile
		FileWriter fw = new FileWriter( projectFolderBasePath.getAbsolutePath() + "/res_track.txt" );
		final BufferedWriter problemWriter = new BufferedWriter( fw );
		for ( int id = 1; id < Collections.max( trackletInfo.keySet() ) + 1; id++ ) {

			problemWriter.write(
					trackletInfo.get( id ).getLineToWrite() );
			problemWriter.newLine();
		}
		problemWriter.close();
	}

	private static RandomAccessibleInterval< IntType > collectTraData(
			final Tr2dTrackingModel trackingModel,
			final Assignment< IndicatorNode > solution ) {

		final RandomAccessibleInterval< IntType > ret =
				DataMover.createEmptyArrayImgLike( trackingModel.getTr2dModel().getRawData(), new IntType() );

		final MappedFactorGraph mfg = trackingModel.getMappedFactorGraph();
		if ( mfg != null && solution != null ) {
			int time = 0;

			int curId = 1;
			for ( final Tr2dSegmentationProblem segProblem : trackingModel.getTrackingProblem().getTimepoints() ) {
				for ( final SegmentNode segVar : segProblem.getSegments() ) {
					for ( final AppearanceHypothesis app : segVar.getInAssignments().getAppearances() ) {
						if ( solution.getAssignment( app ) == 1 ) { // time == 0
							trackletInfo.put( curId, new TraLineData( curId, time ) );
							curId = collectLineageData( ret, solution, time, segVar, curId );
							curId++;
						}
					}
				}
				time++;
			}
		}

		return ret;
	}

	private static int collectLineageData(
			final RandomAccessibleInterval< IntType > imgSolution,
			final Assignment< IndicatorNode > solution,
			final int time,
			final SegmentNode segVar,
			int curId ) {

		final IntervalView< IntType > slice = Views.hyperSlice( imgSolution, 2, time );

		if ( solution.getAssignment( segVar ) == 1 ) {
			final int color = curId;

			final IterableRegion< ? > region = segVar.getSegment().getRegion();
			final int c = color;
			try {
				Regions.sample( region, slice ).forEach( t -> t.set( c ) );
			} catch ( final ArrayIndexOutOfBoundsException aiaob ) {
				Tr2dLog.log.debug( "sol vis bounds exception" );
			}

			for ( final MovementHypothesis move : segVar.getOutAssignments().getMoves() ) {
				if ( solution.getAssignment( move ) == 1 ) {
					curId = collectLineageData( imgSolution, solution, time + 1, move.getDest(), curId );
				}
			}
			for ( final DivisionHypothesis div : segVar.getOutAssignments().getDivisions() ) {
				if ( solution.getAssignment( div ) == 1 ) {
					trackletInfo.get( curId ).setEnd( time );
					int parentId = curId;

					curId++;
					TraLineData child = new TraLineData( curId, time + 1 );
					child.setParentId( parentId );
					trackletInfo.put( curId, child );
					curId = collectLineageData( imgSolution, solution, time + 1, div.getDest1(), curId );

					curId++;
					child = new TraLineData( curId, time + 1 );
					child.setParentId( parentId );
					trackletInfo.put( curId, child );
					curId = collectLineageData( imgSolution, solution, time + 1, div.getDest2(), curId );
				}
			}
			for ( final DisappearanceHypothesis disappear : segVar.getOutAssignments().getDisappearances() ) {
				if ( solution.getAssignment( disappear ) == 1 ) {
					trackletInfo.get( curId ).setEnd( time );
				}
			}
		}

		return curId;
	}

}
