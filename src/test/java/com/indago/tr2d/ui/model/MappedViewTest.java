package com.indago.tr2d.ui.model;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.view.Views;
import org.junit.Test;

import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class MappedViewTest {

	@Test
	public void testMappedView() {
		Function<IntType, String> mapping = IntType::toString;
		Img< IntType > ints = ArrayImgs.ints(new int[] { 1, 2 }, 2);
		RandomAccessibleInterval< String > strings = new MappedView<>(ints, mapping);
		Cursor< String > cursor = Views.iterable(strings).cursor();
		assertEquals("1", cursor.next());
		assertEquals("2", cursor.next());
		assertFalse(cursor.hasNext());
	}

}
