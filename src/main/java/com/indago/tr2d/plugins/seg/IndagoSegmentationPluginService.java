package com.indago.tr2d.plugins.seg;

import java.util.HashMap;
import java.util.Set;

import org.scijava.plugin.AbstractPTService;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.service.Service;

import com.indago.io.ProjectFolder;
import com.indago.log.LoggingPanel;
import com.indago.tr2d.Tr2dLog;
import com.indago.tr2d.ui.model.Tr2dModel;

import net.imagej.ImageJService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.DoubleType;

@Plugin(type = Service.class)
public class IndagoSegmentationPluginService extends AbstractPTService< IndagoSegmentationPlugin >
		implements ImageJService {

	private final HashMap< String, PluginInfo< IndagoSegmentationPlugin > > plugins = new HashMap<>();

	/**
	 * Gets the list of available plugin types.
	 * The names on this list can be passed to
	 * {@link #createPlugin(String, Tr2dModel, LoggingPanel)}}
	 * to create instances of that animal.
	 *
	 * @return a set of plugin names
	 */
	public Set< String > getPluginNames() {
		return plugins.keySet();
	}

	public IndagoSegmentationPlugin createPlugin(
			final String name,
			final ProjectFolder projectFolder,
			final RandomAccessibleInterval< DoubleType > raiData,
			final LoggingPanel logPanel ) {
		// First, we get the animal plugin with the given name.
		final PluginInfo< IndagoSegmentationPlugin > info = plugins.get( name );

		if ( info == null ) throw new IllegalArgumentException( "No segmentation plugin of that name!" );

		// Next, we use the plugin service to create an animal of that kind.
		final IndagoSegmentationPlugin segPlugin = getPluginService().createInstance( info );
		segPlugin.setLogger( Tr2dLog.log.subLogger(name) );
		segPlugin.setProjectFolderAndData( projectFolder, raiData );

		return segPlugin;
	}

	@Override
	public void initialize() {
		for ( final PluginInfo< IndagoSegmentationPlugin > info : getPlugins() ) {
			String name = info.getName();
			if (name == null || name.isEmpty()) {
				name = info.getClassName();
			}
			// Add the plugin to the list of known animals.
			plugins.put( name, info );
		}
	}

	@Override
	public Class< IndagoSegmentationPlugin > getPluginType() {
		return IndagoSegmentationPlugin.class;
	}

}
