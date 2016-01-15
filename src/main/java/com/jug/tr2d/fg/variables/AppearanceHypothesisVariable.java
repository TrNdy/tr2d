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
public class AppearanceHypothesisVariable< S extends Segment, T extends SegmentHypothesisVariable< S > >
		extends
		BooleanVariable {

	private final T segVar;

	/**
	 * @param value
	 */
	public AppearanceHypothesisVariable( final T segment ) {
		this.segVar = segment;
	}

	public T getSegVar() {
		return this.segVar;
	}
}
