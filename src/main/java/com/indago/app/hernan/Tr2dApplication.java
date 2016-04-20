/**
 *
 */
package com.indago.app.hernan;

import java.awt.Image;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.apple.eawt.Application;
import com.indago.tr2d.Tr2dProperties;
import com.indago.tr2d.projectfolder.ProjectData;
import com.indago.tr2d.ui.model.Tr2dModel;
import com.indago.tr2d.ui.util.OsDependentFileChooser;
import com.indago.tr2d.ui.view.Tr2dMainPanel;
import com.indago.util.OSValidator;

import gurobi.GRBEnv;
import gurobi.GRBException;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import weka.gui.ExtensionFileFilter;

/**
 * Starts the tr2d app.
 *
 * @author jug
 */
public class Tr2dApplication {

	private static JFrame guiFrame;
	private static Tr2dMainPanel mainPanel;

	private static File projectFolderBasePath;
	private static File inputStack;
	private static File fileUserProps;
	private static int minTime = 0;
	private static int maxTime = Integer.MAX_VALUE;
	private static int initOptRange = Integer.MAX_VALUE;

	public static void main( final String[] args ) {

		guiFrame = new JFrame( "tr2d" );
		setImageAppIcon();
		new ImageJ();

		parseCommandLineArgs( args );
		checkGurobiAvailability();

		if ( inputStack == null ) {
			inputStack = OsDependentFileChooser.showLoadFileChooser(
					guiFrame,
					Tr2dProperties.RAW_DATA_PATH,
					"Load input tiff stack...",
					new ExtensionFileFilter( "tif", "TIFF Image Stack" ) );
			if ( inputStack == null ) {
				System.exit( 1 );
			}
		}
		IJ.open( inputStack.getAbsolutePath() );
		if ( projectFolderBasePath == null ) {
			boolean validSelection = false;
			while ( !validSelection ) {
				projectFolderBasePath = OsDependentFileChooser.showLoadFolderChooser(
						guiFrame,
						Tr2dProperties.DEFAULT_BASE_PATH,
						"Choose tr2d project folder..." );
				if ( projectFolderBasePath == null ) {
					System.exit( 2 );
				}
				if ( ProjectData.RAW_DATA.existsIn( projectFolderBasePath.getAbsolutePath() ) ) {
					final String msg = String.format(
							"Chosen project folder exists (%s)./nShould this project be overwritten?\nAll data in this project will be overwritten...",
							projectFolderBasePath );
					final int overwrite = JOptionPane.showConfirmDialog( guiFrame, msg, "Project Folder Exists", JOptionPane.YES_NO_OPTION );
					if ( overwrite == JOptionPane.YES_OPTION ) {
						ProjectData.restartWith( projectFolderBasePath, inputStack.getAbsolutePath() );
						validSelection = true;
					}
				} else {
					validSelection = true;
				}
			}
		}

		final ImagePlus imgPlus = WindowManager.getCurrentImage();
		if ( imgPlus == null ) {
			IJ.error( "There must be an active, open window!" );
			// System.exit( 1 );
			return;
		}

		final Tr2dModel model = new Tr2dModel( imgPlus );
		mainPanel = new Tr2dMainPanel( guiFrame, model );

		guiFrame.add( mainPanel );
		guiFrame.setVisible( true );
	}

	/**
	 *
	 */
	private static void setImageAppIcon() {
		Image image = null;
		try {
			image = new ImageIcon( Tr2dApplication.class.getClassLoader().getResource( "IconMpiCbg128.png" ) ).getImage();
		} catch ( final Exception e ) {
			try {
				image = new ImageIcon( Tr2dApplication.class.getClassLoader().getResource(
						"resources/IconMpiCbg128.png" ) ).getImage();
			} catch ( final Exception e2 ) {
				System.out.println( ">>> Error: app icon not found..." );
			}
		}

		if ( image != null ) {
			if ( OSValidator.isMac() ) {
				System.out.println( "On a Mac! --> trying to set icons..." );
				Application.getApplication().setDockIconImage( image );
			} else {
				System.out.println( "Not a Mac! --> trying to set icons..." );
				guiFrame.setIconImage( image );
			}
		}
	}

