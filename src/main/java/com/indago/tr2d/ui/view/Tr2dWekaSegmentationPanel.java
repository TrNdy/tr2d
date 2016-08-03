/**
 *
 */
package com.indago.tr2d.ui.view;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextPane;

import com.indago.tr2d.ui.model.Tr2dWekaSegmentationModel;
import com.indago.tr2d.ui.util.JDoubleListTextPane;
import com.indago.tr2d.ui.util.UniversalFileChooser;

import bdv.util.Bdv;
import bdv.util.BdvHandlePanel;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.IntType;
import weka.gui.ExtensionFileFilter;

/**
 * @author jug
 */
public class Tr2dWekaSegmentationPanel extends JPanel implements ActionListener {

	private static final long serialVersionUID = 9192569315077150275L;

	private final Tr2dWekaSegmentationModel model;

	private JLabel lblClassifier;
	private JTextPane txtClassifierPath;
	private JButton bOpenClassifier;

	private JLabel lblThresholds;
	private JDoubleListTextPane txtThresholds;

	private JButton bStartSegmentation;

	public Tr2dWekaSegmentationPanel( final Tr2dWekaSegmentationModel model ) {
		super( new BorderLayout() );
		this.model = model;
		buildGui();

		for ( final RandomAccessibleInterval< IntType > seghyp : model.getSegmentHypotheses() ) {
			model.bdvAdd( seghyp, "result" );
		}
	}

	/**
	 * Builds the GUI of this panel.
	 */
	private void buildGui() {

		lblClassifier = new JLabel( "classifier: " );
		txtClassifierPath = new JTextPane();
		String fn = "";
		try {
			fn = model.getClassifierFilenames().get( 0 );
		} catch ( final IndexOutOfBoundsException e ) {}
		txtClassifierPath.setText( fn );
		bOpenClassifier = new JButton( "pick classifier" );
		bOpenClassifier.addActionListener( this );

		lblThresholds = new JLabel( "thresholds: " );
		txtThresholds = new JDoubleListTextPane( model.getListThresholds() );

		bStartSegmentation = new JButton( "start" );
		bStartSegmentation.addActionListener( this );

		// Add stuff together
		// - - - - - - - - - -
		final JPanel helper = new JPanel();

		helper.add( lblClassifier );
		helper.add( txtClassifierPath );
		helper.add( bOpenClassifier );

		helper.add( lblThresholds );
		helper.add( txtThresholds );

		helper.add( bStartSegmentation );

		final BdvHandlePanel bdv = new BdvHandlePanel( ( Frame ) this.getTopLevelAncestor(), Bdv
				.options()
				.is2D()
				.inputTriggerConfig( model.getModel().getModel().getDefaultInputTriggerConfig() ) );
		model.bdvSetHandlePanel( bdv );

		this.add( helper, BorderLayout.NORTH );
		this.add( model.bdvGetHandlePanel().getViewerPanel(), BorderLayout.CENTER );
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
		final File file = UniversalFileChooser.showLoadFileChooser(
				this,
				"",
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
			final ArrayList< String > paths = new ArrayList< >();
			paths.add( txtClassifierPath.getText() );
			model.setClassifierPaths( paths );
		} catch ( final IllegalArgumentException iae ) {
			JOptionPane.showMessageDialog(
					this,
					iae.getMessage(),
					"Classifier cannot be used...",
					JOptionPane.ERROR_MESSAGE );
			return;
		}

		try {
			model.setListThresholds( txtThresholds.getList() );
		} catch ( final NumberFormatException nfe ) {
			JOptionPane.showMessageDialog(
					this,
					"List of doubles cannot be parsed!",
					"Number parse error...",
					JOptionPane.ERROR_MESSAGE );
			return;
		}

		// in case all could be set fine:
		model.segment();

		final RandomAccessibleInterval< IntType > seghyps = model.getSegmentHypotheses().get( 0 );
		if ( seghyps == null ) {
			JOptionPane.showMessageDialog(
					this,
					"Raw image data could not be segmented.",
					"Segmentation error...",
					JOptionPane.ERROR_MESSAGE );
		} else {
			model.bdvAdd( seghyps, "result" );
		}
	}
}
