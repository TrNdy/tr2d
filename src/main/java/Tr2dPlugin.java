import com.indago.app.hernan.Tr2dApplication;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;

/**
 * tr2d
 *
 * @author Florian Jug
 */
public class tr2d_ implements PlugIn {
	protected ImagePlus image;

	/**
	 * @see ij.plugin.PlugIn#run(java.lang.String)
	 */
	@Override
	public void run( final String arg ) {
		Tr2dApplication.main( null );
	}

	/**
	 * Main method for debugging.
	 *
	 * For debugging, it is convenient to have a method that starts ImageJ,
	 * loads an
	 * image and calls the plugin, e.g. after setting breakpoints.
	 *
	 * @param args
	 *            unused
	 */
	public static void main(final String[] args) {
		// set the plugins.dir property to make the plugin appear in the Plugins menu
		final Class<?> clazz = tr2d_.class;
		final String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		final String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
		System.setProperty("plugins.dir", pluginsDir);

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	}

}