	/**
	 * Check if GRBEnv can be instantiated. For this to work Gurobi has to be
	 * installed and a valid license has to be pulled.
	 */
	private static void checkGurobiAvailability() {
		final String jlp = System.getProperty( "java.library.path" );
//		System.out.println( jlp );
		try {
			new GRBEnv();
		} catch ( final GRBException e ) {
			final String msgs = "Initial Gurobi test threw exception... check your Gruobi setup!\n\nJava library path: " + jlp;
			JOptionPane.showMessageDialog(
					guiFrame,
					msgs,
					"Gurobi Error?",
					JOptionPane.ERROR_MESSAGE );
			e.printStackTrace();
			System.exit( 98 );
		} catch ( final UnsatisfiedLinkError ulr ) {
			final String msgs =
					"Could initialize Gurobi.\n" + "You might not have installed Gurobi properly or you miss a valid license.\n" + "Please visit 'www.gurobi.com' for further information.\n\n" + ulr
							.getMessage() + "\nJava library path: " + jlp;
			JOptionPane.showMessageDialog(
					guiFrame,
					msgs,
					"Gurobi Error?",
					JOptionPane.ERROR_MESSAGE );
			ulr.printStackTrace();
			System.out.println( "\n>>>>> Java library path: " + jlp + "\n" );
			System.exit( 99 );
		}
	}

