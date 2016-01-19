/**
 *
 */
package com.indago.tr2d.ui.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.indago.app.hernan.costs.HernanAppearanceCostFactory;
import com.indago.app.hernan.costs.HernanDisappearanceCostFactory;
import com.indago.app.hernan.costs.HernanDivisionCostFactory;
import com.indago.app.hernan.costs.HernanMappingCostFactory;
import com.indago.app.hernan.costs.HernanSegmentCostFactory;
import com.indago.app.hernan.models.Tr2dTrackingModelHernan;
import com.indago.tr2d.ui.model.Tr2dModel;
import com.indago.tr2d.ui.model.Tr2dWekaSegmentationModel;
import com.indago.util.converter.RealDoubleNormalizeConverter;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import view.component.IddeaComponent;

/**
 * @author jug
 */
public class Tr2dPanel extends JPanel implements ActionListener, ChangeListener {

	private static Tr2dPanel main;
	private final Tr2dModel model;

	private static JFrame guiFrame;
	private final Frame frame;

	private JTabbedPane tabs;
	private JPanel tabData;
	private JPanel tabSegmentation;
	private JPanel tabTracking;

	private IddeaComponent icData = null;

	/**
	 * @param imgPlus
	 */
	@SuppressWarnings( { "rawtypes", "unchecked" } )
	public Tr2dPanel( final Frame frame, final Tr2dModel model ) {
		super( new BorderLayout( 5, 5 ) );

		setBorder( BorderFactory.createEmptyBorder( 10, 15, 5, 15 ) );
		this.frame = frame;
		this.model = model;

		final Img< ? extends RealType > temp = ImagePlusAdapter.wrapNumeric( model.getImgPlus() );
		model.setImgOrig( Converters.convert(
				( RandomAccessibleInterval ) temp,
				new RealDoubleNormalizeConverter( 1.0 ),
				new DoubleType() ) );
		model.setImgOrigNorm( Converters.convert(
				( RandomAccessibleInterval ) temp,
				new RealDoubleNormalizeConverter( model.getImgPlus().getStatistics().max ),
				new DoubleType() ) );

		buildGui();

		this.frame.setSize( 1200, 758 );
	}

	private void buildGui() {

		tabs = new JTabbedPane();
		tabData = new JPanel( new BorderLayout() );
		final Tr2dWekaSegmentationModel segModel = new Tr2dWekaSegmentationModel( model );
		tabSegmentation = new Tr2dWekaSegmentationPanel( segModel );
		final RandomAccessibleInterval< DoubleType > imgOrig = model.getImgOrig();
		tabTracking =
				new Tr2dTrackingPanel(
						new Tr2dTrackingModelHernan( model, segModel,
								new HernanSegmentCostFactory( imgOrig ),
								new HernanAppearanceCostFactory( imgOrig ),
								new HernanMappingCostFactory( imgOrig ),
								new HernanDivisionCostFactory( imgOrig ),
 new HernanDisappearanceCostFactory( imgOrig ) ) );

		icData = new IddeaComponent( model.getImgOrigNorm() );
		icData.showMenu( false );
		icData.setToolBarLocation( BorderLayout.WEST );
		icData.setToolBarVisible( false );
		icData.setPreferredSize(
				new Dimension( model.getImgPlus().getWidth(), model.getImgPlus().getHeight() ) );
//		icData.showStackSlider( true );
//		icData.showTimeSlider( true );
		tabData.add( icData, BorderLayout.CENTER );

		tabs.add( "Dataset", tabData );
		tabs.add( "Segmentation", tabSegmentation );
		tabs.add( "Tracking", tabTracking );

		this.add( tabs, BorderLayout.CENTER );

		// - - - - - - - - - - - - - - - - - - - - - - - -
		// KEYSTROKE SETUP (usingInput- and ActionMaps)
		// - - - - - - - - - - - - - - - - - - - - - - - -
		this.getInputMap( WHEN_IN_FOCUSED_WINDOW ).put(
				KeyStroke.getKeyStroke( '.' ),
				"tr2d_bindings" );
		this.getInputMap( WHEN_IN_FOCUSED_WINDOW ).put(
				KeyStroke.getKeyStroke( ',' ),
				"tr2d_bindings" );

		this.getActionMap().put( "tr2d_bindings", new AbstractAction() {

			private static final long serialVersionUID = 2L;

			@Override
			public void actionPerformed( final ActionEvent e ) {
				if ( e.getActionCommand().equals( "," ) ) {
				}
				if ( e.getActionCommand().equals( "." ) ) {
				}
			}
		} );
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed( final ActionEvent e ) {
		// TODO Auto-generated method stub
	}

	/**
	 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
	 */
	@Override
	public void stateChanged( final ChangeEvent e ) {
		// TODO Auto-generated method stub
	}

	public static void main( final String[] args ) {
		ImageJ temp = IJ.getInstance();

		if ( temp == null ) {
			temp = new ImageJ();

			if ( args.length > 0 ) {
				IJ.open( args[ 0 ] );
			} else {
				throw new IllegalArgumentException( "Please, set an image path in the program arguments." );
//				IJ.open( "/Users/jug/Desktop/demo.tif" );
			}
		}

		final ImagePlus imgPlus = WindowManager.getCurrentImage();
		if ( imgPlus == null ) {
			IJ.error( "There must be an active, open window!" );
			// System.exit( 1 );
			return;
		}

		guiFrame = new JFrame( "tr2d" );
		final Tr2dModel model = new Tr2dModel( imgPlus );
		main = new Tr2dPanel( guiFrame, model );

		guiFrame.add( main );
		guiFrame.setVisible( true );
	}
}
