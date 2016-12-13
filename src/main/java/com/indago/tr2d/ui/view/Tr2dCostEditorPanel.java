/**
 *
 */
package com.indago.tr2d.ui.view;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import com.indago.costs.CostFactory;
import com.indago.costs.CostParams;
import com.indago.tr2d.Tr2dLog;
import com.indago.tr2d.ui.model.Tr2dSolDiffModel;
import com.indago.tr2d.ui.model.Tr2dTrackingModel;
import com.indago.tr2d.ui.util.UniversalFileChooser;

import bdv.util.Bdv;
import bdv.util.BdvHandlePanel;
import net.miginfocom.swing.MigLayout;
import weka.gui.ExtensionFileFilter;


/**
 * @author jug
 */
public class Tr2dCostEditorPanel extends JPanel implements ActionListener {

	private static final long serialVersionUID = 2601748326346367034L;

	private final Tr2dTrackingModel model;
	private final Tr2dSolDiffModel diffModel;

	private JButton bLoadCosts;
	private JButton bSaveCosts;
	private JButton bRetrack;


	/**
	 * @param model
	 */
	public Tr2dCostEditorPanel( final Tr2dTrackingModel model, final Tr2dSolDiffModel diffModel ) {
		this.model = model;
		this.diffModel = diffModel;
		buildGui();
	}

	private void buildGui() {
		this.setLayout( new BorderLayout() );

		final MigLayout lControls = new MigLayout( "", "[][grow]", "" );
		final JPanel panelControls = new JPanel( lControls );
		bLoadCosts = new JButton( "load" );
		bLoadCosts.addActionListener( this );
		bSaveCosts = new JButton( "save" );
		bSaveCosts.addActionListener( this );
		bRetrack = new JButton( "retrack" );
		bRetrack.addActionListener( this );
		panelControls.add( bLoadCosts, "" );
		panelControls.add( bSaveCosts, "wrap" );
		panelControls.add( bRetrack, "span, growx, wrap" );
		this.add( panelControls, BorderLayout.WEST );

		final JPanel panelCenter = new JPanel( new BorderLayout() );
		diffModel.bdvSetHandlePanel(
				new BdvHandlePanel( ( Frame ) this.getTopLevelAncestor(), Bdv
						.options()
						.is2D()
						.inputTriggerConfig( model.getTr2dModel().getDefaultInputTriggerConfig() ) ) );
		diffModel.populateBdv();
		panelCenter.add( diffModel.bdvGetHandlePanel().getViewerPanel(), BorderLayout.CENTER );
		this.add( panelCenter, BorderLayout.CENTER );

		final JPanel panelCosts = new JPanel();
		fillCostEditPanel( panelCosts );
		final JScrollPane sp = new JScrollPane( panelCosts );
		sp.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		sp.setViewportBorder( null );
		this.add( sp, BorderLayout.EAST );
	}

	private void fillCostEditPanel( final JPanel panel ) {
		final MigLayout layout = new MigLayout( "", "[][grow][]", "" );
		panel.setLayout( layout );

		for ( final CostFactory< ? > cf : model.getCostFactories() ) {
			final MigLayout layoutCF = new MigLayout( "", "[][grow]", "" );
			final JPanel panelCF = new JPanel( layoutCF );
			panelCF.setBorder( BorderFactory.createTitledBorder( cf.getName() ) );

			//loop over all params and create label/field pair
			final CostParams params = cf.getParameters();
			for ( int i = 0; params != null && i < params.size(); i++ ) {
				final String name = params.getName( i );
				final double value = params.get( i );
				final JLabel labelParam = new JLabel( name );
				final int index = i;
				final JTextField txtValue = new JTextField( "" + value );
				txtValue.addActionListener( new ActionListener() {

					@Override
					public void actionPerformed( final ActionEvent e ) {
						try {
							params.set( index, Double.parseDouble( txtValue.getText() ) );
							Tr2dLog.log.trace( "SET " + name + " TO " + params.get( index ) );
						} catch ( final NumberFormatException nfe ) {
							Tr2dLog.log.error( "NOPE! :)" );
						}
					}
				} );
				panelCF.add( labelParam, "" );
				panelCF.add( txtValue, "growx, wrap" );
			}
			panel.add( panelCF, "growx, wrap" );
		}
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed( final ActionEvent e ) {
		if ( e.getSource().equals( bRetrack ) ) {
			model.reset();
			model.runInThread( true );
		} else if ( e.getSource().equals( bSaveCosts ) ) {
			final File costsFile = UniversalFileChooser.showSaveFileChooser(
					model.getTr2dModel().getMainPanel().getTopLevelAncestor(),
					"",
					"Choose file for tr2d cost parameter...",
					new ExtensionFileFilter( "tr2dcosts", "Tr2d cost parametrization file" ) );
			try {
				exportCostParametrization( costsFile );
			} catch ( final IOException e1 ) {
				JOptionPane.showMessageDialog( this, "Cannot write to selected file.", "File Error", JOptionPane.ERROR_MESSAGE );
			}
		} else if ( e.getSource().equals( bLoadCosts ) ) {
			final File costsFile = UniversalFileChooser.showLoadFileChooser(
					model.getTr2dModel().getMainPanel().getTopLevelAncestor(),
					"",
					"Choose tr2d cost parameters file...",
					new ExtensionFileFilter( "tr2dcosts", "Tr2d cost parametrization file" ) );
			try {
				importCostParametrization( costsFile );
			} catch ( final IOException e1 ) {
				JOptionPane.showMessageDialog( this, "Cannot write to selected file.", "File Error", JOptionPane.ERROR_MESSAGE );
			}
		}
	}

	/**
	 * @param costsFile
	 */
	private void importCostParametrization( final File costsFile ) throws IOException {
		Tr2dLog.log.trace( "Loading should happen now... :)" );
	}

	/**
	 * @param costsFile
	 * @throws IOException
	 */
	private void exportCostParametrization( final File costsFile ) throws IOException {
		final SimpleDateFormat sdfDate = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
		final Date now = new Date();
		final String strNow = sdfDate.format( now );

		BufferedWriter costWriter;
		costWriter = new BufferedWriter( new FileWriter( costsFile ) );
		costWriter.write( "# Tr2d cost parameters export from " + strNow + "\n" );

		int size = 0;
		for ( final CostFactory< ? > cf : model.getCostFactories() ) {
			size += cf.getParameters().size();
			costWriter.write( String.format( "# PARAMS FOR: %s\n", cf.getName() ) );
			final double[] params = cf.getParameters().getAsArray();
			for ( int j = 0; j < params.length; j++ ) {
				costWriter.write( String.format( "# >> %s\n", cf.getParameters().getName( j ) ) );
				costWriter.write( String.format( "%f\n", params[ j ] ) );
			}
		}
		costWriter.flush();
		costWriter.close();
	}
}
