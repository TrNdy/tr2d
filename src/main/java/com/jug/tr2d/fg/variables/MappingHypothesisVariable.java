/**
 *
 */
package com.jug.tr2d.fg.variables;

import com.indago.fg.variable.BooleanVariable;
import com.indago.segment.Segment;
import com.indago.segment.fg.SegmentHypothesisVariable;

/**
 * @author jug
 */
public class MappingHypothesisVariable< S extends Segment, T extends SegmentHypothesisVariable< S > >
		extends
		BooleanVariable {

	private final T sourceSegVar;
	private final T destSegVar;

	/**
	 * @param value
	 */
	public MappingHypothesisVariable( final T sourceSegment, final T destSegment ) {
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
