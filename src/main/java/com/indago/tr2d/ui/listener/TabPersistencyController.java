/**
 *
 */
package com.indago.tr2d.ui.listener;

import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * @author jug
 */
public class TabPersistencyController implements ChangeListener {

	private int prevTab;
	private int newTab;

	/**
	 *
	 */
	public TabPersistencyController() {
		this.prevTab = 0;
		this.newTab = 0;
	}

	/**
	 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
	 */
	@Override
	public void stateChanged( final ChangeEvent e ) {
		this.prevTab = this.newTab;
		final JTabbedPane sourceTabbedPane = ( JTabbedPane ) e.getSource();
		this.newTab = sourceTabbedPane.getSelectedIndex();
	}

}
