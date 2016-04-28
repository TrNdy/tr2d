/**
 *
 */
package com.indago.tr2d.ui.view.bdv.overlays;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Random;

import com.indago.fg.Assignment;
import com.indago.pg.IndicatorNode;
import com.indago.tr2d.pg.Tr2dTrackingProblem;

import bdv.util.BdvOverlay;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform2D;

/**
 * @author jug
 */
public class HelloOverlay extends BdvOverlay {

	private final ArrayList< RealPoint > points;

	/**
	 *
	 */
	public HelloOverlay( final Tr2dTrackingProblem tr2dTraProblem, final Assignment< IndicatorNode > pgSolution ) {
		final Random random = new Random();
		points = new ArrayList< >();
		for ( int i = 0; i < 1100; ++i )
			points.add( new RealPoint( random.nextInt( 100 ), random.nextInt( 100 ) ) );
	}

	/**
	 * @see bdv.util.BdvOverlay#draw(java.awt.Graphics2D)
	 */
	@Override
	protected void draw( final Graphics2D g ) {
		final AffineTransform2D t = new AffineTransform2D();
		getCurrentTransform2D( t );

		g.setColor( Color.RED );

		final double[] lPos = new double[ 2 ];
		final double[] gPos1 = new double[ 2 ];
		final double[] gPos2 = new double[ 2 ];
		final int start = info.getTimePointIndex() * 10;
		final int end = Math.min( info.getTimePointIndex() * 10 + 100, 1100 );
		for ( int i = start; i < end; i += 2 ) {
			points.get( i ).localize( lPos );
			t.apply( lPos, gPos1 );
			points.get( i + 1 ).localize( lPos );
			t.apply( lPos, gPos2 );
			g.drawLine( ( int ) gPos1[ 0 ], ( int ) gPos1[ 1 ], ( int ) gPos2[ 0 ], ( int ) gPos2[ 1 ] );
		}
	}

}
