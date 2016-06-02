package com.indago.app.selectsegment;

import java.io.IOException;

import org.scijava.ui.behaviour.ScrollBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.io.yaml.YamlConfigIO;

import com.indago.data.segmentation.LabelData;
import com.indago.data.segmentation.LabelingPlus;
import com.indago.data.segmentation.XmlIoLabelingPlus;
import com.indago.data.segmentation.visualization.ColorStream;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.viewer.TimePointListener;
import bdv.viewer.TriggerBehaviourBindings;
import bdv.viewer.ViewerPanel;
import net.imglib2.RandomAccess;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.ui.TransformListener;
import net.imglib2.view.Views;
import net.trackmate.revised.bdv.AbstractBehaviours;

public class SelectSegments {

	LabelingPlus labelingPlus;

	Bdv bdv;

	BdvListener bdvListener;

	public void run() throws IOException
	{
//		final InputTriggerConfig config = new InputTriggerConfig( YamlConfigIO.read( "/Users/pietzsch/Desktop/tr2d.yaml" ) );
		final InputTriggerConfig config = new InputTriggerConfig( YamlConfigIO.read( "/Users/jug/MPI/ProjectHernan/Tr2dProjectPath/tr2d.yaml" ) );
//		final InputTriggerConfig config = new InputTriggerConfig();

//		final String folder = "/Users/pietzsch/Desktop/data/tr2d/tr2d_project_folder/DebugStack03-crop/tracking/labeling_frames/";
		final String folder = "/Users/jug/MPI/ProjectHernan/Tr2dProjectPath/DebugStack03-crop/tracking/labeling_frames/";
		final String fLabeling = folder + "labeling_frame0000.xml";

		labelingPlus = new XmlIoLabelingPlus().load( fLabeling );

//		Converters.convert( lp.getLabeling().getIndexImg(), new ColorFragments(), new ARGBType() );
//		final Bdv bdv = BdvFunctions.show( lp.getLabeling().getIndexImg(), "img", Bdv.options().is2D() );

		bdv = BdvFunctions.show(
				Converters.convert(
						labelingPlus.getLabeling().getIndexImg(),
						( i, o ) -> o.set( ColorStream.get( i.get() ) ),
						new ARGBType() ),
				"fragments",
				Bdv.options().inputTriggerConfig( config ).is2D() );

		bdvListener = new BdvListener( bdv );

		final TriggerBehaviourBindings bindings = bdv.getBdvHandle().getTriggerbindings();
		final AbstractBehaviours behaviours = new AbstractBehaviours( bindings, "segments", config, new String[] { "tr2d" } );
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

		final ImgLabeling< LabelData, IntType > labeling = labelingPlus.getLabeling();

		final AffineTransform2D transform = new AffineTransform2D();
		bdvListener.getViewerTransform2D( transform );

		final RandomAccess< LabelingType< LabelData > > a =
				RealViews.affine(
						Views.interpolate(
								Views.extendValue(
										labeling,
										labeling.firstElement().createVariable() ),
								new NearestNeighborInterpolatorFactory< >() ),
						transform ).randomAccess();

		a.setPosition( new int[] { x, y } );
		for ( final LabelData label : a.get() )
			System.out.println( "   " + label.getSegment() );
	}

	public static void main( final String[] args ) throws IOException {
		new SelectSegments().run();
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

class ColorFragments implements Converter< IntType, ARGBType > {
	@Override
	public void convert( final IntType input, final ARGBType output ) {
		output.set( ColorStream.get( input.get() ) );
	}
}
