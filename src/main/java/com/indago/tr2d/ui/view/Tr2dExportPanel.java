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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import com.indago.data.segmentation.LabelingSegment;
import com.indago.pg.segments.SegmentNode;
import com.indago.tr2d.ui.model.Tr2dModel;
import com.indago.tr2d.ui.util.SolutionExporter;
import com.indago.tr2d.ui.util.SolutionExporter.Tracklet;
import com.indago.tr2d.ui.util.UniversalFileChooser;
import com.indago.util.Bimap;

import net.miginfocom.swing.MigLayout;


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
		final JPanel controls = new JPanel( new MigLayout() );
		final JPanel viewer = new JPanel( new BorderLayout() );

		final JPanel exportSchitzcells = new JPanel( new MigLayout() );
		export = new JButton( "export to folder" );
		export.addActionListener( this );
		exportSchitzcells.add( export, "growx, wrap" );
		exportSchitzcells.setBorder( BorderFactory.createTitledBorder( "Schitzcells CSV" ) );

		controls.add( exportSchitzcells, "growx, wrap" );

		final JSplitPane splitPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, controls, viewer );
		add( splitPane, BorderLayout.CENTER );
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
			final SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//dd/MM/yyyy
		    final Date now = new Date();
			final String strNow = sdfDate.format( now );

			final SolutionExporter exp = new SolutionExporter( model.getTrackingModel(), model.getTrackingModel().getSolution() );

			final BufferedWriter objWriter = new BufferedWriter( new FileWriter( objects ) );
			objWriter.write( "# Tr2d export from " + strNow + "\n" );
			objWriter.write( "# t, id, x, y, area\n" );
			final Map< Integer, Bimap< Integer, SegmentNode > > mapTime2Segments = exp.getTime2SegmentsMap();
			for ( int t = 0; t < mapTime2Segments.size(); t++ ) {
				final Bimap< Integer, SegmentNode > bimap = mapTime2Segments.get( t );
				for ( int objId = 0; objId < bimap.size(); objId++ ) {
					final SegmentNode segNode = bimap.getB( objId );
					final LabelingSegment segment = segNode.getSegment();
					objWriter.write(
							String.format(
									"%3d,%3d,%8.4f,%8.4f,%3d\n",
									t,
									objId,
									segment.getCenterOfMass().getDoublePosition( 0 ),
									segment.getCenterOfMass().getDoublePosition( 1 ),
									segment.getArea() ) );
				}
			}
			objWriter.close();

			final BufferedWriter trackWriter = new BufferedWriter( new FileWriter( tracks ) );
			trackWriter.write( "# Tr2d export from " + strNow + "\n" );
			trackWriter.write( "# tracklet_id, parent_tracklet_id, child_tracklat_id1, child_tracklat_id2, time_start, [object_ids, ...]\n" );
			final List< Tracklet > tracklets = exp.getTracklets();
			for ( final Tracklet tracklet : tracklets ) {
				trackWriter.write( String.format(
						"%3d,%3d,%3d,%3d,%3d ",
						tracklet.getTrackletId(),
						tracklet.getParentId(),
						tracklet.getChild1(),
						tracklet.getChild2(),
						tracklet.getStartTime() ) );
				final List< Integer > oids = tracklet.getObjectIds();
				for ( final int oid : oids ) {
					trackWriter.write( String.format( ",%3d", oid ) );
				}
				trackWriter.write( "\n" );
			}
			trackWriter.close();
		} catch ( final IOException e ) {
			JOptionPane
					.showMessageDialog( this, "Cannot write in selected export folder... cancel export!", "File Error", JOptionPane.ERROR_MESSAGE );
			e.printStackTrace();
		}
	}

}
