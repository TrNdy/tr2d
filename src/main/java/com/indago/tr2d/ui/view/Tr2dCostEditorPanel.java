/**
 *
 */
package com.indago.tr2d.ui.view;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.indago.costs.CostFactory;
import com.indago.costs.CostParams;
import com.indago.tr2d.ui.model.Tr2dTrackingModel;

import net.miginfocom.swing.MigLayout;


/**
 * @author jug
 */
public class Tr2dCostEditorPanel extends JPanel {

	private static final long serialVersionUID = 2601748326346367034L;

	private final Tr2dTrackingModel model;

	/**
	 * @param model
	 */
	public Tr2dCostEditorPanel( final Tr2dTrackingModel model ) {
		this.model = model;
		buildGui();
	}

	private void buildGui() {
		fillCostEditPanel( this );
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
							System.out.println( "SET!" );
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
}
