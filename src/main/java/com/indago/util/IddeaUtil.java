/**
 *
 */
package com.indago.util;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jhotdraw.draw.Figure;

import com.indago.fkt.SampledFunction1D;

import net.imglib2.IterableInterval;
import net.imglib2.Localizable;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.histogram.Histogram1d;
import net.imglib2.histogram.Real1dBinMapper;
import net.imglib2.newroi.util.ContainsRandomAccessible;
import net.imglib2.newroi.util.LocalizableSet;
import net.imglib2.roi.IterableRegion;
import net.imglib2.roi.Regions;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;
import view.component.IddeaComponent;

/**
 * @author jug
 */
public class IddeaUtil {

	static class MyFigureMarkupsLocator implements LocalizableSet {

		private final Set< Figure > figures;
		private final Color color;

		/**
		 * @param jHotDrawFigures
		 * @param transform
		 */
		public MyFigureMarkupsLocator( final Set< Figure > jHotDrawFigures ) {
			this( jHotDrawFigures, null );
		}

		/**
		 * @param jHotDrawFigures
		 * @param transform
		 * @param filterColor
		 */
		public MyFigureMarkupsLocator( final Set< Figure > jHotDrawFigures, final Color filterColor ) {
			this.figures = jHotDrawFigures;
			this.color = filterColor;
		}

		@Override
		public int numDimensions() {
			return 2;
		}

		@Override
		public boolean contains( final Localizable p ) {
			final Point2D.Double point2d = new Point2D.Double( p.getDoublePosition( 0 ), p.getDoublePosition( 1 ) );

			for ( final Figure f : this.figures ) {

				if ( this.color == null || this.color.equals( f.get( org.jhotdraw.draw.AttributeKeys.STROKE_COLOR ) ) ) {
					if ( f.getBounds().contains( point2d ) ) { //speedup
						if ( f.contains( point2d ) ) { return true; }
					}
				}
			}
			return false;
		}
	}

	/**
	 * @param iddeaComp
	 */
	public static SampledFunction1D getHistogramFromInteractiveViewer( final IddeaComponent iddeaComp, final Color color, final double min, final double max, final int numBins ) {
		final Set< Figure > figures = iddeaComp.getAllAnnotationFigures();

		// Figure out which pixels are selected by any JHotDraw annotations
		final MyFigureMarkupsLocator locations = new MyFigureMarkupsLocator( figures, color );

		// create a (unbounded) BoolType RandomAccessible with pixel value decided by MyCircle.contains
		final RandomAccessible< BoolType > a = new ContainsRandomAccessible( locations );

		// restrict that to the interval of the image
		final RandomAccessibleInterval< BoolType > b = Views.interval( a, iddeaComp.getSourceImage() );

		// make IteratableInterval that iterates only over the True pixels (selected areas)
		final IterableRegion< BoolType > c = Regions.iterable( b );

		// sample img with that: IteratableInterval that iterates only over the
		// img pixels for whose coordinates MyFigureMarkupsLocator.contains() is true
		final IterableInterval< DoubleType > samples = Regions.sample( c, iddeaComp.getSourceImage() );

//		ImageJFunctions.show( Converters.convert( b, new Converter< BoolType, UnsignedByteType >() {
//
//			@Override
//			public void convert( final BoolType input, final UnsignedByteType output ) {
//				output.set( input.get() ? 255 : 0 );
//			}
//		}, new UnsignedByteType() ) );

		final Histogram1d< DoubleType > histogram = new Histogram1d< DoubleType >( samples, new Real1dBinMapper< DoubleType >( min, max, numBins, false ) );
		final long[] longArray = histogram.toLongArray();

		final List< Double > x = new ArrayList< Double >();
		final List< Double > y = new ArrayList< Double >();
		for ( int i = 0; i < longArray.length; i++ ) {
			x.add( min + ( i + 0.5 ) * ( max - min ) / numBins );
			y.add( ( double ) longArray[ i ] );
		}

		return new SampledFunction1D( x, y );
	}
}
