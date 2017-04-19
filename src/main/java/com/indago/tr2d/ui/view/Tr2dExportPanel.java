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
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import com.indago.data.Ellipse2D;
import com.indago.data.PixelCloud2D;
import com.indago.data.segmentation.LabelingSegment;
import com.indago.pg.assignments.AppearanceHypothesis;
import com.indago.pg.assignments.DisappearanceHypothesis;
import com.indago.pg.assignments.DivisionHypothesis;
import com.indago.pg.assignments.MovementHypothesis;
import com.indago.pg.segments.SegmentNode;
import com.indago.tr2d.pg.Tr2dSegmentationProblem;
import com.indago.tr2d.ui.model.Tr2dModel;
import com.indago.tr2d.ui.util.SolutionExporter;
import com.indago.tr2d.ui.util.SolutionExporter.Tracklet;
import com.indago.tr2d.ui.util.UniversalFileChooser;
import com.indago.util.Bimap;

import net.imglib2.Cursor;
import net.miginfocom.swing.MigLayout;


/**
 * @author jug
 */
public class Tr2dExportPanel extends JPanel implements ActionListener {

	private static final long serialVersionUID = 5979453520493046392L;

	private final Tr2dModel model;

	private JButton exportSchnitzcells;

	private JButton exportTrackingProblem;

	private JButton exportTrackingProblemILP;

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

		final JPanel panelSchitzcells = new JPanel( new MigLayout() );
		exportSchnitzcells = new JButton( "export to folder..." );
		exportSchnitzcells.addActionListener( this );
		panelSchitzcells.add( exportSchnitzcells, "growx, wrap" );
		panelSchitzcells.setBorder( BorderFactory.createTitledBorder( "Schitzcells CSV" ) );

		final JPanel panelTrackingProblem = new JPanel( new MigLayout() );
		exportTrackingProblem = new JButton( "export tracking problem..." );
		exportTrackingProblem.addActionListener( this );
		exportTrackingProblemILP = new JButton( "export ILP..." );
		exportTrackingProblemILP.addActionListener( this );
		panelTrackingProblem.add( exportTrackingProblem, "growx, wrap" );
		panelTrackingProblem.add( exportTrackingProblemILP, "growx, wrap" );
		panelTrackingProblem.setBorder( BorderFactory.createTitledBorder( "Tracking Problem" ) );

		controls.add( panelSchitzcells, "growx, wrap" );
		controls.add( panelTrackingProblem, "growx, wrap" );

