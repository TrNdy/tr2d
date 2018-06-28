package com.indago.tr2d.plugins.seg;

import java.util.HashMap;
import java.util.Set;

import org.scijava.plugin.AbstractPTService;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.service.Service;

import com.indago.log.LoggingPanel;
import com.indago.tr2d.Tr2dLog;
import com.indago.tr2d.ui.model.Tr2dModel;

import net.imagej.ImageJService;

@Plugin(type = Service.class)
public class Tr2dSegmentationPluginService extends AbstractPTService< Tr2dSegmentationPlugin >
		implements ImageJService {

	private final HashMap< String, PluginInfo< Tr2dSegmentationPlugin > > plugins = new HashMap<>();

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

	public Tr2dSegmentationPlugin createPlugin( final String name, final Tr2dModel model, final LoggingPanel logPanel ) {
		// First, we get the animal plugin with the given name.
		final PluginInfo< Tr2dSegmentationPlugin > info = plugins.get( name );

		if ( info == null ) throw new IllegalArgumentException( "No segmentation plugin of that name!" );

		// Next, we use the plugin service to create an animal of that kind.
		final Tr2dSegmentationPlugin segPlugin = getPluginService().createInstance( info );
		segPlugin.setLogger( Tr2dLog.log.subLogger(name) );
		segPlugin.setTr2dModel( model );

		return segPlugin;
	}

	@Override
	public void initialize() {
		for ( final PluginInfo< Tr2dSegmentationPlugin > info : getPlugins() ) {
			String name = info.getName();
			if (name == null || name.isEmpty()) {
				name = info.getClassName();
			}
			// Add the plugin to the list of known animals.
			plugins.put( name, info );
		}
	}

	@Override
	public Class< Tr2dSegmentationPlugin > getPluginType() {
		return Tr2dSegmentationPlugin.class;
	}

}
