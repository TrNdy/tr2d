/**
 *
 */
package com.indago.tr2d.ui.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.indago.fg.Assignment;
import com.indago.fg.MappedFactorGraph;
import com.indago.pg.IndicatorNode;
import com.indago.pg.assignments.AppearanceHypothesis;
import com.indago.pg.assignments.DisappearanceHypothesis;
import com.indago.pg.assignments.DivisionHypothesis;
import com.indago.pg.assignments.MovementHypothesis;
import com.indago.pg.segments.SegmentNode;
import com.indago.tr2d.pg.Tr2dSegmentationProblem;
import com.indago.tr2d.ui.model.Tr2dTrackingModel;
import com.indago.util.Bimap;

/**
 * @author jug
 */
public class SolutionExporter {

	public static class Tracklet {

		static int nextId = 0;

		private final int id;
		private int parent = -1;
		private int child1 = -1;
		private int child2 = -1;
		private final int time;
		private final List< Integer > objIds = new ArrayList< Integer >();

		public Tracklet( final int startTime, final int objId ) {
			this.id = nextId;
			nextId++;

			this.time = startTime;
			objIds.add( objId );
		}

		public int getTrackletId() {
			return id;
		}

		public int getStartTime() {
			return time;
		}

		public List< Integer > getObjectIds() {
			return objIds;
		}

		public void setParentId( final int parentId ) {
			this.parent = parentId;
		}

		public void setChild1( final int childId ) {
			this.child1 = childId;
		}

		public void setChild2( final int childId ) {
			this.child2 = childId;
		}

		public int getParentId() {
			return parent;
		}

		public int getChild1() {
			return child1;
		}

		public int getChild2() {
			return child2;
		}

		public void add( final int objId ) {
			this.objIds.add( objId );
		}
	}

	private final Tr2dTrackingModel model;
	private final Assignment< IndicatorNode > solution;

	private final Map< Integer, Bimap< Integer, SegmentNode > > mapTime2Segments;
	private final List< Tracklet > tracklets = new ArrayList<>();

	public SolutionExporter(
			final Tr2dTrackingModel trackingModel,
			final Assignment< IndicatorNode > solution ) {
		this.model = trackingModel;
		this.solution = solution;

		mapTime2Segments = new HashMap<>();

		extractSolution();
	}

	private void extractSolution() {
		final MappedFactorGraph mfg = model.getMappedFactorGraph();
		if ( mfg != null && solution != null ) {

			// get all segmented objects in all time-points
			// --------------------------------------------
			int time = 0;
			for ( final Tr2dSegmentationProblem segProblem : model.getTrackingProblem().getTimepoints() ) {

				final Bimap<Integer, SegmentNode> id2seg = new Bimap<>();
				mapTime2Segments.put( time, id2seg);

				int objId = 0;
				for ( final SegmentNode segVar : segProblem.getSegments() ) {
					if ( solution.getAssignment( segVar ) == 1 ) {
						id2seg.add( objId, segVar );
						objId++;
					}
				}
				time++;
			}

			// get all tracklets
			// -----------------
			time = 0;
			for ( final Tr2dSegmentationProblem segProblem : model.getTrackingProblem().getTimepoints() ) {
				for ( final SegmentNode segVar : segProblem.getSegments() ) {
					for ( final AppearanceHypothesis app : segVar.getInAssignments().getAppearances() ) {
						final Bimap< Integer, SegmentNode > bimap = mapTime2Segments.get( time );
						if ( solution.getAssignment( app ) == 1 ) {
							final Integer objId = bimap.getA( segVar );
							if ( objId != null ) {
								buildLineageTracklets( time, objId );
							}
						}
					}
				}
				time++;
			}
		}
	}

