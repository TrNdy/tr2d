/**
 *
 */
package com.indago.tr2d.ui.model;

import java.util.ArrayList;
import java.util.List;

import com.indago.fg.Assignment;
import com.indago.pg.IndicatorNode;
import com.indago.tr2d.ui.listener.SolutionChangedListener;
import com.indago.tr2d.ui.view.bdv.BdvWithOverlaysOwner;

import bdv.util.BdvHandlePanel;
import bdv.util.BdvOverlay;
import bdv.util.BdvSource;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;

/**
 * @author jug
 */
public class Tr2dSolDiffModel implements BdvWithOverlaysOwner, SolutionChangedListener {

	private final Tr2dTrackingModel trackingModel;

	private final RandomAccessibleInterval< IntType > imgSolDiff = null;

	private BdvHandlePanel bdvHandlePanel;
	private final List< RandomAccessibleInterval< IntType > > imgs;
	private final List< BdvSource > bdvSources = new ArrayList<>();
	private final List< BdvOverlay > overlays = new ArrayList<>();
	private final List< BdvSource > bdvOverlaySources = new ArrayList<>();

	private Assignment< IndicatorNode > oldPgAssignment = null;
	private Assignment< IndicatorNode > newPgAssignment = null;
	private RandomAccessibleInterval< IntType > imgSolutionOld;
	private RandomAccessibleInterval< IntType > imgSolutionNew;


	public Tr2dSolDiffModel( final Tr2dTrackingModel model ) {
		this.trackingModel = model;
		model.addSolutionChangedListener( this );
		imgs = new ArrayList<>();
	}

	public Tr2dTrackingModel getTr2dTrackingModel() {
		return trackingModel;
	}

	public void populateBdv() {
		bdvRemoveAll();

		bdvAdd( trackingModel.getTr2dModel().getRawData(), "RAW" );

		oldPgAssignment = trackingModel.getSolution();
		if ( oldPgAssignment != null ) {
			imgSolutionOld = trackingModel.getImgSolution();
			bdvAdd( imgSolutionOld, "PREV SOL" );
			imgs.add( imgSolutionOld );
		}

	}

	/**
	 * @return the imgSolution
	 */
	public RandomAccessibleInterval< IntType > getImgSolution() {
		return imgSolDiff;
	}

	/**
	 * @see com.indago.tr2d.ui.view.bdv.BdvOwner#setBdvHandlePanel()
	 */
	@Override
	public void bdvSetHandlePanel( final BdvHandlePanel bdvHandlePanel ) {
		this.bdvHandlePanel = bdvHandlePanel;
	}

	/**
	 * @see com.indago.tr2d.ui.view.bdv.BdvOwner#bdvGetHandlePanel()
	 */
	@Override
	public BdvHandlePanel bdvGetHandlePanel() {
		return bdvHandlePanel;
	}

	/**
	 * @see com.indago.tr2d.ui.view.bdv.BdvOwner#bdvGetSources()
	 */
	@Override
	public List< BdvSource > bdvGetSources() {
		return bdvSources;
	}

	/**
	 * @see com.indago.tr2d.ui.view.bdv.BdvOwner#bdvGetSourceFor(net.imglib2.RandomAccessibleInterval)
	 */
	@Override
	public < T extends RealType< T > & NativeType< T > > BdvSource bdvGetSourceFor( final RandomAccessibleInterval< T > img ) {
		final int idx = imgs.indexOf( img );
		if ( idx == -1 ) return null;
		return bdvGetSources().get( idx );
	}

	/**
	 * @see com.indago.tr2d.ui.view.bdv.BdvWithOverlaysOwner#bdvGetOverlays()
	 */
	@Override
	public List< BdvOverlay > bdvGetOverlays() {
		return overlays;
	}

	/**
	 * @see com.indago.tr2d.ui.view.bdv.BdvWithOverlaysOwner#bdvGetOverlaySources()
	 */
	@Override
	public List< BdvSource > bdvGetOverlaySources() {
		return bdvOverlaySources;
	}

	/**
	 * @see com.indago.tr2d.ui.listener.SolutionChangedListener#solutionChanged(com.indago.fg.Assignment)
	 */
	@Override
	public void solutionChanged( final Assignment< IndicatorNode > newAssignment ) {
		this.oldPgAssignment = this.newPgAssignment;
		this.newPgAssignment = newAssignment;
		populateBdv();
	}
}
