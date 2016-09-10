/**
 *
 */
package com.indago.tr2d;

import javax.swing.JFrame;

import com.indago.tr2d.plugins.seg.Tr2dSegmentationPluginService;

import net.imagej.ops.OpService;

/**
 * @author jug
 */
public class Tr2dContext {

	public static OpService ops = null;
	public static Tr2dSegmentationPluginService segPlugins = null;
	public static JFrame guiFrame = null;

}
