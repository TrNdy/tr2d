/**
 *
 */
package com.indago.tr2d.ui.view.bdv.overlays;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

import com.indago.tr2d.ui.model.Tr2dFlowModel;

import bdv.util.BdvOverlay;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.ValuePair;

/**
 * @author jug
 */
public class Tr2dFlowOverlay extends BdvOverlay {

	private final Tr2dFlowModel flowModel;

	/**
	 * @param model
	 */
	public Tr2dFlowOverlay( final Tr2dFlowModel model ) {
		this.flowModel = model;
	}

	/**
	 * @see bdv.util.BdvOverlay#draw(java.awt.Graphics2D)
	 */
	@Override
	protected void draw( final Graphics2D g ) {
		if ( flowModel.hasFlowLoaded() ) {
			drawRegularSpacesVectors( g, info.getTimePointIndex() );
		}
	}

	/**
	 * @param g
	 * @param cur_t
	 * @param length
	 */
	private void drawRegularSpacesVectors( final Graphics2D g, final int t ) {
		final RandomAccessibleInterval< FloatType > flowImg = flowModel.getFlowImage( t );
		final long sizeX = flowImg.dimension( 0 );
		final long sizeY = flowImg.dimension( 1 );
		for ( int x = 0; x < sizeX; x += 10 ) {
			for ( int y = 0; y < sizeY; y += 10 ) {
				drawVector( g, t, x, y );
			}
		}
	}

	/**
	 * @param g
	 * @param flowImg
	 * @param x
	 * @param y
	 */
	private void drawVector( final Graphics2D g, final int t, int x, int y ) {
		final ValuePair< Double, Double > flowVec = flowModel.getFlowVector( t, x, y );
		if ( x == 0 && y == 0 ) return;

		final AffineTransform2D trans = new AffineTransform2D();
		getCurrentTransform2D( trans );

		g.setColor( Color.YELLOW );

		final Graphics2D g2 = g;
		g2.setStroke( new BasicStroke( 1 ) );

		int xto = ( int ) ( x + flowVec.getA() );
		int yto = ( int ) ( y + flowVec.getB() );

		final double[] from = new double[]{x,y};
		final double[] to = new double[] { xto, yto };
		trans.apply( from, from );
		trans.apply( to, to );

		x = (int)from[0];
		y = (int)from[1];
		xto = (int)to[0];
		yto = (int)to[1];

		g.drawLine( x, y, xto, yto );

		g.setColor( Color.PINK );
		g.drawRect( x - 1, y - 1, 2, 2 );
	}
}
