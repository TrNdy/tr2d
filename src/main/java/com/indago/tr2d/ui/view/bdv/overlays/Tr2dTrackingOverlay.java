/**
 *
 */
package com.indago.tr2d.ui.view.bdv.overlays;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

import com.indago.fg.Assignment;
import com.indago.pg.IndicatorNode;
import com.indago.pg.assignments.DivisionHypothesis;
import com.indago.pg.assignments.MovementHypothesis;
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
		final Assignment< IndicatorNode > pgSolution = trackingModel.getSolution();

		if ( pgSolution != null ) {
			final AffineTransform2D t = new AffineTransform2D();
			getCurrentTransform2D( t );

			drawCOMs( g, info.getTimePointIndex() );
			drawCOMTails( g, info.getTimePointIndex(), 5 );
		}
	}

	/**
	 * @param g
	 * @param cur_t
	 * @param length
	 */
	private void drawCOMTails( final Graphics2D g, final int cur_t, final int length ) {
		final Tr2dTrackingProblem tr2dPG = trackingModel.getTrackingProblem();
		final Assignment< IndicatorNode > pgSolution = trackingModel.getSolution();

		final AffineTransform2D trans = new AffineTransform2D();
		getCurrentTransform2D( trans );
		final Tr2dSegmentationProblem tp0 = tr2dPG.getTimepoints().get( info.getTimePointIndex() );
		for ( final SegmentNode segvar : tp0.getSegments() ) {
			if ( pgSolution.getAssignment( segvar ) == 1 ) {
				for ( final MovementHypothesis move : segvar.getInAssignments().getMoves() ) {
					if ( pgSolution.getAssignment( move ) == 1 ) {
						drawCOMTailSegment( g, trans, move.getSrc(), segvar, Color.GREEN, 0, length );
					}
				}
				for ( final DivisionHypothesis div : segvar.getInAssignments().getDivisions() ) {
					if ( pgSolution.getAssignment( div ) == 1 ) {
						drawCOMTailSegment( g, trans, div.getSrc(), segvar, Color.ORANGE, 0, length );
					}
				}
			}
		}
	}

	/**
	 * @param g
	 * @param trans
	 * @param from
	 * @param to
	 * @param i
	 */
	private void drawCOMTailSegment(
			final Graphics2D g,
			final AffineTransform2D trans,
			final SegmentNode from,
			final SegmentNode to,
			final Color color,
			final int i,
			final int length ) {

		g.setColor( color );

		final Graphics2D g2 = g;
		g2.setStroke( new BasicStroke( 3 * ( ( length - i ) / ( ( float ) length ) ) ) );

		final Assignment< IndicatorNode > pgSolution = trackingModel.getSolution();

		final RealLocalizable comFrom = from.getSegment().getCenterOfMass();
		final double[] lposFrom = new double[ 2 ];
		final double[] gposFrom = new double[ 2 ];
		comFrom.localize( lposFrom );
		trans.apply( lposFrom, gposFrom );

		final RealLocalizable comTo = to.getSegment().getCenterOfMass();
		final double[] lposTo = new double[ 2 ];
		final double[] gposTo = new double[ 2 ];
		comTo.localize( lposTo );
		trans.apply( lposTo, gposTo );

		g.drawLine( ( int ) gposFrom[ 0 ], ( int ) gposFrom[ 1 ], ( int ) gposTo[ 0 ], ( int ) gposTo[ 1 ] );

		if ( i < length ) {
			for ( final MovementHypothesis move : to.getInAssignments().getMoves() ) {
				if ( pgSolution.getAssignment( move ) == 1 ) {
					drawCOMTailSegment( g, trans, to, move.getSrc(), Color.GREEN, i + 1, length );
				}
			}
			for ( final DivisionHypothesis div : to.getInAssignments().getDivisions() ) {
				if ( pgSolution.getAssignment( div ) == 1 ) {
					drawCOMTailSegment( g, trans, to, div.getSrc(), Color.ORANGE, i + 1, length );
				}
			}
		}
	}

	/**
	 * @param g
	 */
	private void drawCOMs( final Graphics2D g, final int cur_t ) {
		g.setColor( Color.RED );
		final Graphics2D g2 = g;
		g2.setStroke( new BasicStroke( ( float ) 2.5 ) );
		final int len = 3;

		final Tr2dTrackingProblem tr2dPG = trackingModel.getTrackingProblem();
		final Assignment< IndicatorNode > pgSolution = trackingModel.getSolution();

		final AffineTransform2D trans = new AffineTransform2D();
		getCurrentTransform2D( trans );
		final Tr2dSegmentationProblem tp0 = tr2dPG.getTimepoints().get( info.getTimePointIndex() );
		for ( final SegmentNode segvar : tp0.getSegments() ) {
			if ( pgSolution.getAssignment( segvar ) == 1 ) {
				final RealLocalizable com = segvar.getSegment().getCenterOfMass();
				final double[] lpos = new double[ 2 ];
				final double[] gpos = new double[ 2 ];
				com.localize( lpos );
				trans.apply( lpos, gpos );
				g.drawLine( ( int ) gpos[ 0 ] - len, ( int ) gpos[ 1 ] - len, ( int ) gpos[ 0 ] + len, ( int ) gpos[ 1 ] + len );
				g.drawLine( ( int ) gpos[ 0 ] - len, ( int ) gpos[ 1 ] + len, ( int ) gpos[ 0 ] + len, ( int ) gpos[ 1 ] - len );
			}
		}
	}

}
