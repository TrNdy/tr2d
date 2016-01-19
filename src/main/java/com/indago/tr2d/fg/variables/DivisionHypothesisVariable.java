/**
 *
 */
package com.indago.tr2d.fg.variables;

import com.indago.data.segmentation.Segment;
import com.indago.data.segmentation.fg.SegmentHypothesisVariable;
import com.indago.old_fg.variable.BooleanVariable;

import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;

/**
 * @author jug
 */
public class DivisionHypothesisVariable< S extends Segment, T extends SegmentHypothesisVariable< S > >
		extends
		BooleanVariable {

	private final T sourceSegVar;
	private final T destSegVar1;
	private final T destSegVar2;

	/**
	 * @param value
	 */
	public DivisionHypothesisVariable(
			final T sourceSegment,
			final T destSegment1,
			final T destSegment2 ) {
		this.sourceSegVar = sourceSegment;
		this.destSegVar1 = destSegment1;
		this.destSegVar2 = destSegment2;
	}

	public T getSourceSegVar() {
		return this.sourceSegVar;
	}

	public Pair< T, T > getDestSegVars() {
		return new ValuePair< T, T >( this.destSegVar1, this.destSegVar2 );
	}
}
