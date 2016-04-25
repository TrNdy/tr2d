/**
 *
 */
package com.indago.tr2d.ui.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import com.indago.app.hernan.Tr2dApplication;
import com.indago.data.segmentation.SegmentationMagic;
import com.indago.io.DataMover;
import com.indago.io.DoubleTypeImgLoader;
import com.indago.io.IntTypeImgLoader;
import com.indago.io.projectfolder.ProjectFolder;
import com.indago.util.converter.IntTypeThresholdConverter;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import ij.IJ;
import io.scif.img.ImgIOException;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.real.DoubleType;

/**
 * @author jug
 */
public class Tr2dWekaSegmentationModel {

	private final String FILENAME_THRESHOLD_VALUES = "thresholdValues.csv";
	private final String FILENAME_CLASSIFIERS = "classifierFilenames.csv";
	private final String FILENAME_PREFIX_SUM_IMGS = "sumImage";
	private final String FILENAME_PREFIX_CLASSIFICATION_IMGS = "classificationImage";

	private final Tr2dSegmentationCollectionModel model;
	private ProjectFolder projectFolder;

//	private List< SilentWekaSegmenter< DoubleType > > classifiers;

	private List< Double > listThresholds = new ArrayList< Double >();
	private List< String > listClassifierFilenames = new ArrayList< String >();

	private final List< RandomAccessibleInterval< DoubleType > > imgsClassification = new ArrayList< >();
	private final List< RandomAccessibleInterval< IntType > > imgsSegmentHypotheses = new ArrayList< >();

	/**
	 * @param parentFolder
	 *
	 */
	public Tr2dWekaSegmentationModel( final Tr2dSegmentationCollectionModel tr2dSegmentationCollectionModel, final ProjectFolder parentFolder ) {
		this.model = tr2dSegmentationCollectionModel;

		try {
			this.projectFolder = parentFolder.addFolder( "weka" );
		} catch ( final IOException e ) {
			this.projectFolder = null;
			System.err.println( "ERROR: Subfolder for weka segmentation hypotheses could not be created." );
			e.printStackTrace();
		}

		final CsvParser parser = new CsvParser( new CsvParserSettings() );

		final File thresholdValues = projectFolder.addFile( FILENAME_THRESHOLD_VALUES, FILENAME_THRESHOLD_VALUES ).getFile();
		try {
			final List< String[] > rows = parser.parseAll( new FileReader( thresholdValues ) );
			for ( final String[] strings : rows ) {
				for ( final String value : strings ) {
					try {
						listThresholds.add( Double.parseDouble( value ) );
					} catch ( final NumberFormatException e ) {
						System.err.println( "Could not parse threshold value: " + value );
					} catch ( final Exception e ) {
					}
				}

			}
		} catch ( final FileNotFoundException e ) {
			listThresholds.add( 0.5 );
			listThresholds.add( 0.7 );
			listThresholds.add( 0.95 );
		}

		final File classifierFilenames = projectFolder.addFile( FILENAME_CLASSIFIERS, FILENAME_CLASSIFIERS ).getFile();
		try {
			final List< String[] > rows = parser.parseAll( new FileReader( classifierFilenames ) );
			for ( final String[] strings : rows ) {
				for ( final String value : strings ) {
					listClassifierFilenames.add( value );
				}

			}
		} catch ( final FileNotFoundException e ) {
		}

		// Try to load classification and sum images corresponding to given classifiers
		int i = 0;
		for ( final String string : listClassifierFilenames ) {
			i++;
			try {
				imgsClassification.add(
						DoubleTypeImgLoader.loadTiff( new File( projectFolder.getFolder(), FILENAME_PREFIX_CLASSIFICATION_IMGS + i + ".tif" ) ) );
				imgsSegmentHypotheses.add(
						IntTypeImgLoader.loadTiff( new File( projectFolder.getFolder(), FILENAME_PREFIX_SUM_IMGS + i + ".tif" ) ) );
			} catch ( final ImgIOException e ) {
				JOptionPane.showMessageDialog(
						Tr2dApplication.getGuiFrame(),
						"Weka Segmentation Results could not be loaded from project folder.\n> " + e.getMessage(),
						"Problem loading from project...",
						JOptionPane.ERROR_MESSAGE );
				e.printStackTrace();
			}
		}
	}

