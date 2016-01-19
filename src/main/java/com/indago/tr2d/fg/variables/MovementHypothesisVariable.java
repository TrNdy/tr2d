/**
 *
 */
package com.indago.tr2d.fg.variables;

import com.indago.data.segmentation.Segment;
import com.indago.data.segmentation.fg.SegmentHypothesisVariable;
import com.indago.old_fg.variable.BooleanVariable;

/**
 * @author jug
 */
public class MovementHypothesisVariable< S extends Segment, T extends SegmentHypothesisVariable< S > >
		extends
		BooleanVariable {

	private final T sourceSegVar;
	private final T destSegVar;

	/**
	 * @param value
	 */
	public MovementHypothesisVariable( final T sourceSegment, final T destSegment ) {
		this.sourceSegVar = sourceSegment;
		this.destSegVar = destSegment;
	}

	public T getSourceSegVar() {
		return this.sourceSegVar;
	}

	public T getDestSegVar() {
		return this.destSegVar;
	}
}
