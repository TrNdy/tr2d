/**
 *
 */
package com.indago.tr2d;

import javax.swing.JFrame;

import com.indago.plugins.seg.IndagoSegmentationPluginService;

import net.imagej.ops.OpService;

/**
 * @author jug
 */
public class Tr2dContext {

	public static OpService ops = null;
	public static IndagoSegmentationPluginService segPlugins = null;
	public static JFrame guiFrame = null;

}