	/**
	 * @param time
	 * @param objId
	 */
	private void buildLineageTracklets( final int time, final Integer objId ) {
		final Bimap< Integer, SegmentNode > bimapT = mapTime2Segments.get( time );
		final Bimap< Integer, SegmentNode > bimapTp1 = mapTime2Segments.get( time+1 );
		final SegmentNode segVar = bimapT.getB( objId );

		if ( solution.getAssignment( segVar ) == 1 ) {

			final Tracklet tracklet = new Tracklet( time, objId );
			tracklets.add( tracklet );

			for ( final DisappearanceHypothesis disapp : segVar.getOutAssignments().getDisappearances() ) {
				if ( solution.getAssignment( disapp ) == 1 ) {
					return;
				}
			}
			for ( final MovementHypothesis move : segVar.getOutAssignments().getMoves() ) {
				if ( solution.getAssignment( move ) == 1 ) {
					extendLineageTracklet( tracklet, time + 1, bimapTp1.getA( move.getDest() ) );
				}
			}
			for ( final DivisionHypothesis div : segVar.getOutAssignments().getDivisions() ) {
				if ( solution.getAssignment( div ) == 1 ) {
					final int child1 = startLineageTracklet( tracklet.getTrackletId(), time + 1, bimapTp1.getA( div.getDest1() ) );
					final int child2 = startLineageTracklet( tracklet.getTrackletId(), time + 1, bimapTp1.getA( div.getDest2() ) );
					tracklet.setChild1( child1 );
					tracklet.setChild2( child2 );
				}
			}
		}
	}

	/**
	 * @param tracklet
	 * @param i
	 * @param a
	 */
	private void extendLineageTracklet( final Tracklet tracklet, final int time, final int objId ) {
		final Bimap< Integer, SegmentNode > bimapT = mapTime2Segments.get( time );
		final Bimap< Integer, SegmentNode > bimapTp1 = mapTime2Segments.get( time + 1 );
		final SegmentNode segVar = bimapT.getB( objId );

		tracklet.add( objId );

		for ( final DisappearanceHypothesis disapp : segVar.getOutAssignments().getDisappearances() ) {
			if ( solution.getAssignment( disapp ) == 1 ) { return; }
		}
		for ( final MovementHypothesis move : segVar.getOutAssignments().getMoves() ) {
			if ( solution.getAssignment( move ) == 1 ) {
				extendLineageTracklet( tracklet, time + 1, bimapTp1.getA( move.getDest() ) );
			}
		}
		for ( final DivisionHypothesis div : segVar.getOutAssignments().getDivisions() ) {
			if ( solution.getAssignment( div ) == 1 ) {
				final int child1 = startLineageTracklet( tracklet.getTrackletId(), time + 1, bimapTp1.getA( div.getDest1() ) );
				final int child2 = startLineageTracklet( tracklet.getTrackletId(), time + 1, bimapTp1.getA( div.getDest2() ) );
				tracklet.setChild1( child1 );
				tracklet.setChild2( child2 );
			}
		}
	}

	/**
	 * @param trackletId
	 * @param i
	 * @param a
	 */
	private int startLineageTracklet( final int parent, final int time, final int objId ) {
		final Bimap< Integer, SegmentNode > bimapT = mapTime2Segments.get( time );
		final Bimap< Integer, SegmentNode > bimapTp1 = mapTime2Segments.get( time + 1 );
		final SegmentNode segVar = bimapT.getB( objId );

		final Tracklet tracklet = new Tracklet( time, objId );
		tracklet.setParentId( parent );
		tracklets.add( tracklet );

		for ( final DisappearanceHypothesis disapp : segVar.getOutAssignments().getDisappearances() ) {
			if ( solution.getAssignment( disapp ) == 1 ) {}
		}
		for ( final MovementHypothesis move : segVar.getOutAssignments().getMoves() ) {
			if ( solution.getAssignment( move ) == 1 ) {
				extendLineageTracklet( tracklet, time + 1, bimapTp1.getA( move.getDest() ) );
			}
		}
		for ( final DivisionHypothesis div : segVar.getOutAssignments().getDivisions() ) {
			if ( solution.getAssignment( div ) == 1 ) {
				final int child1 = startLineageTracklet( tracklet.getTrackletId(), time + 1, bimapTp1.getA( div.getDest1() ) );
				final int child2 = startLineageTracklet( tracklet.getTrackletId(), time + 1, bimapTp1.getA( div.getDest2() ) );
				tracklet.setChild1( child1 );
				tracklet.setChild2( child2 );
			}
		}
		return tracklet.getTrackletId();
	}

	/**
	 * @return the mapTime2Segments
	 */
	public Map< Integer, Bimap< Integer, SegmentNode > > getTime2SegmentsMap() {
		return mapTime2Segments;
	}

	/**
	 * @return the tracklets
	 */
	public List< Tracklet > getTracklets() {
		return tracklets;
	}
}
