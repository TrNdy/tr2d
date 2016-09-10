package com.indago.demos.selectsegment;

import net.trackmate.graph.object.AbstractObjectEdge;

/**
 * An edge between two {@link SegmentVertex SegmentVertices}, meaning that
 * {@code source} ⊇ {@code target}.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class SubsetEdge extends AbstractObjectEdge< SubsetEdge, SegmentVertex > {

	protected SubsetEdge( final SegmentVertex source, final SegmentVertex target ) {
		super( source, target );
	}
}
