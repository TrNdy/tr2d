/**
 *
 */
package com.indago.tr2d.ui.view;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import com.indago.costs.CostFactory;
import com.indago.costs.CostParams;
import com.indago.tr2d.ui.model.Tr2dSolDiffModel;
import com.indago.tr2d.ui.model.Tr2dTrackingModel;

import bdv.util.Bdv;
import bdv.util.BdvHandlePanel;
import net.miginfocom.swing.MigLayout;


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
		final MigLayout layout = new MigLayout( "", "[][grow][]", "" );
		this.setLayout( layout );

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
		this.add( panelControls, "growy" );

		final JPanel panelCenter = new JPanel( new BorderLayout() );
		diffModel.bdvSetHandlePanel(
				new BdvHandlePanel( ( Frame ) this.getTopLevelAncestor(), Bdv
						.options()
						.is2D()
						.inputTriggerConfig( model.getTr2dModel().getDefaultInputTriggerConfig() ) ) );
		diffModel.populateBdv();
		panelCenter.add( diffModel.bdvGetHandlePanel().getViewerPanel(), BorderLayout.CENTER );
		this.add( panelCenter, "growx, growy" );

		final JPanel panelCosts = new JPanel();
		fillCostEditPanel( panelCosts );
		final JScrollPane sp = new JScrollPane( panelCosts );
		sp.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
//		sp.setBorder( BorderFactory.createEmptyBorder() );
		sp.setViewportBorder( null );
		this.add( sp, "growy" );
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
							System.out.println( "SET " + name + " TO " + params.get( index ) );
						} catch ( final NumberFormatException nfe ) {
							System.err.println( "NOPE! :)" );
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
			// TODO update costs without the need for regeneration of everything...
//			model.updateCosts();
//			model.prepareFG();
			model.reset();
			model.runInThread( true );
		} else if ( e.getSource().equals( bSaveCosts ) ) {
			int size = 0;
			for ( final CostFactory< ? > cf : model.getCostFactories() ) {
				size += cf.getParameters().size();
			}
			final double[] allParams = new double[ size ];
			int i = 0;
			for ( final CostFactory< ? > cf : model.getCostFactories() ) {
				final double[] params = cf.getParameters().getAsArray();
				for (int j=0; j<params.length; j++) {
					allParams[i++] = params[j];
				}
			}
			System.out.print( "Params: " );
			for ( final double value : allParams ) {
				System.out.print( String.format( "%.2f; ", value ) );
			}
			System.out.print( "\n" );
		}
	}
}
