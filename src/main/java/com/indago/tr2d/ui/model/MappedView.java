package com.indago.tr2d.ui.model;

import net.imglib2.AbstractWrappedInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.AbstractConvertedRandomAccess;

import java.util.function.Function;

public class MappedView<S, T> extends AbstractWrappedInterval<RandomAccessibleInterval<S>> implements RandomAccessibleInterval<T>
{

	private final Function< S, T > mapping;

	public MappedView( RandomAccessibleInterval<S> source, Function<S, T> mapping ) {
		super(source);
		this.mapping = mapping;
	}

	@Override public RandomAccess< T > randomAccess() {
		return new MappedRandomAccess(getSource().randomAccess());
	}

	@Override public RandomAccess< T > randomAccess(Interval interval) {
		return new MappedRandomAccess(getSource().randomAccess(interval));
	}

	private class MappedRandomAccess extends
			AbstractConvertedRandomAccess< S, T >
	{

		private MappedRandomAccess(RandomAccess<S> source) {
			super(source);
		}

		@Override public T get() {
			return mapping.apply(source.get());
		}

		@Override
		public MappedRandomAccess copy() {
			return new MappedRandomAccess(source.copyRandomAccess());
		}
	}
}
