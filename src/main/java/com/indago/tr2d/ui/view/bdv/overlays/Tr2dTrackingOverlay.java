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
	private final int time;

	/**
	 * @param model
	 */
	public Tr2dTrackingOverlay( final Tr2dTrackingModel model ) {
		this.trackingModel = model;
		this.time = -1;
	}

	/**
	 * @param model
	 */
	public Tr2dTrackingOverlay( final Tr2dTrackingModel model, final int t ) {
		this.trackingModel = model;
		this.time = t;
	}

	/**
	 * @see bdv.util.BdvOverlay#draw(java.awt.Graphics2D)
	 */
	@Override
	protected void draw( final Graphics2D g ) {
		final Assignment< IndicatorNode > pgSolution = trackingModel.getSolution();

		if ( pgSolution != null ) {
			final AffineTransform2D trans = new AffineTransform2D();
			getCurrentTransform2D( trans );

			final int t = ( this.time == -1 ) ? info.getTimePointIndex() : this.time;
			drawCOMs( g, t );
			drawCOMTails( g, t, 5 );
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
		final Tr2dSegmentationProblem tp1 = tr2dPG.getTimepoints().get( cur_t );
		for ( final SegmentNode segvar : tp1.getSegments() ) {
			if ( pgSolution.getAssignment( segvar ) == 1 ) {
				for ( final MovementHypothesis move : segvar.getInAssignments().getMoves() ) {
					if ( pgSolution.getAssignment( move ) == 1 ) {
						drawCOMTailSegment( g, trans, cur_t - 1, move.getSrc(), segvar, Color.GREEN, 0, length );
					}
				}
				for ( final DivisionHypothesis div : segvar.getInAssignments().getDivisions() ) {
					if ( pgSolution.getAssignment( div ) == 1 ) {
						drawCOMTailSegment( g, trans, cur_t - 1, div.getSrc(), segvar, Color.ORANGE, 0, length );
					}
				}
			}
		}
	}

	/**
	 * @param g
	 * @param trans
	 * @param from_t
	 * @param from
	 * @param to
	 * @param color
	 * @param i
	 * @param length
	 */
	private void drawCOMTailSegment(
			final Graphics2D g,
			final AffineTransform2D trans,
			final int from_t,
			final SegmentNode from,
			final SegmentNode to,
			final Color color,
			final int i,
			final int length ) {

		final Graphics2D g2 = g;
		g2.setStroke( new BasicStroke( 3 * ( ( length - i ) / ( ( float ) length ) ) ) );

		final Assignment< IndicatorNode > pgSolution = trackingModel.getSolution();

		final Tr2dSegmentationProblem tp0 = trackingModel.getTrackingProblem().getTimepoints().get( from_t );
		final Tr2dSegmentationProblem tp1 = trackingModel.getTrackingProblem().getTimepoints().get( from_t + 1 );
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

		if ( tp0.getEditState().isMoveForcedFrom( from ) ||
			 tp1.getEditState().isMoveForcedTo( to ) ||
		  	 tp0.getEditState().isDivisionForcedFrom( from ) ||
			 tp1.getEditState().isDivisionForcedTo( to )	) {
			g.setColor( Color.CYAN );
			g.drawLine( ( int ) gposFrom[ 0 ], ( int ) gposFrom[ 1 ], ( int ) gposTo[ 0 ], ( int ) gposTo[ 1 ] );
		} else {
			g.setColor( color );
			g.drawLine( ( int ) gposFrom[ 0 ], ( int ) gposFrom[ 1 ], ( int ) gposTo[ 0 ], ( int ) gposTo[ 1 ] );
		}

		if ( from_t > 0 && i < length ) {
			for ( final MovementHypothesis move : from.getInAssignments().getMoves() ) {
				if ( pgSolution.getAssignment( move ) == 1 ) {
					drawCOMTailSegment( g, trans, from_t - 1, move.getSrc(), from, Color.GREEN, i + 1, length );
				}
			}
			for ( final DivisionHypothesis div : from.getInAssignments().getDivisions() ) {
				if ( pgSolution.getAssignment( div ) == 1 ) {
					drawCOMTailSegment( g, trans, from_t - 1, div.getSrc(), from, Color.ORANGE, i + 1, length );
				}
			}
		}
	}

	/**
	 * @param g
	 */
	private void drawCOMs( final Graphics2D g, final int cur_t ) {
		final Color theRegularColor = Color.RED.darker();
		final Color theForcedColor = Color.RED.brighter();
		final Color theAvoidedColor = Color.GRAY.brighter().brighter();

		final Graphics2D g2 = g;
		int len = 3;

		final Tr2dTrackingProblem tr2dPG = trackingModel.getTrackingProblem();
		final Assignment< IndicatorNode > pgSolution = trackingModel.getSolution();

		final AffineTransform2D trans = new AffineTransform2D();
		getCurrentTransform2D( trans );
		final Tr2dSegmentationProblem tp0 = tr2dPG.getTimepoints().get( cur_t );
		for ( final SegmentNode segvar : tp0.getSegments() ) {
			if ( pgSolution.getAssignment( segvar ) == 1 || tp0.getEditState().isAvoided( segvar ) ) {
				final RealLocalizable com = segvar.getSegment().getCenterOfMass();
				final double[] lpos = new double[ 2 ];
				final double[] gpos = new double[ 2 ];
				com.localize( lpos );
				trans.apply( lpos, gpos );

				if ( tp0.getEditState().isForced( segvar ) ) {
					g.setColor( theForcedColor );
					g2.setStroke( new BasicStroke( ( float ) 3.5 ) );
					len = 8;
					g.drawOval( ( int ) gpos[ 0 ] - len, ( int ) gpos[ 1 ] - len, len * 2, len * 2 );
				} else if ( tp0.getEditState().isAvoided( segvar ) ) {
					g.setColor( theAvoidedColor );
					g2.setStroke( new BasicStroke( 4 ) );
					len = 8;
					g.drawLine( ( int ) gpos[ 0 ] - len, ( int ) gpos[ 1 ] - len, ( int ) gpos[ 0 ] + len, ( int ) gpos[ 1 ] + len );
					g.drawLine( ( int ) gpos[ 0 ] - len, ( int ) gpos[ 1 ] + len, ( int ) gpos[ 0 ] + len, ( int ) gpos[ 1 ] - len );
				} else {
					g.setColor( theRegularColor );
					g2.setStroke( new BasicStroke( ( float ) 2.0 ) );
					len = 4;
					g.drawOval( ( int ) gpos[ 0 ] - len, ( int ) gpos[ 1 ] - len, len * 2, len * 2 );
				}
			}
		}
	}

}
