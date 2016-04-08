import com.indago.tr2d.ui.view.Tr2dMainPanel;

import ij.IJ;
import ij.ImageJ;
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
		Tr2dMainPanel.main( null );
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

		// start ImageJ
		new ImageJ();

//		final ImagePlus image = IJ.openImage(
//				"/Users/jug/MPI/ProjectHernan/DebugStack00.tif" );
//		final ImagePlus image = IJ.openImage(
//				"/Users/jug/MPI/ProjectHernan/DebugStack03-crop1.tif" );
		final ImagePlus image = IJ.openImage(
				"/Users/jug/MPI/ProjectHernan/DebugStack03.tif" );
//		final ImagePlus image = IJ.openImage(
//				"/Volumes/FastData/ProjectHernan/HarderDataSet/2015-03-27-P2P-MS2-PP7-lacZ-His_023.tif" );
		image.show();

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	}

}
