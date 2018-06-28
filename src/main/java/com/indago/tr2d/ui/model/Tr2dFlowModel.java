/**
 *
 */
package com.indago.tr2d.ui.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.indago.flow.MSEBlockFlow;
import com.indago.io.FloatTypeImgLoader;
import com.indago.io.ImageSaver;
import com.indago.io.ProjectFile;
import com.indago.io.ProjectFolder;
import com.indago.tr2d.Tr2dContext;
import com.indago.tr2d.Tr2dLog;
import com.indago.tr2d.io.projectfolder.Tr2dProjectFolder;
import com.indago.ui.bdv.BdvWithOverlaysOwner;

import bdv.util.BdvHandlePanel;
import bdv.util.BdvOverlay;
import bdv.util.BdvSource;
import ij.IJ;
import ij.ImagePlus;
import io.scif.img.ImgIOException;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.ValuePair;
import net.imglib2.view.Views;

/**
 * @author jug
 */
public class Tr2dFlowModel implements BdvWithOverlaysOwner {

	private static String SCALE = "scale";
	private static String RADIUS = "radius";
	private static String MAX_DIST = "max_dist";

	private final Tr2dModel model;

	private final ProjectFolder projectFolder;

	private final ProjectFile filePropsParamValues;
	private final Properties propsParamValues;

	private final ProjectFile fileScaledInput;
	private final ProjectFile fileScaledFlow;
	private final ProjectFile fileFlow;

	private BdvHandlePanel bdvHandlePanel;
	private final List< BdvSource > bdvSources = new ArrayList<>();
	private final List< RandomAccessibleInterval< FloatType > > imgs = new ArrayList<>();
	private final List< BdvOverlay > overlays = new ArrayList<>();
	private final List< BdvSource > bdvOverlaySources = new ArrayList<>();

	private double scaleFactor = .25;
	private int blockRadius = 15;
	private int maxDistance = 15;

	public Tr2dFlowModel( final Tr2dModel model ) {
		this.model = model;
		projectFolder = model.getProjectFolder().getFolder( Tr2dProjectFolder.FLOW_FOLDER );
		if ( !projectFolder.exists() ) {
			projectFolder.getFolder().mkdir();
		}
		filePropsParamValues = projectFolder.addFile( "guiValues.props" );
		propsParamValues = loadParameterProps( filePropsParamValues.getFile() );

		fileScaledInput = projectFolder.addFile( "input_scaled.tif" );
		fileFlow = projectFolder.addFile( "flow.tif" );
		fileScaledFlow = projectFolder.addFile( "flow_scaled.tif" );

		if ( !fileFlow.canRead() ) {
			Tr2dLog.log.info( "Flow subfolder does not contain a file 'flow.tif'." );
		} else {
			try {
				imgs.add( FloatTypeImgLoader.loadTiff( fileFlow.getFile() ) );
			} catch ( final ImgIOException e ) {
				e.printStackTrace();
			}
		}
	}

	public ProjectFolder getProjectFolder() {
		return projectFolder;
	}

	public Tr2dModel getModel() {
		return model;
	}

	public RandomAccessibleInterval< FloatType > getFlowImage() {
		if ( imgs.size() > 0 )
			return imgs.get( 0 );
		else
			return null;
	}

	public RandomAccessibleInterval< FloatType > getFlowImage( final int t ) {
		return Views.hyperSlice( imgs.get( 0 ), 3, t );
	}

