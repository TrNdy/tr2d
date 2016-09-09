/**
 *
 */
package com.indago.tr2d.plugins.seg;

import java.util.List;

import javax.swing.JPanel;

import com.indago.tr2d.ui.model.Tr2dModel;

import net.imagej.ImageJPlugin;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.IntType;

/**
 * @author jug
 */
public interface Tr2dSegmentationPlugin extends ImageJPlugin {

	public JPanel getInteractionPanel();

	public < T extends NativeType< T > > void addInput( RandomAccessibleInterval< T > rai );

	public < T extends NativeType< T > > List< RandomAccessibleInterval< T > > getInputs();

	public List< RandomAccessibleInterval< IntType > > getOutputs();

	public void setTr2dModel( Tr2dModel model );

	public String getUiName();
}
