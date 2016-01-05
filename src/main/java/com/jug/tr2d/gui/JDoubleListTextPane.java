/**
 *
 */
package com.jug.tr2d.gui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTextPane;


/**
 * @author jug
 */
public class JDoubleListTextPane extends JTextPane {

	private final List< Double > listOfDoubles;

	public JDoubleListTextPane( final List< Double > listOfDoubles ) {
		this.listOfDoubles = listOfDoubles;
		this.setText( this.toString() );
	}

	@Override
	public String toString() {
		String ret = "";
		boolean first = true;
		for ( final Double d : listOfDoubles ) {
			if (!first) {
				ret += ", ";
			} else {
				first = false;
			}
			ret += d.toString();
		}
		return ret;
	}

	/**
	 * @return
	 */
	public List< Double > getList() throws NumberFormatException {
		final List< Double > ret = new ArrayList< Double >();
		final String str = this.getText();
		final String[] columns = str.split( "," );
		for ( final String col : columns ) {
			ret.add( Double.parseDouble( col ) );
		}
		return ret;
	}
}
