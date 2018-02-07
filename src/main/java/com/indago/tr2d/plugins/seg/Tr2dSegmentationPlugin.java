/**
 *
 */
package com.indago.tr2d.plugins.seg;

import java.util.List;

import javax.swing.JPanel;

import org.scijava.log.Logger;

import com.indago.tr2d.ui.model.Tr2dModel;

import net.imagej.ImageJPlugin;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.IntType;

/**
 * @author jug
 */
public interface Tr2dSegmentationPlugin extends ImageJPlugin {

	JPanel getInteractionPanel();

	List< RandomAccessibleInterval< IntType > > getOutputs();

	void setTr2dModel( Tr2dModel model );

	String getUiName();

	public void setLogger( Logger logger );

	default boolean isUsable() { return true; };
}
