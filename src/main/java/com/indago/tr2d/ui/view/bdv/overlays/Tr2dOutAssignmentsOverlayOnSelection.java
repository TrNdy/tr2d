/**
 *
 */
package com.indago.tr2d.ui.view.bdv.overlays;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
public class Tr2dOutAssignmentsOverlayOnSelection extends BdvOverlay {

	private final Tr2dTrackingModel trackingModel;
	private Collection< SegmentNode > selectedNodes = null;
	private final int time;
	private final boolean showMovements;
	private final boolean showDivisions;

	private final List< Color > listActiveColor;
	private final List< Color > listMovementColors;
	private final List< Color > listDivisionColors;

	public Tr2dOutAssignmentsOverlayOnSelection(
			final Tr2dTrackingModel model,
			final int t,
			final boolean showMovements,
			final boolean showDivisions ) {
		this.trackingModel = model;
		this.time = t;
		this.showMovements = showMovements;
		this.showDivisions = showDivisions;

		this.listActiveColor = new ArrayList<>();
		this.listActiveColor.add( Color.CYAN );

		this.listMovementColors = new ArrayList<>();
		this.listMovementColors.add( new Color( 102, 0, 204 ) );
		this.listMovementColors.add( new Color( 127, 0, 255 ) );
		this.listMovementColors.add( new Color( 153, 51, 255 ) );
		this.listMovementColors.add( new Color( 178, 102, 255 ) );
		this.listMovementColors.add( new Color( 204, 153, 255 ) );
		this.listMovementColors.add( new Color( 229, 204, 255 ) );

		this.listDivisionColors = new ArrayList<>();
		this.listDivisionColors.add( new Color( 204, 102, 0 ) );
		this.listDivisionColors.add( new Color( 255, 128, 0 ) );
		this.listDivisionColors.add( new Color( 255, 153, 51 ) );
		this.listDivisionColors.add( new Color( 255, 178, 102 ) );
		this.listDivisionColors.add( new Color( 255, 204, 153 ) );
		this.listDivisionColors.add( new Color( 255, 229, 204 ) );
	}

