/**
 *
 */
package com.jug.tr2d.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextPane;

import com.jug.tr2d.Tr2dProperties;
import com.jug.tr2d.Tr2dWekaSegmentationModel;

import view.component.IddeaComponent;
import weka.gui.ExtensionFileFilter;

/**
 * @author jug
 */
public class Tr2dWekaSegmentationPanel extends JPanel implements ActionListener {

	private final Tr2dWekaSegmentationModel modelWekaSeg;

	private JLabel lblClassifier;
	private JTextPane txtClassifierPath;
	private JButton bOpenClassifier;

	private JButton bStartSegmentation;

	private IddeaComponent icSegmentation = null;

	public Tr2dWekaSegmentationPanel( final Tr2dWekaSegmentationModel model ) {
		super( new BorderLayout() );
		modelWekaSeg = model;
		buildGui();
	}

	/**
	 * Builds the GUI of this panel.
	 */
	private void buildGui() {

		lblClassifier = new JLabel( "classifier: " );
		txtClassifierPath = new JTextPane();
		txtClassifierPath.setText( modelWekaSeg.getClassifierPath() );
		bOpenClassifier = new JButton( "pick classifier" );
		bOpenClassifier.addActionListener( this );

		bStartSegmentation = new JButton( "start" );
		bStartSegmentation.addActionListener( this );

		final JPanel helper = new JPanel();
		helper.add( lblClassifier );
		helper.add( txtClassifierPath );
		helper.add( bOpenClassifier );
		helper.add( bStartSegmentation );

		icSegmentation = new IddeaComponent();
		icSegmentation.showMenu( false );
		icSegmentation.setToolBarLocation( BorderLayout.WEST );
		icSegmentation.setToolBarVisible( false );
//		icSegmentation.setPreferredSize( new Dimension( imgPlus.getWidth(), imgPlus.getHeight() ) );
//		icSegmentation.showStackSlider( true );
//		icSegmentation.showTimeSlider( true );

		this.add( helper, BorderLayout.NORTH );
		this.add( icSegmentation, BorderLayout.CENTER );
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed( final ActionEvent e ) {
		if ( e.getSource().equals( bOpenClassifier ) ) {
			actionSelectClassifierFile();
		} else if ( e.getSource().equals( bStartSegmentation ) ) {
			actionStartSegmentaion();
		}
	}

	/**
	 * Opens FileChooser to select a classifier later used for Weka
	 * segmentation.
	 */
	private void actionSelectClassifierFile() {
		final ExtensionFileFilter eff =
				new ExtensionFileFilter( new String[] { "model", "MODEL" }, "weka-model-file" );
		final File file = OsDependentFileChooser.showLoadFileChooser(
				this,
				Tr2dProperties.CLASSIFIER_PATH,
				"Classifier to be loaded...",
				eff );
		if ( file != null ) {
			txtClassifierPath.setText( file.getAbsolutePath() );
		}
	}

	/**
	 * Start segmentation procedure.
	 */
	private void actionStartSegmentaion() {
		try {
			modelWekaSeg.setClassifierPath( txtClassifierPath.getText() );
		} catch ( final IllegalArgumentException iae ) {
			JOptionPane.showMessageDialog(
					this,
					iae.getMessage(),
					"Classifier cannot be used...",
					JOptionPane.ERROR_MESSAGE );
			return;
		}

//		try {
//			modelWekaSeg.setSigma( Double.parseDouble( txtSigma.getText() ) );
//		} catch ( final NumberFormatException nfe ) {
//			JOptionPane.showMessageDialog(
//					this,
//					"Sigma ist not a floating point number.\nSegmentation could not be started!",
//					"Number parse error...",
//					JOptionPane.ERROR_MESSAGE );
//			return;
//		}

		// in case all could be set fine:
		modelWekaSeg.segment();

		try {
			icSegmentation.setSourceImage( modelWekaSeg.getSegmentation() );
		} catch ( final IllegalAccessException e ) {
			JOptionPane.showMessageDialog(
					this,
					e.getMessage(),
					"Segmentation error...",
					JOptionPane.ERROR_MESSAGE );
			e.printStackTrace();
		}
	}

}