	/**
	 * Parse command line arguments and set static variables accordingly.
	 *
	 * @param args
	 */
	private static void parseCommandLineArgs( final String[] args ) {
		final String helpMessageLine1 =
				"Tr2d args: [-uprops properties-file] -p project-folder [-i input-stack] [-tmin idx] [-tmax idx] [-orange num-frames]";

		// create Options object & the parser
		final Options options = new Options();
		final CommandLineParser parser = new BasicParser();
		// defining command line options
		final Option help = new Option( "help", "print this message" );

		final Option timeFirst = new Option( "tmin", "min_time", true, "first time-point to be processed" );
		timeFirst.setRequired( false );

		final Option timeLast = new Option( "tmax", "max_time", true, "last time-point to be processed" );
		timeLast.setRequired( false );

		final Option optRange = new Option( "orange", "opt_range", true, "initial optimization range" );
		optRange.setRequired( false );

		final Option projectfolder = new Option( "p", "projectfolder", true, "tr2d project folder" );
		projectfolder.setRequired( false );

		final Option instack = new Option( "i", "input", true, "tiff stack to be read" );
		instack.setRequired( false );

		final Option userProps = new Option( "uprops", "userprops", true, "user properties file to be loaded" );
		userProps.setRequired( false );

		options.addOption( help );
		options.addOption( timeFirst );
		options.addOption( timeLast );
		options.addOption( optRange );
		options.addOption( instack );
		options.addOption( projectfolder );
		options.addOption( userProps );
		// get the commands parsed
		CommandLine cmd = null;
		try {
			cmd = parser.parse( options, args );
		} catch ( final ParseException e1 ) {
			final HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(
					helpMessageLine1,
					"",
					options,
					"Error: " + e1.getMessage() );
			System.exit( 0 );
		}

		if ( cmd.hasOption( "help" ) ) {
			final HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( helpMessageLine1, options );
			System.exit( 0 );
		}

		projectFolderBasePath = null;
		if ( cmd.hasOption( "p" ) ) {
			projectFolderBasePath = new File( cmd.getOptionValue( "p" ) );
			if ( !projectFolderBasePath.exists() ) {
				final String msg = "Given project folder does not exist!";
				JOptionPane.showMessageDialog( guiFrame, msg, "Argument Error", JOptionPane.ERROR_MESSAGE );
				System.out.println( "ERROR: " + msg );
				System.exit( 1 );
			}
			if ( !projectFolderBasePath.isDirectory() ) {
				final String msg = "Given project folder is not a folder!";
				JOptionPane.showMessageDialog( guiFrame, msg, "Argument Error", JOptionPane.ERROR_MESSAGE );
				System.out.println( "ERROR: " + msg );
				System.exit( 2 );
			}
			if ( !projectFolderBasePath.canWrite() ) {
				final String msg = "Given project folder cannot be written to!";
				JOptionPane.showMessageDialog( guiFrame, msg, "Argument Error", JOptionPane.ERROR_MESSAGE );
				System.out.println( "ERROR: " + msg );
				System.exit( 3 );
			}
		}

		inputStack = null;
		if ( cmd.hasOption( "i" ) ) {
			inputStack = new File( cmd.getOptionValue( "i" ) );
			if ( !inputStack.isFile() ) {
				final String msg = "Given input tiff stack could not be found!";
				JOptionPane.showMessageDialog( guiFrame, msg, "Argument Error", JOptionPane.ERROR_MESSAGE );
				System.out.println( "ERROR: " + msg );
				System.exit( 4 );
			}
			if ( !inputStack.canRead() ) {
				final String msg = "Given input tiff stack is not readable!";
				JOptionPane.showMessageDialog( guiFrame, msg, "Argument Error", JOptionPane.ERROR_MESSAGE );
				System.out.println( "ERROR: " + msg );
				System.exit( 5 );
			}
		} else if ( projectFolderBasePath != null ) { // if a project folder was given load data from there!
			inputStack = new File( ProjectData.RAW_DATA.getAbsPathIn( projectFolderBasePath.getAbsolutePath() ) );
			if ( !inputStack.canRead() ) {
				final String msg = String.format( "No raw tiff stack found in given project folder (%s)!", projectFolderBasePath );
				JOptionPane.showMessageDialog( guiFrame, msg, "Argument Error", JOptionPane.ERROR_MESSAGE );
				System.out.println( "ERROR: " + msg );
				System.exit( 6 );
			}
		}

		fileUserProps = null;
		if ( cmd.hasOption( "uprops" ) ) {
			fileUserProps = new File( cmd.getOptionValue( "uprops" ) );
			if ( !inputStack.canRead() ) {
				final String msg = String.format( "User properties file not readable (%s). Continue without...", fileUserProps.getAbsolutePath() );
				JOptionPane.showMessageDialog( guiFrame, msg, "Argument Warning", JOptionPane.WARNING_MESSAGE );
				System.out.println( "WARNING: " + msg );
			}
		}

		if ( cmd.hasOption( "tmin" ) ) {
			minTime = Integer.parseInt( cmd.getOptionValue( "tmin" ) );
			if ( minTime < 0 ) {
				final String msg = "Argument 'tmin' cannot be smaller than 0... using tmin=0...";
				JOptionPane.showMessageDialog( guiFrame, msg, "Argument Warning", JOptionPane.WARNING_MESSAGE );
				System.out.println( "WARNING: " + msg );
			}
		}
		if ( cmd.hasOption( "tmax" ) ) {
			maxTime = Integer.parseInt( cmd.getOptionValue( "tmax" ) );
			if ( maxTime < minTime ) {
				maxTime = minTime + 1;
				final String msg = String.format( "Argument 'tmax' cannot be smaller than 'tmin'... using tmax=%d...", maxTime );
				JOptionPane.showMessageDialog( guiFrame, msg, "Argument Warning", JOptionPane.WARNING_MESSAGE );
				System.out.println( "WARNING: " + msg );
			}
		}

		if ( cmd.hasOption( "orange" ) ) {
			initOptRange = Integer.parseInt( cmd.getOptionValue( "orange" ) );
			if ( initOptRange > maxTime - minTime ) {
				initOptRange = maxTime - minTime;
				final String msg =
						String.format( "Argument 'orange' (initial optimization range in frames) too large... using %d instead...", initOptRange );
				JOptionPane.showMessageDialog( guiFrame, msg, "Argument Warning", JOptionPane.WARNING_MESSAGE );
				System.out.println( "WARNING: " + msg );
			}
		}
	}
}
