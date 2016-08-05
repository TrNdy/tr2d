/**
 *
 */
package com.indago.app.hernan;

import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.scijava.Context;
import org.scijava.app.StatusService;
import org.scijava.io.IOService;

import com.apple.eawt.Application;
import com.indago.io.projectfolder.Tr2dProjectFolder;
import com.indago.tr2d.ui.model.Tr2dModel;
import com.indago.tr2d.ui.util.FrameProperties;
import com.indago.tr2d.ui.util.UniversalFileChooser;
import com.indago.tr2d.ui.view.Tr2dMainPanel;
import com.indago.util.OSValidator;

import gurobi.GRBEnv;
import gurobi.GRBException;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import io.scif.codec.CodecService;
import io.scif.formats.qt.QTJavaService;
import io.scif.formats.tiff.TiffService;
import io.scif.img.ImgUtilityService;
import io.scif.services.DatasetIOService;
import io.scif.services.JAIIIOService;
import io.scif.services.LocationService;
import io.scif.services.TranslatorService;
import net.imagej.DatasetService;
import net.imagej.ops.OpMatchingService;
import net.imagej.ops.OpService;
import weka.gui.ExtensionFileFilter;

/**
 * Starts the tr2d app.
 *
 * @author jug
 */
public class Tr2dApplication {

	/**
	 * true, iff this app is not started by the imagej2/fiji plugin (tr2d_)
	 */
	public static boolean isStandalone = true;

	private static JFrame guiFrame;
	private static Tr2dMainPanel mainPanel;

	private static File inputStack;
	private static Tr2dProjectFolder projectFolder;

	private static File fileUserProps;
	private static int minTime = 0;
	private static int maxTime = Integer.MAX_VALUE;
	private static int initOptRange = Integer.MAX_VALUE;

	public static OpService ops = null;

	public static void main( final String[] args ) {

		System.setProperty( "apple.laf.useScreenMenuBar", "true" );

		if ( isStandalone ) { // main NOT called via Tr2dPlugin
			System.out.println( "STANDALONE" );

			final ImageJ temp = IJ.getInstance();
			if ( temp == null ) {
				new ImageJ();
			}

			// Create context (since we did not receive one that was injected in 'Tr2dPlugin')
//			final Context context = new Context( OpService.class, OpMatchingService.class );
			final Context context = new Context( OpService.class, OpMatchingService.class,
					IOService.class, DatasetIOService.class, LocationService.class,
					DatasetService.class, ImgUtilityService.class, StatusService.class,
					TranslatorService.class, QTJavaService.class, TiffService.class,
					CodecService.class, JAIIIOService.class );
			ops = context.getService( OpService.class );
		} else {
			System.out.println( "COMMAND -- ops=" + ops.toString() );
		}

		checkGurobiAvailability();
		parseCommandLineArgs( args );

		guiFrame = new JFrame( "tr2d" );
		if ( isStandalone ) setImageAppIcon();

		final ImagePlus imgPlus = openStackOrProjectUserInteraction();
		final Tr2dModel model = new Tr2dModel( projectFolder, imgPlus );
		mainPanel = new Tr2dMainPanel( guiFrame, model );

		guiFrame.getContentPane().add( mainPanel );
		setFrameSizeAndCloseOperation();
		guiFrame.setVisible( true );
		mainPanel.collapseLog();
	}

