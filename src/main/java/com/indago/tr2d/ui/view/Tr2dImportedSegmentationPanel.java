/**
 *
 */
package com.indago.tr2d.ui.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.indago.iddea.view.component.IddeaComponent;
import com.indago.io.projectfolder.ProjectFile;
import com.indago.tr2d.ui.model.Tr2dImportedSegmentationModel;

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
		listSegmentations = new JList< ProjectFile >( model.getLoadedFiles() );
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
	public void actionPerformed( final ActionEvent e ) {}

	/**
	 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
	 */
	@Override
	public void valueChanged( final ListSelectionEvent e ) {
		if ( e.getValueIsAdjusting() == false ) {

			final int idx = listSegmentations.getSelectedIndex();
			if ( idx > -1 ) {
				icSegmentation.setSourceImage( model.getSegmentHypothesesImages().get( idx ) );
			}
		}
	}

}
