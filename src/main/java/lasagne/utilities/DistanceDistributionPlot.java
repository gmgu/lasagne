/*
 * Copyright (c) 2012, LASAGNE and/or its affiliates. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * - Neither the name of LASAGNE or the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior
 * written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package lasagne.utilities;

import java.awt.Color;

import javax.swing.JDialog;
import javax.swing.JFrame;

import de.erichseifert.gral.data.DataTable;
import de.erichseifert.gral.plots.BarPlot;
import de.erichseifert.gral.plots.XYPlot;
import de.erichseifert.gral.plots.axes.AxisRenderer;
import de.erichseifert.gral.plots.lines.DefaultLineRenderer2D;
import de.erichseifert.gral.plots.lines.LineRenderer;
import de.erichseifert.gral.plots.points.PointRenderer;
import de.erichseifert.gral.ui.InteractivePanel;
import de.erichseifert.gral.util.Insets2D;

public class DistanceDistributionPlot extends JDialog {
	public DistanceDistributionPlot(JFrame owner, double[] values, String fn) {
		super(owner, true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setSize(700, 500);

		@SuppressWarnings("unchecked")
		DataTable data = new DataTable(Double.class, Double.class);
		for (int i = 0; i < values.length; i++) {
			Double x = (double) i;
			data.add(x, values[i]);
		}
		XYPlot plot = new XYPlot(data);
		getContentPane().add(new InteractivePanel(plot));
		LineRenderer lines = new DefaultLineRenderer2D();
		plot.setLineRenderer(data, lines);
		Color color = new Color(0.0f, 0.3f, 1.0f);
		double insetsTop = 20.0, insetsLeft = 80.0, insetsBottom = 60.0, insetsRight = 40.0;
		plot.setInsets(new Insets2D.Double(insetsTop, insetsLeft, insetsBottom,
				insetsRight));
		plot.setSetting(BarPlot.TITLE, "Distance distribution of " + fn);
		plot.getPointRenderer(data).setSetting(PointRenderer.COLOR, color);
		plot.getLineRenderer(data).setSetting(LineRenderer.COLOR, color);
		plot.getAxisRenderer(XYPlot.AXIS_X).setSetting(AxisRenderer.LABEL,
				"Distance");
		plot.getAxisRenderer(XYPlot.AXIS_Y).setSetting(AxisRenderer.LABEL,
				"Frequency (percentage)");
		plot.getAxisRenderer(XYPlot.AXIS_Y).setSetting(
				AxisRenderer.LABEL_DISTANCE, 2);
		plot.getAxisRenderer(XYPlot.AXIS_Y).setSetting(
				AxisRenderer.TICKS_SPACING, 0.01);
		setLocationRelativeTo(null);
		setVisible(true);
	}
}