	/**
	 * Loads the given classifier file.
	 *
	 * @param classifierFile
	 */
	private void loadClassifier( final File classifierFile ) {
		SegmentationMagic
				.setClassifier( classifierFile.getParent() + "/", classifierFile.getName() );
//		classifiers.add( SegmentationMagic.getClassifier() );
	}

	/**
	 * @return
	 */
	public List< String > getClassifierFilenames() {
		return listClassifierFilenames;
	}

	/**
	 * @param absolutePath
	 */
	public void setClassifierPaths( final List< String > absolutePaths ) throws IllegalArgumentException {
		this.listClassifierFilenames = absolutePaths;

		try {
			final FileWriter writer = new FileWriter( new File( projectFolder.getFolder(), FILENAME_CLASSIFIERS ) );
			for ( final String string : absolutePaths ) {
				writer.append( string );
				writer.append( "\n" );
			}
			writer.flush();
			writer.close();
		} catch ( final IOException e ) {
			e.printStackTrace();
		}
	}

	/**
	 * Performs the segmentation procedure previously set up.
	 */
	public void segment() {
		imgsClassification.clear();
		int i = 0;
		for ( final String absolutePath : listClassifierFilenames ) {
			i++;
			System.out.println( String.format("Classifier %d of %d -- %s", i, listClassifierFilenames.size(), absolutePath) );

			final File cf = new File( absolutePath );
			if ( !cf.exists() || !cf.canRead() )
				System.err.println( String.format( "Given classifier file cannot be read (%s)", absolutePath ) );
			loadClassifier( cf );

    		// classify frames
			final RandomAccessibleInterval< DoubleType > classification =
					SegmentationMagic.returnClassification( getModel().getModel().getImgOrig() );
			imgsClassification.add( classification );
			IJ.save(
					ImageJFunctions.wrap( classification, "classification image" ).duplicate(),
					new File( projectFolder.getFolder(), FILENAME_PREFIX_CLASSIFICATION_IMGS + i + ".tif" ).getAbsolutePath() );

			// collect thresholds into SumImage
			RandomAccessibleInterval< IntType > imgTemp;
			final Img< IntType > sumimg = DataMover.createEmptyArrayImgLike( classification, new IntType() );
			imgsSegmentHypotheses.add( sumimg );

    		for ( final Double d : listThresholds ) {
    			imgTemp = Converters.convert(
						classification,
    					new IntTypeThresholdConverter( d ),
						new IntType() );
				DataMover.add( imgTemp, ( IterableInterval ) sumimg );
    		}
			IJ.save(
					ImageJFunctions.wrap( sumimg, "sum image" ).duplicate(),
					new File( projectFolder.getFolder(), FILENAME_PREFIX_SUM_IMGS + i + ".tif" ).getAbsolutePath() );
		}
	}

	/**
	 * @return
	 * @throws IllegalAccessException
	 */
	public List< RandomAccessibleInterval< DoubleType > > getClassifications() {
		return imgsClassification;
	}

	/**
	 * @return
	 * @throws IllegalAccessException
	 */
	public List< RandomAccessibleInterval< IntType > > getSegmentHypotheses() {
		return imgsSegmentHypotheses;
	}

	/**
	 * @return the listThresholds
	 */
	public List< Double > getListThresholds() {
		return listThresholds;
	}

	/**
	 *
	 * @param list
	 */
	public void setListThresholds( final List< Double > list ) {
		this.listThresholds = list;
		try {
			final FileWriter writer = new FileWriter( new File( projectFolder.getFolder(), FILENAME_THRESHOLD_VALUES ) );
			for ( final Double value : listThresholds ) {
				writer.append( value.toString() );
				writer.append( ", " );
			}
			writer.flush();
			writer.close();
		} catch ( final IOException e ) {
			e.printStackTrace();
		}
	}

	/**
	 * @return the model
	 */
	public Tr2dSegmentationCollectionModel getModel() {
		return model;
	}
}