		final JSplitPane splitPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, controls, viewer );
		add( splitPane, BorderLayout.CENTER );
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed( final ActionEvent e ) {
		if ( e.getSource().equals( exportSchnitzcells ) ) {
			final File projectFolderBasePath = UniversalFileChooser.showLoadFolderChooser(
					model.getMainPanel().getTopLevelAncestor(),
					"",
					"Choose folder for tr2d Schnitzcell export..." );
			if ( projectFolderBasePath.exists() && projectFolderBasePath.isDirectory() ) {
				schnitzcellExport( projectFolderBasePath );
			} else {
				JOptionPane.showMessageDialog( this, "Please choose a valid folder for this export!", "Selection Error", JOptionPane.ERROR_MESSAGE );
			}
		} else if ( e.getSource().equals( exportTrackingProblem ) ) {
			final File projectFolderBasePath = UniversalFileChooser.showLoadFolderChooser(
					model.getMainPanel().getTopLevelAncestor(),
					"",
					"Choose folder for tr2d tracking problem export..." );
			if ( projectFolderBasePath.exists() && projectFolderBasePath.isDirectory() ) {
				trackingProblemExport( projectFolderBasePath );
			} else {
				JOptionPane.showMessageDialog( this, "Please choose a valid folder for this export!", "Selection Error", JOptionPane.ERROR_MESSAGE );
			}
		} else if ( e.getSource().equals( exportTrackingProblemILP ) ) {
			final File projectFolderBasePath = UniversalFileChooser.showLoadFolderChooser(
					model.getMainPanel().getTopLevelAncestor(),
					"",
					"Choose folder for ILP tracking problem export..." );
			if ( projectFolderBasePath.exists() && projectFolderBasePath.isDirectory() ) {
				trackingProblemILPExport( projectFolderBasePath );
			} else {
				JOptionPane.showMessageDialog( this, "Please choose a valid folder for this export!", "Selection Error", JOptionPane.ERROR_MESSAGE );
			}
		}
	}

	/**
	 * @param projectFolderBasePath
	 */
	private void trackingProblemExport( final File projectFolderBasePath ) {
		final File exportFile = new File( projectFolderBasePath, "tr2d_problem.jug" );

		final Map< SegmentNode, Integer > mapSeg2Id = new HashMap<>();
		int next_segment_id = -1;

		try {
			final SimpleDateFormat sdfDate = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );//dd/MM/yyyy
			final Date now = new Date();
			final String strNow = sdfDate.format( now );

			final SolutionExporter exp = new SolutionExporter( model.getTrackingModel(), model.getTrackingModel().getSolution() );

			final BufferedWriter problemWriter = new BufferedWriter( new FileWriter( exportFile ) );
			problemWriter.write( "# Tr2d problem export from " + strNow + "\n" );

			final List< Tr2dSegmentationProblem > timePoints = model.getTrackingModel().getTrackingProblem().getTimepoints();

			problemWriter.write( "\n# === SEGMENT HYPOTHESES =================================================\n\n" );
			for ( final Tr2dSegmentationProblem t : timePoints ) {
				problemWriter.write( String.format( "\nt=%d\n", t.getTime() ) );

				// write all segment hypotheses
				final Collection< SegmentNode > segments = t.getSegments();
				for ( final SegmentNode segment : segments ) {
					mapSeg2Id.put( segment, ++next_segment_id );
					writeSegmentLine( segment, next_segment_id, problemWriter );
				}
			}

			problemWriter.write( "# === ASSIGNMNETS ========================================================\n" );
			for ( final Tr2dSegmentationProblem t : timePoints ) {
				final Collection< SegmentNode > segments = t.getSegments();
				// for each segmetn hyp, write all assignments
				for ( final SegmentNode segment : segments ) {

					final Collection< AppearanceHypothesis > apps = segment.getInAssignments().getAppearances();
					for ( final AppearanceHypothesis app : apps ) {
						writeAppearanceLine( app, mapSeg2Id, problemWriter );
					}
					final Collection< DisappearanceHypothesis > disapps = segment.getOutAssignments().getDisappearances();
					for ( final DisappearanceHypothesis disapp : disapps ) {
						writeDisappearanceLine( disapp, mapSeg2Id, problemWriter );
					}
					final Collection< MovementHypothesis > moves = segment.getOutAssignments().getMoves();
					for ( final MovementHypothesis move : moves ) {
						writeMovementLine( move, mapSeg2Id, problemWriter );
					}
					final Collection< DivisionHypothesis > divs = segment.getOutAssignments().getDivisions();
					for ( final DivisionHypothesis div : divs ) {
						writeDivisionLine( div, mapSeg2Id, problemWriter );
					}
					problemWriter.write( "\n" );
				}
			}

			problemWriter.write( "# === CONSTRAINTS ========================================================\n\n" );

			problemWriter.close();

		} catch ( final IOException e ) {
			JOptionPane
					.showMessageDialog( this, "Cannot write in selected export folder... cancel export!", "File Error", JOptionPane.ERROR_MESSAGE );
			e.printStackTrace();
		}
	}

	/**
	 * @param segment
	 * @param id
	 * @param writer
	 * @throws IOException
	 */
	private void writeSegmentLine( final SegmentNode segment, final int id, final BufferedWriter writer ) throws IOException {
		// H <id> <cost> <com_x_pos> <com_y_pos>
		writer.write(
				String.format(
						"H %d %.16f (%.1f,%.1f)\n",
						id,
						segment.getCost(),
						segment.getSegment().getCenterOfMass().getFloatPosition( 0 ),
						segment.getSegment().getCenterOfMass().getFloatPosition( 1 ) ) );
	}

	/**
	 * @param app
	 * @param mapSeg2Id
	 * @param writer
	 * @throws IOException
	 */
	private void writeAppearanceLine( final AppearanceHypothesis app, final Map< SegmentNode, Integer > mapSeg2Id, final BufferedWriter writer )
			throws IOException {
		// APP <segment_id> <cost>
		writer.write(
				String.format(
						"APP %d %.16f\n",
						mapSeg2Id.get( app.getDest() ),
						app.getCost() ) );
	}

	/**
	 * @param disapp
	 * @param mapSeg2Id
	 * @param writer
	 * @throws IOException
	 */
	private void writeDisappearanceLine(
			final DisappearanceHypothesis disapp,
			final Map< SegmentNode, Integer > mapSeg2Id,
			final BufferedWriter writer )
			throws IOException {
		// DISAPP <segment_id> <cost>
		writer.write(
				String.format(
						"DISAPP %d %.16f\n",
						mapSeg2Id.get( disapp.getSrc() ),
						disapp.getCost() ) );
	}

	/**
	 * @param move
	 * @param mapSeg2Id
	 * @param writer
	 * @throws IOException
	 */
	private void writeMovementLine(
			final MovementHypothesis move,
			final Map< SegmentNode, Integer > mapSeg2Id,
			final BufferedWriter writer )
			throws IOException {
		// MOVE <source_segment_id> <dest_segment_id> <cost>
		writer.write(
				String.format(
						"MOVE %d %d %.16f\n",
						mapSeg2Id.get( move.getSrc() ),
						mapSeg2Id.get( move.getDest() ),
						move.getCost() ) );
	}

	/**
	 * @param div
	 * @param mapSeg2Id
	 * @param writer
	 * @throws IOException
	 */
	private void writeDivisionLine(
			final DivisionHypothesis div,
			final Map< SegmentNode, Integer > mapSeg2Id,
			final BufferedWriter writer )
			throws IOException {
		// MOVE <source_segment_id> <dest1_segment_id> <dest2_segment_id> <cost>
		writer.write(
				String.format(
						"DIV %d %d %d %.16f\n",
						mapSeg2Id.get( div.getSrc() ),
						mapSeg2Id.get( div.getDest1() ),
						mapSeg2Id.get( div.getDest2() ),
						div.getCost() ) );
	}

	/**
	 * @param projectFolderBasePath
	 */
	private void trackingProblemILPExport( final File projectFolderBasePath ) {
		// TODO Auto-generated method stub

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
			objWriter.write( "# t, id, area, com_x, com_y, angle, r1, r2\n" );
			final Map< Integer, Bimap< Integer, SegmentNode > > mapTime2Segments = exp.getTime2SegmentsMap();
			for ( int t = 0; t < mapTime2Segments.size(); t++ ) {
				final Bimap< Integer, SegmentNode > bimap = mapTime2Segments.get( t );
				for ( int objId = 0; objId < bimap.size(); objId++ ) {
					final SegmentNode segNode = bimap.getB( objId );
					final LabelingSegment segment = segNode.getSegment();

					// get fitted ellipse
					final PixelCloud2D< Integer > cloud = new PixelCloud2D<>();
					final Cursor< Void > cursor = segment.getRegion().cursor();
					while ( cursor.hasNext() ) {
						cursor.fwd();
						cloud.addPoint( cursor.getIntPosition( 0 ), cursor.getIntPosition( 1 ), 1 );
					}
					final Ellipse2D ellipse = cloud.getEllipticalApproximation();

					objWriter.write(
							String.format(
									"%3d,%3d,%3d,%8.4f,%8.4f,%8.4f,%8.4f,%8.4f\n",
									t,
									objId,
									segment.getArea(),
									ellipse.getCenter().getX(),
									ellipse.getCenter().getY(),
									ellipse.getAngle(),
									ellipse.getA(),
									ellipse.getB() ) );
				}
			}
			objWriter.close();

			final BufferedWriter trackWriter = new BufferedWriter( new FileWriter( tracks ) );
			trackWriter.write( "# Tr2d export from " + strNow + "\n" );
			trackWriter.write( "# tracklet_id, parent_tracklet_id, child_tracklat_id1, child_tracklat_id2, (time, object_id)...\n" );
			final List< Tracklet > tracklets = exp.getTracklets();
			for ( final Tracklet tracklet : tracklets ) {
				trackWriter.write( String.format(
						"%3d,%3d,%3d,%3d",
						tracklet.getTrackletId(),
						tracklet.getParentId(),
						tracklet.getChild1(),
						tracklet.getChild2() ) );
				final List< Integer > oids = tracklet.getObjectIds();
				int time = tracklet.getStartTime();
				for ( final int oid : oids ) {
					trackWriter.write( String.format( " ,%3d,%3d", time++, oid ) );
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
