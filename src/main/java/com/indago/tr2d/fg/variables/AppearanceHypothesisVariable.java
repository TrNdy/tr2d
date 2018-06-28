/**
 *
 */
package com.indago.tr2d.fg.variables;

import com.indago.data.segmentation.Segment;
import com.indago.data.segmentation.fg.SegmentHypothesisVariable;
import com.indago.fg.BooleanVariable;

/**
 * @author jug
 */
public class AppearanceHypothesisVariable< S extends Segment, T extends SegmentHypothesisVariable< S > >
		extends
		BooleanVariable {

	private final T segVar;

	public AppearanceHypothesisVariable( final T segment ) {
		this.segVar = segment;
	}

	public T getSegVar() {
		return this.segVar;
	}
}
