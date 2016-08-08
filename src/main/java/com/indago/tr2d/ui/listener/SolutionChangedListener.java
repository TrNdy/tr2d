/**
 *
 */
package com.indago.tr2d.ui.listener;

import com.indago.fg.Assignment;
import com.indago.pg.IndicatorNode;

/**
 * @author jug
 */
public interface SolutionChangedListener {

	public void solutionChanged( Assignment< IndicatorNode > newAssignment );
}
