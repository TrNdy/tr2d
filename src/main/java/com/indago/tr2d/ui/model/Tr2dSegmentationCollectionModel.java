/**
 *
 */
package com.indago.tr2d.ui.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import com.indago.io.ProjectFolder;
import com.indago.tr2d.Tr2dContext;
import com.indago.tr2d.Tr2dLog;
import com.indago.tr2d.io.projectfolder.Tr2dProjectFolder;
import com.indago.tr2d.plugins.seg.Tr2dSegmentationPlugin;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.IntType;

/**
 * @author jug
 */
public class Tr2dSegmentationCollectionModel implements AutoCloseable {

	private final Tr2dModel model;

	private final ProjectFolder projectFolder;

	private final List< Tr2dSegmentationPlugin > plugins = new ArrayList<>();

	public Tr2dSegmentationCollectionModel( final Tr2dModel model ) {
		this.model = model;
		projectFolder = model.getProjectFolder().getFolder( Tr2dProjectFolder.SEGMENTATION_FOLDER );
	}

	public ProjectFolder getProjectFolder() {
		return projectFolder;
	}

	public Tr2dModel getModel() {
		return model;
	}

	public void addPlugin( final Tr2dSegmentationPlugin segPlugin ) {
		this.plugins.add( segPlugin );
	}

	public List< Tr2dSegmentationPlugin > getPlugins() {
		return plugins;
	}

	public List< RandomAccessibleInterval< IntType > > getSumImages() {
		final List< RandomAccessibleInterval< IntType > > ret = new ArrayList< RandomAccessibleInterval< IntType > >();
		for ( final Tr2dSegmentationPlugin plugin : plugins ) {
			ret.addAll( plugin.getOutputs() );
		}
		return ret;
	}

	@Override
	public void close() {
		for(Tr2dSegmentationPlugin plugin : this.plugins)
			try {
				plugin.close();
			}
			catch (Exception e) {
				Tr2dLog.log.warn("Exception while closing: " + plugin.getUiName(), e);
			}
	}
}
