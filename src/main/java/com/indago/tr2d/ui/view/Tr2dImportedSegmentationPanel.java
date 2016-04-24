/**
 *
 */
package com.indago.tr2d.ui.view;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JPanel;

import com.indago.tr2d.ui.model.Tr2dImportedSegmentationModel;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.DoubleType;

/**
 * @author jug
 */
public class Tr2dImportedSegmentationPanel extends JPanel {

	private static final long serialVersionUID = -4610859107829248753L;

	private final Tr2dImportedSegmentationModel model;

	public Tr2dImportedSegmentationPanel( final Tr2dImportedSegmentationModel model ) {
		super( new BorderLayout() );
		this.model = model;
		buildGui();

		final List< RandomAccessibleInterval< DoubleType > > segs = model.getSegmentHypotheses();
	}

	private void buildGui() {
	}

}
