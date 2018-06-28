/**
 *
 */
package com.indago.tr2d.ui.model;

import java.util.ArrayList;
import java.util.List;

import com.indago.fg.Assignment;
import com.indago.pg.IndicatorNode;
import com.indago.tr2d.ui.listener.SolutionChangedListener;
import com.indago.tr2d.ui.util.SolutionVisualizer;
import com.indago.tr2d.ui.view.bdv.overlays.Tr2dTrackingOverlay;
import com.indago.ui.bdv.BdvWithOverlaysOwner;

import bdv.util.BdvHandlePanel;
import bdv.util.BdvOverlay;
import bdv.util.BdvSource;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
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
		final int bdvTime = bdvHandlePanel.getViewerPanel().getState().getCurrentTimepoint();

		bdvRemoveAll();
		imgs.clear();

		bdvAdd( trackingModel.getTr2dModel().getRawData(), "RAW" );
		bdvHandlePanel.getViewerPanel().setTimepoint( bdvTime );

		newPgAssignment = trackingModel.getSolution();

		if ( imgSolutionOld == null ) {
			imgSolutionOld = SolutionVisualizer.drawSolutionSegmentImages( trackingModel, oldPgAssignment );
		}
		bdvAdd( imgSolutionOld, "PREVIOUS SOL", 0, 5, new ARGBType( 0xFF0000 ), true );
		imgs.add( imgSolutionOld );

		imgSolutionNew = SolutionVisualizer.drawSolutionSegmentImages( trackingModel, newPgAssignment );
		bdvAdd( imgSolutionNew, "CURRENT SOL", 0, 5, new ARGBType( 0x00FF00 ), true );
		imgs.add( imgSolutionNew );

		bdvAdd( new Tr2dTrackingOverlay( trackingModel ), "overlay_tracking" );
	}

	/**
	 * @return the imgSolution
	 */
	public RandomAccessibleInterval< IntType > getImgSolution() {
		return imgSolDiff;
	}

	/**
	 * @see com.indago.ui.bdv.BdvOwner#bdvSetHandlePanel(BdvHandlePanel)
	 */
	@Override
	public void bdvSetHandlePanel( final BdvHandlePanel bdvHandlePanel ) {
		this.bdvHandlePanel = bdvHandlePanel;
	}

	/**
	 * @see com.indago.ui.bdv.BdvOwner#bdvGetHandlePanel()
	 */
	@Override
	public BdvHandlePanel bdvGetHandlePanel() {
		return bdvHandlePanel;
	}

	/**
	 * @see com.indago.ui.bdv.BdvOwner#bdvGetSources()
	 */
	@Override
	public List< BdvSource > bdvGetSources() {
		return bdvSources;
	}

	/**
	 * @see com.indago.ui.bdv.BdvOwner#bdvGetSourceFor(net.imglib2.RandomAccessibleInterval)
	 */
	@Override
	public < T extends RealType< T > & NativeType< T > > BdvSource bdvGetSourceFor( final RandomAccessibleInterval< T > img ) {
		final int idx = imgs.indexOf( img );
		if ( idx == -1 ) return null;
		return bdvGetSources().get( idx );
	}

	/**
	 * @see com.indago.ui.bdv.BdvWithOverlaysOwner#bdvGetOverlays()
	 */
	@Override
	public List< BdvOverlay > bdvGetOverlays() {
		return overlays;
	}

	/**
	 * @see com.indago.ui.bdv.BdvWithOverlaysOwner#bdvGetOverlaySources()
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
		this.imgSolutionOld = this.imgSolutionNew;

		this.newPgAssignment = newAssignment;

		populateBdv();
	}
}
