/**
 *
 */
package com.indago.tr2d.ui.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.indago.iddea.view.component.IddeaComponent;
import com.indago.io.projectfolder.ProjectFile;
import com.indago.tr2d.ui.model.Tr2dImportedSegmentationModel;
import com.indago.tr2d.ui.util.UniversalFileChooser;

import io.scif.img.ImgIOException;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.IntType;
import weka.gui.ExtensionFileFilter;

/**
 * @author jug
 */
public class Tr2dImportedSegmentationPanel extends JPanel implements ActionListener, ListSelectionListener {

	private static final long serialVersionUID = -4610859107829248753L;

	private final Tr2dImportedSegmentationModel model;

	private final JList< ProjectFile > listSegmentations;
	private final JButton add = new JButton( "+" );
	private final JButton remove = new JButton( "-" );
	private IddeaComponent icSegmentation = null;

	public Tr2dImportedSegmentationPanel( final Tr2dImportedSegmentationModel model ) {
		super( new BorderLayout() );
		this.model = model;
		listSegmentations = new JList< ProjectFile >( model.getListModel() );
		buildGui();
	}

	private void buildGui() {
		final JPanel viewer = new JPanel( new BorderLayout() );
		icSegmentation = new IddeaComponent(
				new Dimension(
						(int) model.getModel().getModel().getImgOrig().dimension( 0 ),
						(int) model.getModel().getModel().getImgOrig().dimension( 1 ) ) );
		icSegmentation.showMenu( false );
		icSegmentation.setToolBarLocation( BorderLayout.WEST );
		icSegmentation.setToolBarVisible( false );
//		icSegmentation.installDefaultToolBar();
//		icSegmentation.setPreferredSize( new Dimension( imgPlus.getWidth(), imgPlus.getHeight() ) );
//		icSegmentation.showStackSlider( true );
//		icSegmentation.showTimeSlider( true );
		viewer.add( icSegmentation, BorderLayout.CENTER );

		final JPanel list = new JPanel( new BorderLayout() );
		listSegmentations.addListSelectionListener( this );
		list.add( listSegmentations, BorderLayout.CENTER );
		final JPanel helper = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
		add.addActionListener( this );
		remove.addActionListener( this );
		helper.add( add );
		helper.add( remove );
		list.add( helper, BorderLayout.SOUTH );

		final JSplitPane splitPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, list, viewer );
		splitPane.setResizeWeight( 0.1 ); // 1.0 == extra space given to left component alone!
		this.add( splitPane, BorderLayout.CENTER );

	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed( final ActionEvent e ) {
		if ( e.getSource().equals( add ) ) {
			final File file = UniversalFileChooser.showLoadFileChooser(
					this,
					null,
					"Choose a sum image tiff file to import...",
					new ExtensionFileFilter( "tif", "TIFF Image Stack" ) );
			try {
				model.importSegmentation( file );
				listSegmentations.setSelectedIndex( listSegmentations.getModel().getSize() - 1 );
			} catch ( ImgIOException | IOException e1 ) {
				e1.printStackTrace();
			}
		} else if ( e.getSource().equals( remove ) ) {
			model.removeSegmentations( listSegmentations.getSelectedIndices() );
			listSegmentations.setSelectedIndex( 0 );
			updateViewer( 0 );
		}
	}

	/**
	 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
	 */
	@Override
	public void valueChanged( final ListSelectionEvent e ) {
		if ( e.getValueIsAdjusting() == false ) {

			final int idx = listSegmentations.getSelectedIndex();
			if ( idx > -1 ) {
				updateViewer( idx );
			} else {
				listSegmentations.setSelectedIndex( 0 );
			}
		}
	}

	/**
	 * Updates viewer to show the image associated with the loaded segmentation
	 * at list index <code>idx</code>.
	 *
	 * @param idx
	 */
	private void updateViewer( final int idx ) {
		final RandomAccessibleInterval< IntType > img = model.getSegmentHypothesesImages().get( idx );
		icSegmentation.setSourceImage( img );
	}

}
