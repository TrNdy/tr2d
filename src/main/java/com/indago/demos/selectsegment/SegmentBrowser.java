package com.indago.demos.selectsegment;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mastodon.collection.RefCollections;
import org.mastodon.graph.algorithm.traversal.BreadthFirstIterator;
import org.mastodon.revised.ui.selection.HighlightModel;
import org.mastodon.revised.ui.selection.Selection;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.ScrollBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

import com.indago.data.segmentation.LabelData;
import com.indago.data.segmentation.LabelingPlus;
import com.indago.tr2d.Tr2dLog;

import bdv.util.Bdv;
import bdv.viewer.TimePointListener;
import bdv.viewer.ViewerPanel;
import net.imglib2.RandomAccess;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.ui.TransformListener;
import net.imglib2.view.Views;

public class SegmentBrowser
{
	private final Bdv bdv;

	private final BdvListener bdvListener;

	private final LabelingPlus labelingPlus;

	private final Selection< SegmentVertex, SubsetEdge > segmentsUnderMouse;

	private final HighlightModel< SegmentVertex, SubsetEdge > highlightModel;

	private final Selection< SegmentVertex, SubsetEdge > selectionModel;

	private final List< SegmentVertex > segmentsOrdered;

	private SegmentVertex currentSegment;

	private final SegmentGraph segmentGraph;

	private final MouseMotionListener mml;

	public SegmentBrowser(
			final Bdv bdv,
			final LabelingPlus labelingPlus,
			final SegmentGraph segmentGraph,
			final Selection< SegmentVertex, SubsetEdge > segmentsUnderMouse,
			final HighlightModel< SegmentVertex, SubsetEdge > highlightModel,
			final Selection< SegmentVertex, SubsetEdge > selectionModel,
			final InputTriggerConfig inputConf )
	{
		this.bdv = bdv;
		this.segmentGraph = segmentGraph;
		this.selectionModel = selectionModel;
		this.bdvListener = new BdvListener( bdv );
		this.labelingPlus = labelingPlus;
		this.segmentsUnderMouse = segmentsUnderMouse;
		this.highlightModel = highlightModel;

		segmentsOrdered = RefCollections.createRefList( segmentGraph.vertices() );
		currentSegment = null;

		this.mml = new MouseMotionListener() {

			@Override
			public void mouseDragged( final MouseEvent e ) {
			}

			@Override
			public void mouseMoved( final MouseEvent e ) {
				final int x = e.getX();
				final int y = e.getY();
				findSegments( x, y );
			}
		};
		bdv.getBdvHandle().getViewerPanel().getDisplay().addMouseMotionListener( this.mml );

		final TriggerBehaviourBindings bindings = bdv.getBdvHandle().getTriggerbindings();
		final Behaviours behaviours = new Behaviours( inputConf, new String[] { "tr2d" } );
		behaviours.behaviour(
				new ScrollBehaviour() {
					@Override
					public void scroll( final double wheelRotation, final boolean isHorizontal, final int x, final int y ) {
						if ( !isHorizontal )
							browseSegments( x, y, wheelRotation > 0 );
					}
				},
				"browse segments",
				"shift scroll" );
		behaviours.behaviour(
				new ClickBehaviour() {
					@Override
					public void click( final int arg0, final int arg1 ) {
						if ( currentSegment == null ) {
							selectionModel.clearSelection();
						} else {
							toggleSelectionOfCurrentSegment();
						}
					}
				},
				"select current segment",
				"button1" );
		behaviours.install( bindings, "segments" );
	}

	public synchronized void toggleSelectionOfCurrentSegment() {
		if ( currentSegment != null )
		{
			selectionModel.pauseListeners();
			if ( isCliqueSelected( currentSegment ) ) {
				// unselect entire clique
				final List< SegmentVertex > conflicting = new ArrayList<>();
				new BreadthFirstIterator<>( currentSegment, segmentGraph ).forEachRemaining( v -> conflicting.add( v ) );
				new InverseBreadthFirstIterator<>( currentSegment, segmentGraph ).forEachRemaining( v -> conflicting.add( v ) );
				for ( final SegmentVertex v : conflicting )
					selectionModel.setSelected( v, false );
				selectionModel.setSelected( currentSegment, false );
			} else if ( selectionModel.isSelected( currentSegment ) ) {
				final List< SegmentVertex > conflicting = new ArrayList<>();
				new BreadthFirstIterator<>( currentSegment, segmentGraph ).forEachRemaining( v -> conflicting.add( v ) );
				new InverseBreadthFirstIterator<>( currentSegment, segmentGraph ).forEachRemaining( v -> conflicting.add( v ) );
				for ( final SegmentVertex v : conflicting )
					selectionModel.setSelected( v, true );
			} else {
				final List< SegmentVertex > conflicting = new ArrayList<>();
				new BreadthFirstIterator<>( currentSegment, segmentGraph ).forEachRemaining( v -> conflicting.add( v ) );
				new InverseBreadthFirstIterator<>( currentSegment, segmentGraph ).forEachRemaining( v -> conflicting.add( v ) );
				for ( final SegmentVertex v : conflicting )
					selectionModel.setSelected( v, false );
				selectionModel.setSelected( currentSegment, true );
			}
			selectionModel.resumeListeners();
		}
	}