	public ValuePair< Double, Double > getFlowVector( final int t, final int x, final int y ) {
		final double[] polar = new double[ 2 ];

		if ( imgs.size() == 0 ) {
			return new ValuePair<>( 0.0, 0.0 );
		} else {
			final RandomAccess< FloatType > ra = imgs.get( 0 ).randomAccess();

			final int[] pos_for_x_direction = new int[] { x, y, 0, t };
			ra.setPosition( pos_for_x_direction );
			polar[ 0 ] = ra.get().get();

			final int[] pos_for_y_direction = new int[] { x, y, 1, t };
			ra.setPosition( pos_for_y_direction );
			polar[ 1 ] = ra.get().get();

			// GET dx, dy FROM POLAR (attention to NaN)
			double dx;
			double dy;
			if ( polar[ 0 ] == 0 || Double.isNaN( polar[ 1 ] ) ) {
				dx = 0;
				dy = 0;
			} else {
				dx = polar[ 0 ] * Math.sin( polar[ 1 ] );
				dy = polar[ 0 ] * Math.cos( polar[ 1 ] );
			}

			return new ValuePair<>( dx, dy );
		}
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

	public boolean hasFlowLoaded() {
		if ( imgs.size() == 1 )
			return true;
		return false;
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

	public void computeAndStoreFlow() {
		// make the parameter values persistent
		saveParameterProps( filePropsParamValues.getFile() );

		final MSEBlockFlow flowMagic = new MSEBlockFlow();

		//scaling
		final Img< FloatType > img = ImageJFunctions.convertFloat( model.getImgPlus() );

		final RandomAccessibleInterval< FloatType > imgScaled =
				scale( img, new double[] { scaleFactor, scaleFactor, 1 }, new NLinearInterpolatorFactory<>(), Tr2dContext.ops );

//		final ImagePlus scaledImagePlus = ImageJFunctions.show( imgScaled );

		ImageSaver.saveAsTiff( fileScaledInput.getAbsolutePath(), imgScaled );

		//final ImagePlus scaledImagePlus = new ImgOpener().openImgs( fileScaledInput.getAbsolutePath() )
		//ImageJFunctions.show( img )
		final ImagePlus scaledImagePlus = ImageJFunctions.wrap( imgScaled, "scaled raw image" ).duplicate();
		flowMagic.computeAndStoreFlow(
				scaledImagePlus,
				getScaleFactor(),
				getBlockRadius(),
				( byte ) getMaxDistance(),
				fileScaledFlow.getAbsolutePath() );

		try {
			final Img< FloatType > scaledFlow = FloatTypeImgLoader.loadTiffEnsureType( fileScaledFlow.getFile() );

			//inverse scaling
			final RandomAccessibleInterval< FloatType > flow =
					scale(
							scaledFlow,
							new double[] { 1. / scaleFactor, 1. / scaleFactor, 1, 1 },
							new NearestNeighborInterpolatorFactory<>(),
							Tr2dContext.ops );

			final ImagePlus ip = ImageJFunctions.wrap( flow, "flow" );
			IJ.save( ip.duplicate(), fileFlow.getAbsolutePath() );
//			ImageSaver.saveAsTiff( fileFlow.getAbsolutePath(), flow );
			imgs.clear();
			imgs.add( flow );
		} catch ( final ImgIOException e ) {
			e.printStackTrace();
		}
	}

	// this can be removed as soo as the actual stupido bug was fixed which will happen in oct. 2016
	@SuppressWarnings( "unchecked" )
	public < T extends RealType< T > > RandomAccessibleInterval< T > scale(
			final RandomAccessibleInterval< T > in,
			final double[] scaleFactors,
			final InterpolatorFactory< T, RandomAccessible< T > > interpolator,
			final OpService ops ) {

		return ( RandomAccessibleInterval< T > ) ops.run(
				DefaultScaleView.class,
				in,
				scaleFactors,
				interpolator );

	}

	public int getBlockRadius() {
		return blockRadius;
	}

	public void setBlockRadius( final int blockRadius ) {
		this.blockRadius = blockRadius;
	}

	public int getMaxDistance() {
		return maxDistance;
	}

	public void setMaxDistance( final int maxDistance ) {
		this.maxDistance = maxDistance;
	}

	public double getScaleFactor() {
		return scaleFactor;
	}

	public void setScaleFactor( final double factor ) {
		this.scaleFactor = factor;
	}

	private Properties loadParameterProps( final File propsFile ) {
		InputStream is = null;
		final Properties props = new Properties();
		props.setProperty( Tr2dFlowModel.SCALE, "" + scaleFactor );
		props.setProperty( Tr2dFlowModel.RADIUS, "" + blockRadius );
		props.setProperty( Tr2dFlowModel.MAX_DIST, "" + maxDistance );

		// First try loading from the current directory
		try {
			is = new FileInputStream( propsFile );
			props.load( is );
		} catch ( final Exception e ) {
			Tr2dLog.log.warn( "No GUI props found for Tr2dFlowModel." );
		}

		scaleFactor = Double.parseDouble( props.getProperty( SCALE, "" + scaleFactor ) );
		blockRadius = Integer.parseInt( props.getProperty( RADIUS, "" + blockRadius ) );
		maxDistance = Integer.parseInt( props.getProperty( MAX_DIST, "" + maxDistance ) );

		return props;
	}

	public void saveParameterProps( final File propsFile ) {
		try {
			final OutputStream out = new FileOutputStream( propsFile );

			propsParamValues.setProperty( Tr2dFlowModel.SCALE, "" + this.scaleFactor );
			propsParamValues.setProperty( Tr2dFlowModel.RADIUS, "" + this.blockRadius );
			propsParamValues.setProperty( Tr2dFlowModel.MAX_DIST, "" + this.maxDistance );

			propsParamValues.store( out, "Tr2dFlowModel parameters" );
		} catch ( final Exception e ) {
			e.printStackTrace();
		}
	}

	/**
	 * Deletes scaled input, scaled flow, and actual flow files (if exist).
	 */
	public void removeFlowFiles() {
		imgs.clear();
		fileScaledInput.getFile().delete();
		fileScaledFlow.getFile().delete();
		fileFlow.getFile().delete();
	}
}
