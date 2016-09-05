/**
 *
 */
package com.indago.tr2d.ui.util;

import org.jfree.util.Log;

import com.indago.fg.Assignment;
import com.indago.fg.MappedFactorGraph;
import com.indago.io.DataMover;
import com.indago.pg.IndicatorNode;
import com.indago.pg.assignments.AppearanceHypothesis;
import com.indago.pg.assignments.DivisionHypothesis;
import com.indago.pg.assignments.MovementHypothesis;
import com.indago.pg.segments.SegmentNode;
import com.indago.tr2d.pg.Tr2dSegmentationProblem;
import com.indago.tr2d.ui.model.Tr2dTrackingModel;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.roi.IterableRegion;
import net.imglib2.roi.Regions;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * @author jug
 */
public class SolutionVisulizer {

	/**
	 *
	 */
	public static RandomAccessibleInterval< IntType > drawSolutionSegmentImages(
			final Tr2dTrackingModel trackingModel,
			final Assignment< IndicatorNode > solution ) {

		final RandomAccessibleInterval< IntType > ret =
				DataMover.createEmptyArrayImgLike( trackingModel.getTr2dModel().getRawData(), new IntType() );

		final MappedFactorGraph mfg = trackingModel.getMappedFactorGraph();
		if ( mfg != null && solution != null ) {
//			final UnaryCostConstraintGraph fg = mfg.getFg();
//			final AssignmentMapper< Variable, IndicatorNode > assMapper = mfg.getAssmntMapper();
//			final Map< IndicatorNode, Variable > varMapper = mfg.getVarmap();

			int time = 0;
			final int curColorId = 1;
			for ( final Tr2dSegmentationProblem segProblem : trackingModel.getTrackingProblem().getTimepoints() ) {
				for ( final SegmentNode segVar : segProblem.getSegments() ) {
					for ( final AppearanceHypothesis app : segVar.getInAssignments().getAppearances() ) {
						if ( solution.getAssignment( app ) == 1 ) { // || time == 0
							drawLineageWithId( ret, solution, time, segVar, curColorId ); // 10 + curColorId
//							curColorId++;
						}
					}
				}
				time++;
			}
		}

		return ret;
	}

	/**
	 * @param segVar
	 * @param curColorId
	 */
	private static void drawLineageWithId(
			final RandomAccessibleInterval< IntType > imgSolution,
			final Assignment< IndicatorNode > solution,
			final int time,
			final SegmentNode segVar,
			final int curColorId ) {

		final IntervalView< IntType > slice = Views.hyperSlice( imgSolution, 2, time );

		if ( solution.getAssignment( segVar ) == 1 ) {
			final int color = curColorId;

//			for ( final DisappearanceHypothesis disapp : segVar.getOutAssignments().getDisappearances() ) {
//				if ( problemSolution.getAssignment( disapp ) == 1 ) {
//					color = 5;
//				}
//			}

			final IterableRegion< ? > region = segVar.getSegment().getRegion();
			final int c = color;
			try {
				Regions.sample( region, slice ).forEach( t -> t.set( c ) );
			} catch ( final ArrayIndexOutOfBoundsException aiaob ) {
				Log.debug( "sol vis bounds exception" );
			}

			for ( final MovementHypothesis move : segVar.getOutAssignments().getMoves() ) {
				if ( solution.getAssignment( move ) == 1 ) {
					drawLineageWithId( imgSolution, solution, time + 1, move.getDest(), curColorId );
				}
			}
			for ( final DivisionHypothesis div : segVar.getOutAssignments().getDivisions() ) {
				if ( solution.getAssignment( div ) == 1 ) {
					drawLineageWithId( imgSolution, solution, time + 1, div.getDest1(), curColorId );
					drawLineageWithId( imgSolution, solution, time + 1, div.getDest2(), curColorId );
				}
			}
		}
	}

}