	public void setSelectedSegmentNodes( final Collection< SegmentNode > selectedNodes ) {
		this.selectedNodes = selectedNodes;
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

			drawCOMs( g, time );
			drawOutAssignments( g, time );
		}
	}

	private void drawOutAssignments( final Graphics2D g, final int cur_t ) {
		if ( this.selectedNodes == null ) return;

		final Tr2dTrackingProblem tr2dPG = trackingModel.getTrackingProblem();
		final Assignment< IndicatorNode > pgSolution = trackingModel.getSolution();

		final AffineTransform2D trans = new AffineTransform2D();
		getCurrentTransform2D( trans );
		final Tr2dSegmentationProblem tp0 = tr2dPG.getTimepoints().get( time );
		for ( final SegmentNode segvar : tp0.getSegments() ) {
			if ( this.selectedNodes.contains( segvar ) ) {
				if ( showMovements ) {
					int i = 0;
					for ( final MovementHypothesis move : segvar.getOutAssignments().getMoves() ) {
						if ( pgSolution.getAssignment( move ) == 1 ) {
							drawOutMove( g, trans, move, listActiveColor, i++, segvar.getOutAssignments().getMoves().size() );
						} else {
							drawOutMove( g, trans, move, listMovementColors, i++, segvar.getOutAssignments().getMoves().size() );
						}
					}
				}
				if ( showDivisions ) {
					int i = 0;
					for ( final DivisionHypothesis div : segvar.getOutAssignments().getDivisions() ) {
						if ( pgSolution.getAssignment( div ) == 1 ) {
							drawOutDivision( g, trans, div, listActiveColor, i++, segvar.getOutAssignments().getDivisions().size() );
						} else {
							drawOutDivision( g, trans, div, listDivisionColors, i++, segvar.getOutAssignments().getDivisions().size() );
						}
					}
				}
			}
		}
	}

	/**
	 * @param g
	 *            Graphics2D
	 * @param trans
	 *            AffineTransform2D
	 * @param move
	 *            MovementHypothesis
	 * @param color
	 *            List< Color >
	 * @param i
	 *            is a zero based count of how often this function has been
	 *            called for the same source
	 * @param n
	 *            is the number of times this function will be called for the
	 *            same move.source
	 */
	private void drawOutMove(
			final Graphics2D g,
			final AffineTransform2D trans,
			final MovementHypothesis move,
			final List< Color > colors,
			final int i,
			final int n ) {

		final Graphics2D g2 = g;

		final RealLocalizable comFrom = move.getSrc().getSegment().getCenterOfMass();
		final double[] lposFrom = new double[ 2 ];
		final double[] gposFrom = new double[ 2 ];
		comFrom.localize( lposFrom );
		trans.apply( lposFrom, gposFrom );

		final RealLocalizable comTo = move.getDest().getSegment().getCenterOfMass();
		final double[] lposTo = new double[ 2 ];
		final double[] gposTo = new double[ 2 ];
		comTo.localize( lposTo );
		trans.apply( lposTo, gposTo );

		// setting transparency
		final float alpha = ( float ) ( 0.8 - ( 0.6 * i / n ) );
		final AlphaComposite alcom = AlphaComposite.getInstance(
				AlphaComposite.SRC_OVER,
				alpha );
		g.setComposite( alcom );

		// draw dest cross
		g2.setStroke( new BasicStroke( ( float ) 1.5 ) );
		final int len = 3;
		g.setColor( Color.lightGray ); // shadow
		g.drawLine( ( int ) gposTo[ 0 ] - len - 1, ( int ) gposTo[ 1 ] - len + 1, ( int ) gposTo[ 0 ] + len - 1, ( int ) gposTo[ 1 ] + len + 1 );
		g.drawLine( ( int ) gposTo[ 0 ] - len - 1, ( int ) gposTo[ 1 ] + len + 1, ( int ) gposTo[ 0 ] + len - 1, ( int ) gposTo[ 1 ] - len + 1 );
		g.setColor( colors.get( i % colors.size() ) ); // cross
		g.drawLine( ( int ) gposTo[ 0 ] - len, ( int ) gposTo[ 1 ] - len, ( int ) gposTo[ 0 ] + len, ( int ) gposTo[ 1 ] + len );
		g.drawLine( ( int ) gposTo[ 0 ] - len, ( int ) gposTo[ 1 ] + len, ( int ) gposTo[ 0 ] + len, ( int ) gposTo[ 1 ] - len );

		// write cost string
		final Font font = g2.getFont();
		final FontMetrics metrics = g2.getFontMetrics( font );
		g.setColor( Color.lightGray ); // shadow
		g.drawString(
				String.format( " c=%.2f", move.getCost() ),
				( int ) gposFrom[ 0 ] - 1,
				( int ) ( gposFrom[ 1 ] - metrics.getHeight() * ( i + 0.5 ) + 1 ) );
		g.setColor( colors.get( i % colors.size() ) ); // text
		g.drawString(
				String.format( " c=%.2f", move.getCost() ),
				( int ) gposFrom[ 0 ],
				( int ) ( gposFrom[ 1 ] - metrics.getHeight() * ( i + 0.5 ) ) );

		// draw move line
		g2.setStroke( new BasicStroke( 1 + ( ( float ) i * 6 ) / n ) );
//		g.setColor( Color.lightGray ); // shadow
//		g.drawLine( ( int ) gposFrom[ 0 ] - 1, ( int ) gposFrom[ 1 ] + 1, ( int ) gposTo[ 0 ] - 1, ( int ) gposTo[ 1 ] + 1 );
		g.setColor( colors.get( i % colors.size() ) ); // line
		g.drawLine( ( int ) gposFrom[ 0 ], ( int ) gposFrom[ 1 ], ( int ) gposTo[ 0 ], ( int ) gposTo[ 1 ] );
	}

	/**
	 * @param g
	 *            Graphics2D
	 * @param trans
	 *            AffineTransform2D
	 * @param move
	 *            MovementHypothesis
	 * @param color
	 *            List< Color >
	 * @param i
	 *            is a zero based count of how often this function has been
	 *            called for the same source
	 * @param n
	 *            is the number of times this function will be called for the
	 *            same move.source
	 */
	private void drawOutDivision(
			final Graphics2D g,
			final AffineTransform2D trans,
			final DivisionHypothesis division,
			final List< Color > colors,
			final int i,
			final int n ) {

		final Graphics2D g2 = g;

		final RealLocalizable comFrom = division.getSrc().getSegment().getCenterOfMass();
		final double[] lposFrom = new double[ 2 ];
		final double[] gposFrom = new double[ 2 ];
		comFrom.localize( lposFrom );
		trans.apply( lposFrom, gposFrom );

		final RealLocalizable comTo1 = division.getDest1().getSegment().getCenterOfMass();
		final double[] lposTo1 = new double[ 2 ];
		final double[] gposTo1 = new double[ 2 ];
		comTo1.localize( lposTo1 );
		trans.apply( lposTo1, gposTo1 );

		final RealLocalizable comTo2 = division.getDest2().getSegment().getCenterOfMass();
		final double[] lposTo2 = new double[ 2 ];
		final double[] gposTo2 = new double[ 2 ];
		comTo2.localize( lposTo2 );
		trans.apply( lposTo2, gposTo2 );

		// setting transparency
		final float alpha = ( float ) ( 0.8 - ( 0.6 * i / n ) );
		final AlphaComposite alcom = AlphaComposite.getInstance(
				AlphaComposite.SRC_OVER,
				alpha );
		g.setComposite( alcom );

		// draw dest crossess
		g2.setStroke( new BasicStroke( ( float ) 1.5 ) );
		final int len = 3;
		g.setColor( Color.lightGray ); // shadow
		g.drawLine( ( int ) gposTo1[ 0 ] - len - 1, ( int ) gposTo1[ 1 ] - len + 1, ( int ) gposTo1[ 0 ] + len - 1, ( int ) gposTo1[ 1 ] + len + 1 );
		g.drawLine( ( int ) gposTo1[ 0 ] - len - 1, ( int ) gposTo1[ 1 ] + len + 1, ( int ) gposTo1[ 0 ] + len - 1, ( int ) gposTo1[ 1 ] - len + 1 );
		g.drawLine( ( int ) gposTo2[ 0 ] - len - 1, ( int ) gposTo2[ 1 ] - len + 1, ( int ) gposTo2[ 0 ] + len - 1, ( int ) gposTo2[ 1 ] + len + 1 );
		g.drawLine( ( int ) gposTo2[ 0 ] - len - 1, ( int ) gposTo2[ 1 ] + len + 1, ( int ) gposTo2[ 0 ] + len - 1, ( int ) gposTo2[ 1 ] - len + 1 );
		g.setColor( colors.get( i % colors.size() ) ); // crosses
		g.drawLine( ( int ) gposTo1[ 0 ] - len, ( int ) gposTo1[ 1 ] - len, ( int ) gposTo1[ 0 ] + len, ( int ) gposTo1[ 1 ] + len );
		g.drawLine( ( int ) gposTo1[ 0 ] - len, ( int ) gposTo1[ 1 ] + len, ( int ) gposTo1[ 0 ] + len, ( int ) gposTo1[ 1 ] - len );
		g.drawLine( ( int ) gposTo2[ 0 ] - len, ( int ) gposTo2[ 1 ] - len, ( int ) gposTo2[ 0 ] + len, ( int ) gposTo2[ 1 ] + len );
		g.drawLine( ( int ) gposTo2[ 0 ] - len, ( int ) gposTo2[ 1 ] + len, ( int ) gposTo2[ 0 ] + len, ( int ) gposTo2[ 1 ] - len );

		// write cost string
		final Font font = g2.getFont();
		final FontMetrics metrics = g2.getFontMetrics( font );
		g.setColor( Color.lightGray ); // shadow
		g.drawString(
				String.format( " c=%.2f", division.getCost() ),
				( int ) gposFrom[ 0 ] - 1,
				( int ) ( gposFrom[ 1 ] + metrics.getHeight() * ( i + 0.5 ) + 1 ) );
		g.setColor( colors.get( i % colors.size() ) ); // text
		g.drawString(
				String.format( " c=%.2f", division.getCost() ),
				( int ) gposFrom[ 0 ],
				( int ) ( gposFrom[ 1 ] + metrics.getHeight() * ( i + 0.5 ) ) );

		// draw division line
		g.setColor( colors.get( i % colors.size() ) );
		g2.setStroke( new BasicStroke( 1 + ( ( float ) i * 6 ) / n ) );
		g.drawLine( ( int ) gposFrom[ 0 ], ( int ) gposFrom[ 1 ], ( int ) gposTo1[ 0 ], ( int ) gposTo1[ 1 ] );
		g.drawLine( ( int ) gposFrom[ 0 ], ( int ) gposFrom[ 1 ], ( int ) gposTo2[ 0 ], ( int ) gposTo2[ 1 ] );
	}

	private void drawCOMs( final Graphics2D g, final int cur_t ) {
		if ( this.selectedNodes == null ) return;

		g.setColor( Color.RED );
		final Graphics2D g2 = g;
		g2.setStroke( new BasicStroke( ( float ) 2.5 ) );
		final int len = 3;

		final Tr2dTrackingProblem tr2dPG = trackingModel.getTrackingProblem();

		final AffineTransform2D trans = new AffineTransform2D();
		getCurrentTransform2D( trans );
		final Tr2dSegmentationProblem tp0 = tr2dPG.getTimepoints().get( time );
		for ( final SegmentNode segvar : tp0.getSegments() ) {
			if ( this.selectedNodes.contains( segvar ) ) {
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
