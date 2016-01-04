package net.imglib2.newroi.util;

import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.type.logic.BoolType;

public class ContainsRandomAccessible implements RandomAccessible< BoolType >
{
	private final LocalizableSet contains;

	public ContainsRandomAccessible( final LocalizableSet contains )
	{
		this.contains = contains;
	}

	@Override
	public int numDimensions()
	{
		return contains.numDimensions();
	}

	@Override
	public RandomAccess< BoolType > randomAccess()
	{
		return new ContainsRandomAccess( contains );
	}

	@Override
	public RandomAccess< BoolType > randomAccess( final Interval interval )
	{
		return new ContainsRandomAccess( contains );
	}
}
