package com.indago.tr2d.tracking;

import java.util.ArrayList;
import java.util.List;

import com.indago.tracking.SegmentationProblem;
import com.indago.tracking.TrackingProblem;

public class Tr2dTrackingProblem implements TrackingProblem {

	private final List< SegmentationProblem > timepoints;

	public Tr2dTrackingProblem() {
		timepoints = new ArrayList< >();
	}

	@Override
	public List< SegmentationProblem > getTimepoints() {
		return timepoints;
	}

	/**
	 * @param segmentationProblem
	 */
	public void addSegmentationProblem( final Tr2dSegmentationProblem segmentationProblem ) {
		timepoints.add( segmentationProblem );
		if ( timepoints.size() >= 2 ) {
			// connect to previous frame
			System.err.println(
					"tr2dTrackingProblem::addSegmentationProblem() -- implementation missing" );
		}
	}
}
