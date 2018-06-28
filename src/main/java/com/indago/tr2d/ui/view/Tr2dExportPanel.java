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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
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
import com.indago.fg.Assignment;
import com.indago.fg.MappedFactorGraph;
import com.indago.fg.Variable;
import com.indago.ilp.SolveGurobi.GurobiResult;
import com.indago.pg.IndicatorNode;
import com.indago.pg.assignments.AppearanceHypothesis;
import com.indago.pg.assignments.AssignmentNode;
import com.indago.pg.assignments.DisappearanceHypothesis;
import com.indago.pg.assignments.DivisionHypothesis;
import com.indago.pg.assignments.MovementHypothesis;
import com.indago.pg.segments.ConflictSet;
import com.indago.pg.segments.SegmentNode;
import com.indago.tr2d.pg.Tr2dSegmentationProblem;
import com.indago.tr2d.ui.model.Tr2dModel;
import com.indago.tr2d.ui.util.SolutionExporter;
import com.indago.tr2d.ui.util.SolutionExporter.Tracklet;
import com.indago.tr2d.ui.util.UniversalFileChooser;
import com.indago.util.Bimap;

import gnu.trove.map.TObjectIntMap;
import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBVar;
import net.imglib2.Cursor;
import net.imglib2.util.ValuePair;
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
		final boolean export_continuation_constraints = false; // currently not desired (agreement with Paul)

		final File exportFile = new File( projectFolderBasePath, "tr2d_problem.jug" );

		final Map< SegmentNode, ValuePair< Integer, Integer > > mapSeg2Id = new HashMap<>();
		int next_segment_id = -1;
		final Map< AssignmentNode, ValuePair< Integer, Integer > > mapAss2Id = new HashMap<>();
		int next_assignment_id = -1;

		try {
			final SimpleDateFormat sdfDate = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );//dd/MM/yyyy
			final Date now = new Date();
			final String strNow = sdfDate.format( now );

			final BufferedWriter problemWriter = new BufferedWriter( new FileWriter( exportFile ) );
			problemWriter.write( "# Tr2d problem export from " + strNow + "\n" );
			problemWriter.write( String.format( "# objective_value = %.12f\n", model.getTrackingModel().getSolver().getLatestEnergy() ) );

			final MappedFactorGraph mfg = model.getTrackingModel().getMappedFactorGraph();
			final Bimap< IndicatorNode, Variable > varmap = mfg.getVarmap();

			// Prepare the ability to modify the Gurobi variable names
			// This is useful in order to debug exported FGs and the corresponding .lp file.
			boolean modifyGurobiVarNames = true;
			final Assignment< Variable > fgSolution = model.getTrackingModel().getFgSolution();
			GurobiResult gurobiResults = null;
			GRBVar[] grbVars = null;
			TObjectIntMap< Variable > var2index = null;
			if ( fgSolution instanceof GurobiResult ) {
				modifyGurobiVarNames = true;
				gurobiResults = ( GurobiResult ) fgSolution;
				grbVars = gurobiResults.getModel().getVars();
				var2index = gurobiResults.getVariableToIndex();
			}

			final List< Tr2dSegmentationProblem > timePoints = model.getTrackingModel().getTrackingProblem().getTimepoints();

			problemWriter.write( "\n# === SEGMENT HYPOTHESES =================================================\n" );
			for ( final Tr2dSegmentationProblem t : timePoints ) {
				next_segment_id = -1;
				problemWriter.write( String.format( "\n# t=%d\n", t.getTime() ) );

				// write all segment hypotheses
				final Collection< SegmentNode > segments = t.getSegments();
//				final ArrayList< SegmentNode > segments = ( ArrayList< SegmentNode > ) t.getSegments();
//				Collections.sort( segments, new Comparator< SegmentNode >() {
//
//					@Override
//					public int compare( final SegmentNode o1, final SegmentNode o2 ) {
//						final float x1 = o1.getSegment().getCenterOfMass().getFloatPosition( 0 );
//						final float x2 = o2.getSegment().getCenterOfMass().getFloatPosition( 0 );
//						final float y1 = o1.getSegment().getCenterOfMass().getFloatPosition( 1 );
//						final float y2 = o2.getSegment().getCenterOfMass().getFloatPosition( 1 );
//						if ( x1 - x2 == 0f ) { return Float.compare( y1, y2 ); }
//						return Float.compare( x1, x2 );
//					}
//				} );

				for ( final SegmentNode segment : segments ) {
					mapSeg2Id.put( segment, new ValuePair< Integer, Integer >( t.getTime(), ++next_segment_id ) );
					final String shortName = writeSegmentLine( t.getTime(), segment, next_segment_id, problemWriter );

					if ( modifyGurobiVarNames ) {
						try {
							grbVars[ var2index.get( varmap.getB( segment ) ) ].set( GRB.StringAttr.VarName, shortName );
						} catch ( final GRBException e ) {}
					}
				}
			}

			problemWriter.write( "\n# === ASSIGNMNETS ========================================================\n\n" );
			for ( final Tr2dSegmentationProblem t : timePoints ) {
				next_assignment_id = -1;
				final Collection< SegmentNode > segments = t.getSegments();
				// for each segmetn hyp, write all assignments
				for ( final SegmentNode segment : segments ) {

					final Collection< AppearanceHypothesis > apps = segment.getInAssignments().getAppearances();
					for ( final AppearanceHypothesis app : apps ) {
						mapAss2Id.put( app, new ValuePair<>( t.getTime(), ++next_assignment_id ) );
						final String shortName = writeAppearanceLine( app, mapSeg2Id, problemWriter );

						if ( modifyGurobiVarNames ) {
							try {
								grbVars[ var2index.get( varmap.getB( app ) ) ].set( GRB.StringAttr.VarName, shortName );
							} catch ( final GRBException e ) {}
						}
					}
					final Collection< DisappearanceHypothesis > disapps = segment.getOutAssignments().getDisappearances();
					for ( final DisappearanceHypothesis disapp : disapps ) {
						mapAss2Id.put( disapp, new ValuePair<>( t.getTime(), ++next_assignment_id ) );
						final String shortName = writeDisappearanceLine( disapp, mapSeg2Id, problemWriter );

						if ( modifyGurobiVarNames ) {
							try {
								grbVars[ var2index.get( varmap.getB( disapp ) ) ].set( GRB.StringAttr.VarName, shortName );
							} catch ( final GRBException e ) {}
						}
					}
					final Collection< MovementHypothesis > moves = segment.getOutAssignments().getMoves();
					for ( final MovementHypothesis move : moves ) {
						mapAss2Id.put( move, new ValuePair<>( t.getTime(), ++next_assignment_id ) );
						final String shortName = writeMovementLine( move, mapSeg2Id, problemWriter );

						if ( modifyGurobiVarNames ) {
							try {
								grbVars[ var2index.get( varmap.getB( move ) ) ].set( GRB.StringAttr.VarName, shortName );
							} catch ( final GRBException e ) {}
						}
					}
					final Collection< DivisionHypothesis > divs = segment.getOutAssignments().getDivisions();
					for ( final DivisionHypothesis div : divs ) {
						mapAss2Id.put( div, new ValuePair<>( t.getTime(), ++next_assignment_id ) );
						final String shortName = writeDivisionLine( div, mapSeg2Id, problemWriter );

						if ( modifyGurobiVarNames ) {
							try {
								grbVars[ var2index.get( varmap.getB( div ) ) ].set( GRB.StringAttr.VarName, shortName );
							} catch ( final GRBException e ) {}
						}
					}
					problemWriter.write( "\n" );
				}
			}

			problemWriter.write( "# === CONSTRAINTS ========================================================\n\n" );
			for ( final Tr2dSegmentationProblem t : timePoints ) {
				for ( final ConflictSet cs : t.getConflictSets() ) {
					final List< ValuePair< Integer, Integer > > timeAndIdPairs = new ArrayList<>();
					final Iterator< SegmentNode > it = cs.iterator();
					while ( it.hasNext() ) {
						final SegmentNode segnode = it.next();
						final ValuePair< Integer, Integer > timeAndId = mapSeg2Id.get( segnode );
						if ( timeAndId == null ) throw new IllegalStateException( "this should not be possible -- find bug!" );
						timeAndIdPairs.add( timeAndId );
					}
					// CONFSET <t id...>
					problemWriter.write( "CONFSET " );
					boolean first = true;
					for ( final ValuePair< Integer, Integer > timeAndId : timeAndIdPairs ) {
						if ( !first ) problemWriter.write( " + " );
						problemWriter.write( String.format( "%3d %4d ", timeAndId.a, timeAndId.b ) );
						first = false;
					}
					problemWriter.write( " <= 1\n" );

					// - - - - - - - - - - - - - - - - - - - - -
					// We agreed with Paul and Bogdan that this
					// is explicitly clear and therefore not ne-
					// cessary to be exported.
					if ( export_continuation_constraints ) {
						final Collection< SegmentNode > segments = t.getSegments();
						for ( final SegmentNode segment : segments ) {
							final List< ValuePair< Integer, Integer > > leftSegs = new ArrayList<>();
							for ( final AssignmentNode ass : segment.getInAssignments().getAllAssignments() ) {
								final ValuePair< Integer, Integer > timeAndId = mapAss2Id.get( ass );
								if ( timeAndId == null ) throw new IllegalStateException( "this should not be possible -- find bug!" );
								leftSegs.add( timeAndId );
							}
							final ValuePair< Integer, Integer > segTimeAndId = mapSeg2Id.get( segment );

							// CONT <time> <seg_id> <left_ass_ids as (time, id) pairs...>
							problemWriter.write( String.format( "CONT    %3d %4d ", segTimeAndId.a, segTimeAndId.b ) );
							for ( final ValuePair< Integer, Integer > leftTimeAndId : leftSegs ) {
								problemWriter.write( String.format( "%3d %4d", leftTimeAndId.a, leftTimeAndId.b ) );
							}
							problemWriter.write( "\n" );

							final List< ValuePair< Integer, Integer > > rightSegs = new ArrayList<>();
							for ( final AssignmentNode ass : segment.getOutAssignments().getAllAssignments() ) {
								final ValuePair< Integer, Integer > timeAndId = mapAss2Id.get( ass );
								if ( timeAndId == null ) throw new IllegalStateException( "this should not be possible -- find bug!" );
								rightSegs.add( timeAndId );
							}
							// CONT <time> <seg_id> <right_ass_ids as (time, id) pairs...>
							problemWriter.write( String.format( "CONT    %3d %4d ", segTimeAndId.a, segTimeAndId.b ) );
							for ( final ValuePair< Integer, Integer > rightTimeAndId : rightSegs ) {
								problemWriter.write( String.format( "%3d %4d", rightTimeAndId.a, rightTimeAndId.b ) );
							}
							problemWriter.write( "\n" );
						}
					}
				}
			}

			problemWriter.close();

		} catch ( final IOException e ) {
			JOptionPane
					.showMessageDialog( this, "Cannot write in selected export folder... cancel export!", "File Error", JOptionPane.ERROR_MESSAGE );
			e.printStackTrace();
		}
	}

	/**
	 * @param t
	 * @param segment
	 * @param id
	 * @param writer
	 * @throws IOException
	 */
	private String writeSegmentLine( final int t, final SegmentNode segment, final int id, final BufferedWriter writer ) throws IOException {
		// H <id> <cost> <com_x_pos> <com_y_pos>
		writer.write(
				String.format(
						"H %3d %4d %.16f (%.1f,%.1f)\n",
						t,
						id,
						segment.getCost(),
						segment.getSegment().getCenterOfMass().getFloatPosition( 0 ),
						segment.getSegment().getCenterOfMass().getFloatPosition( 1 ) ) );

		return String.format(
				"H-%d/%d",
				t,
				id );

	}

	/**
	 * @param app
	 * @param mapSeg2Id
	 * @param writer
	 * @throws IOException
	 */
	private String writeAppearanceLine(
			final AppearanceHypothesis app,
			final Map< SegmentNode, ValuePair< Integer, Integer > > mapSeg2Id,
			final BufferedWriter writer )
			throws IOException {
		// APP <time> <segment_id> <cost>
		final ValuePair< Integer, Integer > timeAndId = mapSeg2Id.get( app.getDest() );
		writer.write(
				String.format(
						"APP     %3d %4d %.16f\n",
						timeAndId.a,
						timeAndId.b,
						app.getCost() ) );
		return String.format(
				"APP-%d/%d",
				timeAndId.a,
				timeAndId.b );
	}

	/**
	 * @param disapp
	 * @param mapSeg2Id
	 * @param writer
	 * @throws IOException
	 */
	private String writeDisappearanceLine(
			final DisappearanceHypothesis disapp,
			final Map< SegmentNode, ValuePair< Integer, Integer > > mapSeg2Id,
			final BufferedWriter writer )
			throws IOException {
		// DISAPP <time> <segment_id> <cost>
		final ValuePair< Integer, Integer > timeAndId = mapSeg2Id.get( disapp.getSrc() );
		writer.write(
				String.format(
						"DISAPP  %3d %4d %.16f\n",
						timeAndId.a,
						timeAndId.b,
						disapp.getCost() ) );
		return String.format(
				"DISAPP-%d/%d",
				timeAndId.a,
				timeAndId.b );
	}

	/**
	 * @param move
	 * @param mapSeg2Id
	 * @param writer
	 * @throws IOException
	 */
	private String writeMovementLine(
			final MovementHypothesis move,
			final Map< SegmentNode, ValuePair< Integer, Integer > > mapSeg2Id,
			final BufferedWriter writer )
			throws IOException {
		// MOVE <ass_id> <source_time> <source_segment_id> <dest_time> <dest_segment_id> <cost>
		final ValuePair< Integer, Integer > timeAndId4Src = mapSeg2Id.get( move.getSrc() );
		final ValuePair< Integer, Integer > timeAndId4Dest = mapSeg2Id.get( move.getDest() );
		writer.write(
				String.format(
						"MOVE    %3d %4d %3d %4d %.16f\n",
						timeAndId4Src.a,
						timeAndId4Src.b,
						timeAndId4Dest.a,
						timeAndId4Dest.b,
						move.getCost() ) );
		return String.format(
				"MOVE-%d/%d-%d/%d",
				timeAndId4Src.a,
				timeAndId4Src.b,
				timeAndId4Dest.a,
				timeAndId4Dest.b );
	}

	/**
	 * @param div
	 * @param mapSeg2Id
	 * @param writer
	 * @throws IOException
	 */
	private String writeDivisionLine(
			final DivisionHypothesis div,
			final Map< SegmentNode, ValuePair< Integer, Integer > > mapSeg2Id,
			final BufferedWriter writer )
			throws IOException {
		// DIV <ass_id> <source_segment_id> <dest1_segment_id> <dest2_segment_id> <cost>
		final ValuePair< Integer, Integer > timeAndId4Src = mapSeg2Id.get( div.getSrc() );
		final ValuePair< Integer, Integer > timeAndId4Dest1 = mapSeg2Id.get( div.getDest1() );
		final ValuePair< Integer, Integer > timeAndId4Dest2 = mapSeg2Id.get( div.getDest2() );
		writer.write(
				String.format(
						"DIV     %3d %4d %3d %4d %3d %4d %.16f\n",
						timeAndId4Src.a,
						timeAndId4Src.b,
						timeAndId4Dest1.a,
						timeAndId4Dest1.b,
						timeAndId4Dest2.a,
						timeAndId4Dest2.b,
						div.getCost() ) );
		return String.format(
				"DIV-%d/%d-%d/%d-%d/%d",
				timeAndId4Src.a,
				timeAndId4Src.b,
				timeAndId4Dest1.a,
				timeAndId4Dest1.b,
				timeAndId4Dest2.a,
				timeAndId4Dest2.b );
	}

	/**
	 * @param projectFolderBasePath
	 */
	private void trackingProblemILPExport( final File projectFolderBasePath ) {
		final File exportFile = new File( projectFolderBasePath, "tr2d_problem.lp" );

		try {
			model.getTrackingModel().getSolver().saveLatestModel( exportFile.getAbsolutePath() );
		} catch ( final NullPointerException e ) {
			JOptionPane
					.showMessageDialog( this, "Cannot write LP to file.", "Gurobi Model Error", JOptionPane.ERROR_MESSAGE );
			e.printStackTrace();
		}
	}

	public void schnitzcellExport( final File projectFolderBasePath ) {
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
