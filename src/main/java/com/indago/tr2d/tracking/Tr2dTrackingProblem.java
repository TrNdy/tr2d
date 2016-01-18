package com.indago.tr2d.tracking;

import java.util.List;

import com.indago.tracking.SegmentationProblem;

public interface Tr2dTrackingProblem
{
	List< SegmentationProblem > getTimepoints();
}
