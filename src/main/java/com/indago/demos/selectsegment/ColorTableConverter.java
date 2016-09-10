package com.indago.demos.selectsegment;

import java.util.ArrayList;
import java.util.Arrays;

import com.indago.data.segmentation.LabelingPlus;

import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.IntType;

public class ColorTableConverter implements Converter< IntType, ARGBType > {

	private final LabelingPlus labelingPlus;

	private final ArrayList< ColorTable > colorTables;

	private int[] lut;

	public ColorTableConverter( final LabelingPlus labelingPlus ) {
		this.labelingPlus = labelingPlus;
		colorTables = new ArrayList<>();
	}

	public synchronized void update() {
		final int numSets = labelingPlus.getLabeling().getMapping().numSets();
		final int[] newlut = new int[ numSets ];

		Arrays.fill( newlut, 0 );

		for ( final ColorTable colorTable : colorTables ) {
			final int[] ct = colorTable.getLut();
			if ( ct == null )
				continue;

			for ( int i = 0; i < newlut.length; ++i ) {
				final int acc = newlut[ i ];
				final int col = ct[ i ];
				final int r = Math.min( 255, ARGBType.red( acc ) + ARGBType.red( col ) );
				final int g = Math.min( 255, ARGBType.green( acc ) + ARGBType.green( col ) );
				final int b = Math.min( 255, ARGBType.blue( acc ) + ARGBType.blue( col ) );
				newlut[ i ] = ARGBType.rgba( r, g, b, 255 );
			}
		}

		lut = newlut;
	}

	@Override
	public void convert( final IntType input, final ARGBType output ) {
		output.set( lut[ input.get() ] );
	}

	public synchronized boolean addColorTable( final ColorTable colorTable ) {
		if ( !colorTables.contains( colorTable ) ) {
			colorTables.add( colorTable );
			return true;
		}
		return false;
	}

	public synchronized boolean removeColorTable( final ColorTable l ) {
		return colorTables.remove( l );
	}
}