	/**
	 * @param currentSegment
	 * @return true if the currentSegment and all conflicting segments are
	 *         currently selected.
	 */
	private boolean isCliqueSelected( final SegmentVertex currentSegment ) {
		if ( !selectionModel.isSelected( currentSegment ) ) { return false; }
		final List< SegmentVertex > conflicting = new ArrayList<>();
		new BreadthFirstIterator<>( currentSegment, segmentGraph ).forEachRemaining( v -> conflicting.add( v ) );
		new InverseBreadthFirstIterator<>( currentSegment, segmentGraph ).forEachRemaining( v -> conflicting.add( v ) );
		for ( final SegmentVertex v : conflicting ) {
			if ( !selectionModel.isSelected( v ) ) { return false; }
		}
		return true;
	}

	public synchronized void browseSegments( final int x, final int y, final boolean forward ) {
		if ( currentSegment != null ) {
			int i = segmentsOrdered.indexOf( currentSegment );
			if ( forward )
				i = Math.min( segmentsOrdered.size() - 1, i + 1 );
			else
				i = Math.max( 0, i - 1 );
			currentSegment = segmentsOrdered.get( i );
			highlightModel.highlightVertex( currentSegment );
		}
	}

	public synchronized void findSegments( final int x, final int y ) {
		final ImgLabeling< LabelData, IntType > labeling = labelingPlus.getLabeling();

		final AffineTransform2D transform = new AffineTransform2D();
		bdvListener.getViewerTransform2D( transform );

		final RandomAccess< LabelingType< LabelData > > a =
				RealViews.affine(
						Views.interpolate(
								Views.extendValue(
										labeling,
										labeling.firstElement().createVariable() ),
								new NearestNeighborInterpolatorFactory<>() ),
						transform ).randomAccess();

		a.setPosition( new int[] { x, y } );

		final Set< LabelData > previouslySelectedLabels = new HashSet<>();
		for ( final SegmentVertex v : segmentsUnderMouse.getSelectedVertices() )
			previouslySelectedLabels.add( v.getLabelData() );

		if ( !a.get().equals( previouslySelectedLabels ) ) {
			segmentsUnderMouse.pauseListeners();
			segmentsUnderMouse.clearSelection();
			for ( final LabelData label : a.get() )
				segmentsUnderMouse.setSelected( segmentGraph.getVertexForLabel( label ), true );
			segmentsUnderMouse.resumeListeners();

			segmentsOrdered.clear();
			segmentsOrdered.addAll( segmentsUnderMouse.getSelectedVertices() );
			if ( segmentsOrdered.isEmpty() ) {
				currentSegment = null;
			} else {
				segmentsOrdered.sort(
						( s1, s2 ) -> ( int ) ( s1.getLabelData().getSegment().getArea() - s2.getLabelData().getSegment().getArea() ) );
//				if ( !segmentsOrdered.contains( currentSegment ) )
					currentSegment = segmentsOrdered.get( 0 );
			}

			highlightModel.highlightVertex( currentSegment );
		}
	}

	/**
	 * Removes itself from the bdv it was created for. (The one given during
	 * construction...)
	 */
	public void unregister() {
		try {
			bdv.getBdvHandle().getViewerPanel().getDisplay().removeMouseMotionListener( this.mml );
		} catch ( final NullPointerException npe ) {
			Tr2dLog.log.error( "NullPointerException in SegmentBrowser::unregister" );
		}
	}
}

class BdvListener implements TransformListener< AffineTransform3D >, TimePointListener
{
	private final AffineTransform3D viewerTransform = new AffineTransform3D();

	private int timePointIndex;

	public BdvListener( final Bdv bdv ) {
		final ViewerPanel viewerPanel = bdv.getBdvHandle().getViewerPanel();
		viewerPanel.addRenderTransformListener( this );
		viewerPanel.addTimePointListener( this );
	}

	@Override
	public void timePointChanged( final int timePointIndex )
	{
		this.timePointIndex = timePointIndex;
	}

	@Override
	public void transformChanged( final AffineTransform3D transform ) {
		synchronized( viewerTransform )
		{
			viewerTransform.set( transform );
		}
	}

	public void getViewerTransform2D( final AffineTransform2D transform )
	{
		synchronized ( viewerTransform ) {
			transform.set(
					viewerTransform.get( 0, 0 ), viewerTransform.get( 0, 1 ), viewerTransform.get( 0, 3 ),
					viewerTransform.get( 1, 0 ), viewerTransform.get( 1, 1 ), viewerTransform.get( 1, 3 ) );
		}
	}
}
