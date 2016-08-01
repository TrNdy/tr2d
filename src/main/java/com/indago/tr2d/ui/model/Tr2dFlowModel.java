/**
 *
 */
package com.indago.tr2d.ui.model;

import java.util.ArrayList;
import java.util.List;

import com.indago.flow.MSEBlockFlow;
import com.indago.io.FloatTypeImgLoader;
import com.indago.io.ProjectFile;
import com.indago.io.ProjectFolder;
import com.indago.io.projectfolder.Tr2dProjectFolder;
import com.indago.tr2d.ui.view.bdv.BdvWithOverlaysOwner;

import bdv.util.BdvHandlePanel;
import bdv.util.BdvOverlay;
import bdv.util.BdvSource;
import io.scif.img.ImgIOException;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.ValuePair;
import net.imglib2.view.Views;

/**
 * @author jug
 */
public class Tr2dFlowModel implements BdvWithOverlaysOwner {

	private final Tr2dModel model;

	private final ProjectFolder projectFolder;
	private final ProjectFile flowFile;

	private BdvHandlePanel bdvHandlePanel;
	private final List< BdvSource > bdvSources = new ArrayList<>();
	private final List< RandomAccessibleInterval< FloatType > > imgs = new ArrayList<>();
	private final List< BdvOverlay > overlays = new ArrayList<>();
	private final List< BdvSource > bdvOverlaySources = new ArrayList<>();

	private int blockRadius = 20;
	private int maxDistance = 15;

	/**
	 * @param model
	 */
	public Tr2dFlowModel( final Tr2dModel model ) {
		this.model = model;
		projectFolder = model.getProjectFolder().getFolder( Tr2dProjectFolder.FLOW_FOLDER );
		if ( !projectFolder.exists() ) {
			projectFolder.getFolder().mkdir();
		}
		flowFile = projectFolder.addFile( "flow.tif" );

		if ( !flowFile.canRead() ) {
			System.err
					.println( "\nINFO: Flow subfolder does not contain a file 'flow.tif'." );
		} else {
			try {
				imgs.add( FloatTypeImgLoader.loadTiff( flowFile.getFile() ) );
			} catch ( final ImgIOException e ) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * @return
	 */
	public ProjectFolder getProjectFolder() {
		return projectFolder;
	}

	/**
	 * @return
	 */
	public Tr2dModel getModel() {
		return model;
	}

	/**
	 * @return
	 */
	public RandomAccessibleInterval< FloatType > getFlowImage() {
		if ( imgs.size() > 0 )
			return imgs.get( 0 );
		else
			return null;
	}

	/**
	 * @return
	 */
	public RandomAccessibleInterval< FloatType > getFlowImage( final int t ) {
		return Views.hyperSlice( imgs.get( 0 ), 3, t );
	}

	/**
	 * @return
	 */
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

			//		if ( t == 2 ) {
			//			System.out.println( String.format( "t,x,y: %d,%d%d: polar=(%f,%f); cart=(%f,%f);", t, x, y, polar[ 0 ], polar[ 1 ], dx, dy ) );
			//		}

			return new ValuePair<>( dx, dy );
		}
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
	 * @return
	 */
	public boolean hasFlowLoaded() {
		if ( imgs.size() == 1 )
			return true;
		return false;
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
	 *
	 */
	public void computeAndStoreFlow() {
		final MSEBlockFlow flowMagic = new MSEBlockFlow();
		imgs.clear();
		imgs.add( flowMagic.computeAndStoreFlow( model.getImgPlus(), getBlockRadius(), ( byte ) getMaxDistance(), flowFile.getAbsolutePath() ) );
	}

	/**
	 * @return the blockRadius
	 */
	public int getBlockRadius() {
		return blockRadius;
	}

	/**
	 * @param blockRadius the blockRadius to set
	 */
	public void setBlockRadius( final int blockRadius ) {
		this.blockRadius = blockRadius;
	}

	/**
	 * @return the maxDistance
	 */
	public int getMaxDistance() {
		return maxDistance;
	}

	/**
	 * @param maxDistance the maxDistance to set
	 */
	public void setMaxDistance( final int maxDistance ) {
		this.maxDistance = maxDistance;
	}
}
