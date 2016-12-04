/**
 *
 */
package com.indago.tr2d.ui.view;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.indago.tr2d.ui.model.Tr2dModel;
import com.indago.tr2d.ui.util.UniversalFileChooser;


/**
 * @author jug
 */
public class Tr2dExportPanel extends JPanel implements ActionListener {

	private static final long serialVersionUID = 5979453520493046392L;

	private final Tr2dModel model;

	private JButton export;

	/**
	 * @param model
	 */
	public Tr2dExportPanel( final Tr2dModel model ) {
		super( new BorderLayout() );
		this.model = model;
		buildGui();
	}

	private void buildGui() {
		export = new JButton( "EXPORT..." );
		export.addActionListener( this );

		this.add( new JLabel( "Export Schitzcells CSV format: " ), BorderLayout.NORTH );
		this.add( export, BorderLayout.CENTER );
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed( final ActionEvent e ) {
		final File projectFolderBasePath = UniversalFileChooser.showLoadFolderChooser(
				model.getMainPanel().getTopLevelAncestor(),
				"",
				"Choose folder for tr2d Schnitzcell export..." );
		if ( projectFolderBasePath.exists() && projectFolderBasePath.isDirectory() ) {
			schnitzcellExport( projectFolderBasePath );
		} else {
			JOptionPane.showMessageDialog( this, "Please choose a valid folder for this export!", "Selection Error", JOptionPane.ERROR_MESSAGE );
		}
	}

	/**
	 * @param projectFolderBasePath
	 */
	private void schnitzcellExport( final File projectFolderBasePath ) {
		final File objects = new File( projectFolderBasePath, "tr2d_objects.csv" );
		final File tracks = new File( projectFolderBasePath, "tr2d_tracks.csv" );

		try {
			final BufferedWriter objWriter = new BufferedWriter( new FileWriter( objects ) );
			objWriter.write( "Hello world!" );
			final BufferedWriter trackWriter = new BufferedWriter( new FileWriter( tracks ) );
			trackWriter.write( "Hello world!" );

			objWriter.close();
			trackWriter.close();
		} catch ( final IOException e ) {
			JOptionPane
					.showMessageDialog( this, "Cannot write in selected export folder... cancel export!", "File Error", JOptionPane.ERROR_MESSAGE );
			e.printStackTrace();
		}
	}

}
