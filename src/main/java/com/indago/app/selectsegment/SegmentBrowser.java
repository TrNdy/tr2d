package com.indago.app.selectsegment;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import org.scijava.ui.behaviour.ScrollBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;

import com.indago.data.segmentation.LabelingPlus;

import bdv.util.Bdv;
import bdv.viewer.TimePointListener;
import bdv.viewer.TriggerBehaviourBindings;
import bdv.viewer.ViewerPanel;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.TransformListener;
import net.trackmate.revised.bdv.AbstractBehaviours;

public class SegmentBrowser
{
	final private Bdv bdv;

	final private BdvListener bdvListener;

	final private LabelingPlus labelingPlus;

	public SegmentBrowser(
			final Bdv bdv,
			final LabelingPlus labelingPlus,
			final InputTriggerConfig inputConf )
	{
		this.bdv = bdv;
		this.bdvListener = new BdvListener( bdv );
		this.labelingPlus = labelingPlus;


		bdv.getBdvHandle().getViewerPanel().getDisplay().addMouseMotionListener( l );

		final TriggerBehaviourBindings bindings = bdv.getBdvHandle().getTriggerbindings();
		final AbstractBehaviours behaviours = new AbstractBehaviours( bindings, "segments", inputConf, new String[] { "tr2d" } );
		behaviours.behaviour(
				new ScrollBehaviour() {
					@Override
					public void scroll( final double wheelRotation, final boolean isHorizontal, final int x, final int y ) {
						findSegments( x, y );
					}
				},
				"browse segments",
				"scroll" );
	}

	public void findSegments( final int x, final int y )
	{
		System.out.println( "findSegments (" + x + ", " + y + "):" );

//		final ImgLabeling< LabelData, IntType > labeling = labelingPlus.getLabeling();
//
//		final AffineTransform2D transform = new AffineTransform2D();
//		bdvListener.getViewerTransform2D( transform );
//
//		final RandomAccess< LabelingType< LabelData > > a =
//				RealViews.affine(
//						Views.interpolate(
//								Views.extendValue(
//										labeling,
//										labeling.firstElement().createVariable() ),
//								new NearestNeighborInterpolatorFactory< >() ),
//						transform ).randomAccess();
//
//		a.setPosition( new int[] { x, y } );
//		for ( final LabelData label : a.get() )
//			System.out.println( "   " + label.getSegment() );
	}


	class MML implements MouseMotionListener
	{
		@Override
		public void mouseDragged( final MouseEvent e ) {
		}

		@Override
		public void mouseMoved( final MouseEvent e ) {
			final int x = e.getX();
			final int y = e.getY();
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
