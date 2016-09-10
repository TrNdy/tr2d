/**
 *
 */
package com.indago.tr2d.ui.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
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

import com.indago.io.ProjectFile;
import com.indago.tr2d.Tr2dLog;
import com.indago.tr2d.ui.model.Tr2dImportedSegmentationModel;
import com.indago.tr2d.ui.util.UniversalFileChooser;

import bdv.util.Bdv;
import bdv.util.BdvHandlePanel;
import io.scif.img.ImgIOException;
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

	public Tr2dImportedSegmentationPanel( final Tr2dImportedSegmentationModel model ) {
		super( new BorderLayout() );
		this.model = model;
		listSegmentations = new JList< ProjectFile >( model.getListModel() );
		buildGui();

		if ( model.getSegmentHypothesesImages().size() > 0 ) {
			listSegmentations.setSelectedIndex( 0 );
		}
	}

	private void buildGui() {
		final JPanel viewer = new JPanel( new BorderLayout() );
		model.bdvSetHandlePanel(
				new BdvHandlePanel( ( Frame ) this.getTopLevelAncestor(), Bdv
						.options()
						.is2D()
						.inputTriggerConfig( model.getModel().getModel().getDefaultInputTriggerConfig() ) ) );
		viewer.add( model.bdvGetHandlePanel().getViewerPanel(), BorderLayout.CENTER );

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
				Tr2dLog.log.info( "File selection canceled." );
			}
		} else if ( e.getSource().equals( remove ) ) {
			model.removeSegmentations( listSegmentations.getSelectedIndices() );
			listSegmentations.clearSelection();
		}
	}

	/**
	 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
	 */
	@Override
	public void valueChanged( final ListSelectionEvent e ) {
		if ( e.getValueIsAdjusting() == false ) {

			model.bdvRemoveAll();
			if ( listSegmentations.getSelectedIndices().length > 0 ) {
				for ( final int idx : listSegmentations.getSelectedIndices() ) {
					model.bdvAdd(
							model.getSegmentHypothesesImages().get( idx ),
							listSegmentations.getModel().getElementAt( idx ).getFile().getName() );
				}
			} else {
				model.bdvRemoveAll();
			}
		}
	}
}
