import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import com.indago.app.hernan.Tr2dApplication;

import ij.ImagePlus;

/**
 * Tr2d Plugin for Fiji/ImageJ2
 *
 * @author Florian Jug
 */

@Plugin( type = Command.class, headless = true, menuPath = "Plugins>Tracking>Tr2d (alpha)" )
public class Tr2dPlugin implements Command {
	protected ImagePlus image;

	/**
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		Tr2dApplication.isStandalone = false;
		Tr2dApplication.main( null );
	}
}
