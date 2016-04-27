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

import com.indago.io.projectfolder.ProjectFile;
import com.indago.tr2d.ui.model.Tr2dImportedSegmentationModel;
import com.indago.tr2d.ui.util.UniversalFileChooser;
import com.indago.util.ImglibUtil;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandlePanel;
import bdv.util.BdvSource;
import io.scif.img.ImgIOException;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
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

	private BdvHandlePanel bdv;

	public Tr2dImportedSegmentationPanel( final Tr2dImportedSegmentationModel model ) {
		super( new BorderLayout() );
		this.model = model;
		listSegmentations = new JList< ProjectFile >( model.getListModel() );
		buildGui();

		if ( model.getSegmentHypothesesImages().size() > 0 ) {
			updateBdvView( model.getSegmentHypothesesImages().get( 0 ) );
		}
	}

	private void buildGui() {
		final JPanel viewer = new JPanel( new BorderLayout() );
		bdv = new BdvHandlePanel( ( Frame ) this.getTopLevelAncestor(), Bdv.options().is2D() );
		viewer.add( bdv.getViewerPanel(), BorderLayout.CENTER );

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
			updateBdvView( model.getSegmentHypothesesImages().get( 0 ) );
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
				updateBdvView( model.getSegmentHypothesesImages().get( idx ) );
			} else {
				listSegmentations.setSelectedIndex( 0 );
			}
		}
	}

	/**
	 * @param img
	 */
	private < T extends RealType< T > & NativeType< T > > void updateBdvView( final RandomAccessibleInterval< T > img ) {
		final Runnable task = new Runnable() {

			@Override
			public void run() {
				final BdvSource source = BdvFunctions.show(
						img,
						"segmentation",
						Bdv.options().addTo( bdv ) );
//				source.removeFromBdv();
				final T min = img.randomAccess().get().copy();
				final T max = min.copy();
				ImglibUtil.computeMinMax( Views.iterable( img ), min, max );
				source.setDisplayRangeBounds( 0, max.getRealDouble() );
				source.setDisplayRange( min.getRealDouble(), max.getRealDouble() );
			}
		};
		new Thread( task ).start();
	}

}
