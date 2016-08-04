import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import com.indago.app.hernan.Tr2dApplication;

import net.imagej.ops.OpService;

/**
 * Tr2d Plugin for Fiji/ImageJ2
 *
 * @author Florian Jug
 */

@Plugin( type = Command.class, headless = true, menuPath = "Plugins>Tracking>Tr2d (alpha)" )
public class Tr2dPlugin implements Command {

	@Parameter
	private OpService ops;

	/**
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		Tr2dApplication.isStandalone = false;
		Tr2dApplication.ops = ops;
		Tr2dApplication.main( null );
	}
}
