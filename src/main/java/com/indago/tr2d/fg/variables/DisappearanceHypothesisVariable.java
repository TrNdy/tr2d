/**
 *
 */
package com.indago.tr2d.fg.variables;

import com.indago.data.segmentation.Segment;
import com.indago.data.segmentation.fg.SegmentHypothesisVariable;
import com.indago.fg.variable.BooleanVariable;

/**
 * @author jug
 */
public class DisappearanceHypothesisVariable< S extends Segment, T extends SegmentHypothesisVariable< S > >
		extends
		BooleanVariable {

	private final T segVar;

	/**
	 * @param value
	 */
	public DisappearanceHypothesisVariable( final T segment ) {
		this.segVar = segment;
	}

	public T getSegVar() {
		return this.segVar;
	}
}
