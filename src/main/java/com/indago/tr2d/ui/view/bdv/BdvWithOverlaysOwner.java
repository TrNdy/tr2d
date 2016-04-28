/**
 *
 */
package com.indago.tr2d.ui.view.bdv;

import java.util.List;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvOverlay;
import bdv.util.BdvSource;

/**
 * @author jug
 */
public interface BdvWithOverlaysOwner extends BdvOwner {

	public List< BdvSource > bdvGetOverlaySources();

	public List< BdvOverlay > bdvGetOverlays();

	/**
	 * @param overlay
	 * @return
	 */
	public default BdvSource bdvGetSourceFor( final BdvOverlay overlay ) {
		final int idx = bdvGetOverlays().indexOf( overlay );
		if ( idx == -1 ) return null;
		return bdvGetOverlaySources().get( idx );
	}

	/**
	 * @param img
	 */
	public default void bdvAdd( final BdvOverlay overlay ) {
		final BdvSource source = BdvFunctions.showOverlay(
				overlay,
				"overlay",
				Bdv.options().addTo( bdvGetHandlePanel() ) );
		bdvGetOverlaySources().add( source );
		bdvGetOverlays().add( overlay );
	}

	/**
	 * @param img
	 */
	public default void bdvRemove( final BdvOverlay overlay ) {
		final BdvSource source = bdvGetSourceFor( overlay );
		source.removeFromBdv();
		bdvGetOverlaySources().remove( source );
		bdvGetOverlays().remove( overlay );
	}

	/*
	 *
	 */
	public default void bdvRemoveAllOverlays() {
		for ( final BdvSource overlay : bdvGetOverlaySources() ) {
			overlay.removeFromBdv();
		}
		bdvGetOverlaySources().clear();
		bdvGetOverlays().clear();
	}

}