	private static void setFrameSizeAndCloseOperation() {
		try {
			FrameProperties.load( projectFolder.getFile( Tr2dProjectFolder.FRAME_PROPERTIES ).getFile(), guiFrame );
		} catch ( final IOException e ) {
			System.err.println( "\nWARNING: Frame properties not found. Will use default values." );
			guiFrame.setBounds( FrameProperties.getCenteredRectangle( 1200, 1024 ) );
		}

		guiFrame.setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE );
		guiFrame.addWindowListener( new WindowAdapter() {

			@Override
			public void windowClosing( final WindowEvent we ) {
				final Object[] options = { "Quit", "Cancel" };
				final int choice = JOptionPane.showOptionDialog(
						guiFrame,
						"Do you really want to quit Tr2d?",
						"Quit?",
						JOptionPane.DEFAULT_OPTION,
						JOptionPane.QUESTION_MESSAGE,
						null,
						options,
						options[ 0 ] );
				if ( choice == 0 ) {
					try {
						FrameProperties.save( projectFolder.getFile( Tr2dProjectFolder.FRAME_PROPERTIES ).getFile(), guiFrame );
					} catch ( final Exception e ) {
						System.err.println( "ERROR: Could not save frame properties in project folder!" );
						e.printStackTrace();
					}
					Tr2dApplication.quit( 0 );
				}
			}
		} );
	}

	/**
	 * @param i
	 */
	public static void quit( final int exit_value ) {
		guiFrame.dispose();
		if ( isStandalone ) {
			System.exit( exit_value );
		}
	}

	/**
	 * @return
	 */
	private static ImagePlus openStackOrProjectUserInteraction() {
		UniversalFileChooser.showOptionPaneWithTitleOnMac = true;

		File projectFolderBasePath = null;
		if ( projectFolder != null ) projectFolderBasePath = projectFolder.getFolder();

		if ( inputStack == null ) {
			final Object[] options = { "Tr2d Project...", "TIFF Stack..." };
			final int choice = JOptionPane.showOptionDialog(
					guiFrame,
					"Please choose an input type to be opened.",
					"Open...",
					JOptionPane.DEFAULT_OPTION,
					JOptionPane.QUESTION_MESSAGE,
					null,
					options,
					options[ 0 ] );
			if ( choice == 0 ) {
				UniversalFileChooser.showOptionPaneWithTitleOnMac = false;
				projectFolderBasePath = UniversalFileChooser.showLoadFolderChooser(
						guiFrame,
						"",
						"Choose tr2d project folder..." );
				UniversalFileChooser.showOptionPaneWithTitleOnMac = true;
				if ( projectFolderBasePath == null ) {
					Tr2dApplication.quit( 1 );
				}
				try {
					projectFolder = new Tr2dProjectFolder( projectFolderBasePath );
					inputStack = projectFolder.getFile( Tr2dProjectFolder.RAW_DATA ).getFile();
					if ( !inputStack.canRead() || !inputStack.exists() ) {
						final String msg = "Invalid project folder -- missing RAW data or read protected!";
						JOptionPane.showMessageDialog( guiFrame, msg, "Argument Error", JOptionPane.ERROR_MESSAGE );
						System.out.println( "ERROR: " + msg );
						Tr2dApplication.quit( 1 );
					}
				} catch ( final IOException e ) {
					System.err.println(
							String.format( "ERROR: Project folder (%s) could not be initialized.", projectFolderBasePath.getAbsolutePath() ) );
					e.printStackTrace();
					Tr2dApplication.quit( 1 );
				}
			} else if ( choice == 1 ) {
				inputStack = UniversalFileChooser.showLoadFileChooser(
						guiFrame,
						"",
						"Load input tiff stack...",
						new ExtensionFileFilter( "tif", "TIFF Image Stack" ) );
			}
			if ( inputStack == null ) {
				Tr2dApplication.quit( 1 );
			}
		}
		if ( projectFolderBasePath == null ) {
			boolean validSelection = false;
			while ( !validSelection ) {
				projectFolderBasePath = UniversalFileChooser.showLoadFolderChooser(
						guiFrame,
						"",
						"Choose tr2d project folder..." );
				if ( projectFolderBasePath == null ) {
					Tr2dApplication.quit( 2 );
				}
				try {
					projectFolder = new Tr2dProjectFolder( projectFolderBasePath );
				} catch ( final IOException e ) {
					System.err.println(
							String.format( "ERROR: Project folder (%s) could not be initialized.", projectFolderBasePath.getAbsolutePath() ) );
					e.printStackTrace();
					Tr2dApplication.quit( 2 );
				}
				if ( projectFolder.getFile( Tr2dProjectFolder.RAW_DATA ).exists() ) {
					final String msg = String.format(
							"Chosen project folder exists (%s)./nShould this project be overwritten?\nAll data in this project will be overwritten...",
							projectFolderBasePath );
					final int overwrite = JOptionPane.showConfirmDialog( guiFrame, msg, "Project Folder Exists", JOptionPane.YES_NO_OPTION );
					if ( overwrite == JOptionPane.YES_OPTION ) {
						validSelection = true;
					}
				} else {
					validSelection = true;
				}
			}
			projectFolder.restartWithRawDataFile( inputStack.getAbsolutePath() );
		}

//		IJ.open( inputStack.getAbsolutePath() );
		final ImagePlus imgPlus = IJ.openImage( inputStack.getAbsolutePath() );
		if ( imgPlus == null ) {
			IJ.error( "There must be an active, open window!" );
			Tr2dApplication.quit( 4 );
		}

		UniversalFileChooser.showOptionPaneWithTitleOnMac = false;
		return imgPlus;
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
				Tr2dApplication.quit( 98 );
			} catch ( final UnsatisfiedLinkError ulr ) {
				final String msgs =
						"Could not initialize Gurobi.\n" + "You might not have installed Gurobi properly or you miss a valid license.\n" + "Please visit 'www.gurobi.com' for further information.\n\n" + ulr
								.getMessage() + "\nJava library path: " + jlp;
				JOptionPane.showMessageDialog(
						guiFrame,
						msgs,
						"Gurobi Error?",
						JOptionPane.ERROR_MESSAGE );
				ulr.printStackTrace();
				System.out.println( "\n>>>>> Java library path: " + jlp + "\n" );
				Tr2dApplication.quit( 99 );
			}
		} catch ( final NoClassDefFoundError err ) {
			final String msgs =
					"Gurobi seems to be not installed on your system.\n" + "Please visit 'www.gurobi.com' for further information.\n\n" + "Java library path: " + jlp;
			JOptionPane.showMessageDialog(
					guiFrame,
					msgs,
					"Gurobi not installed?",
					JOptionPane.ERROR_MESSAGE );
			err.printStackTrace();
			Tr2dApplication.quit( 100 );
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
			Tr2dApplication.quit( 0 );
		}

		if ( cmd.hasOption( "help" ) ) {
			final HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( helpMessageLine1, options );
			Tr2dApplication.quit( 0 );
		}

		File projectFolderBasePath = null;
		if ( cmd.hasOption( "p" ) ) {
			projectFolderBasePath = new File( cmd.getOptionValue( "p" ) );
			if ( !projectFolderBasePath.exists() ) {
				final String msg = "Given project folder does not exist!";
				JOptionPane.showMessageDialog( guiFrame, msg, "Argument Error", JOptionPane.ERROR_MESSAGE );
				System.out.println( "ERROR: " + msg );
				Tr2dApplication.quit( 1 );
			}
			if ( !projectFolderBasePath.isDirectory() ) {
				final String msg = "Given project folder is not a folder!";
				JOptionPane.showMessageDialog( guiFrame, msg, "Argument Error", JOptionPane.ERROR_MESSAGE );
				System.out.println( "ERROR: " + msg );
				Tr2dApplication.quit( 2 );
			}
			if ( !projectFolderBasePath.canWrite() ) {
				final String msg = "Given project folder cannot be written to!";
				JOptionPane.showMessageDialog( guiFrame, msg, "Argument Error", JOptionPane.ERROR_MESSAGE );
				System.out.println( "ERROR: " + msg );
				Tr2dApplication.quit( 3 );
			}
		}

		inputStack = null;
		if ( cmd.hasOption( "i" ) ) {
			inputStack = new File( cmd.getOptionValue( "i" ) );
			if ( !inputStack.isFile() ) {
				final String msg = "Given input tiff stack could not be found!";
				JOptionPane.showMessageDialog( guiFrame, msg, "Argument Error", JOptionPane.ERROR_MESSAGE );
				System.out.println( "ERROR: " + msg );
				Tr2dApplication.quit( 5 );
			}
			if ( !inputStack.canRead() ) {
				final String msg = "Given input tiff stack is not readable!";
				JOptionPane.showMessageDialog( guiFrame, msg, "Argument Error", JOptionPane.ERROR_MESSAGE );
				System.out.println( "ERROR: " + msg );
				Tr2dApplication.quit( 6 );
			}
		} else if ( projectFolderBasePath != null ) { // if a project folder was given load data from there!
			try {
				projectFolder = new Tr2dProjectFolder( projectFolderBasePath );
				inputStack = projectFolder.getFile( Tr2dProjectFolder.RAW_DATA ).getFile();
				if ( !inputStack.canRead() ) {
					final String msg = String.format( "No raw tiff stack found in given project folder (%s)!", projectFolderBasePath );
					JOptionPane.showMessageDialog( guiFrame, msg, "Argument Error", JOptionPane.ERROR_MESSAGE );
					System.out.println( "ERROR: " + msg );
					Tr2dApplication.quit( 7 );
				}
			} catch ( final IOException e ) {
				System.err.println(
						String.format( "ERROR: Project folder (%s) could not used to load data from.", projectFolderBasePath.getAbsolutePath() ) );
				e.printStackTrace();
				Tr2dApplication.quit( 8 );
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

	/**
	 * @return the guiFrame
	 */
	public static JFrame getGuiFrame() {
		return guiFrame;
	}
}
