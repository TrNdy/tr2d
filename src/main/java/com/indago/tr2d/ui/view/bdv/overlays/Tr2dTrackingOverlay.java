/**
 *
 */
package com.indago.tr2d.ui.view.bdv.overlays;

import java.awt.Color;
import java.awt.Graphics2D;

import com.indago.fg.Assignment;
import com.indago.pg.IndicatorNode;
import com.indago.pg.segments.SegmentNode;
import com.indago.tr2d.pg.Tr2dSegmentationProblem;
import com.indago.tr2d.pg.Tr2dTrackingProblem;
import com.indago.tr2d.ui.model.Tr2dTrackingModel;

import bdv.util.BdvOverlay;
import net.imglib2.RealLocalizable;
import net.imglib2.realtransform.AffineTransform2D;

/**
 * @author jug
 */
public class Tr2dTrackingOverlay extends BdvOverlay {

	private final Tr2dTrackingModel trackingModel;

	/**
	 * @param model
	 */
	public Tr2dTrackingOverlay( final Tr2dTrackingModel model ) {
		this.trackingModel = model;
	}

	/**
	 * @see bdv.util.BdvOverlay#draw(java.awt.Graphics2D)
	 */
	@Override
	protected void draw( final Graphics2D g ) {
		final Tr2dTrackingProblem tr2dPG = trackingModel.getTrackingProblem();
		final Assignment< IndicatorNode > pgSolution = trackingModel.getSolution();

		if ( pgSolution != null ) {
			final AffineTransform2D t = new AffineTransform2D();
			getCurrentTransform2D( t );

			g.setColor( Color.RED );

			final Tr2dSegmentationProblem tp = tr2dPG.getTimepoints().get( info.getTimePointIndex() );
			for ( final SegmentNode segment : tp.getSegments() ) {
				final SegmentNode segvar = tp.getSegmentVar( tp.getLabelingSegment( segment ) );
				if ( pgSolution.getAssignment( segvar ) == 1 ) {
					final RealLocalizable com = segvar.getSegment().getCenterOfMass();
					final double[] lpos = new double[ 2 ];
					final double[] gpos = new double[ 2 ];
					com.localize( lpos );
					t.apply( lpos, gpos );
					final int len = 3;
					g.drawLine( ( int ) gpos[ 0 ] - len, ( int ) gpos[ 1 ] - len, ( int ) gpos[ 0 ] + len, ( int ) gpos[ 1 ] + len );
					g.drawLine( ( int ) gpos[ 0 ] - len, ( int ) gpos[ 1 ] + len, ( int ) gpos[ 0 ] + len, ( int ) gpos[ 1 ] - len );
				}
			}
		}
	}

}
