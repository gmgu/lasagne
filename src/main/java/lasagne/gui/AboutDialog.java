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

package lasagne.gui;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class AboutDialog extends JDialog {
	public AboutDialog(JFrame owner) {
		super(owner, true);
		try {
			ClassLoader cl = getClass().getClassLoader();
			setTitle("About LASAGNE");
			setSize(600, 480);
			Box b = Box.createVerticalBox();
			b.add(Box.createGlue());
			ImageIcon icon = new ImageIcon(
					cl.getResource("img/lasagneheader.png"));
			b.add(new JLabel("", icon, JLabel.CENTER));
			String tab = "&nbsp;&nbsp;";
			String s = "<HTML>" + tab + "<BR>";
			s += tab + "LASAGNE is currently maintained by<BR>";
			s += tab
					+ tab
					+ tab
					+ tab
					+ "Pilu Crescenzi (contact developer: pierluigi.crescenzi@unifi.it)<BR>";
			s += tab + tab + tab + tab + "Roberto Grossi<BR>";
			s += tab + tab + tab + tab + "Leonardo Lanzi<BR>";
			s += tab + tab + tab + tab + "Andrea Marino<BR>";
			s += tab
					+ "LASAGNE uses software produced within the following projects:<BR>";
			s += tab + tab + tab + tab + "Apache<BR>";
			s += tab + tab + tab + tab + "GRAL Java Graphing<BR>";
			s += tab + "LASAGNE implements algorithms described in:<BR>";
			s += tab
					+ tab
					+ tab
					+ tab
					+ "P. Crescenzi, R. Grossi, C. Imbrenda, L. Lanzi, A. Marino<BR>";
			s += tab + tab + tab + tab + tab
					+ "Finding the Diameter in Real-World Graphs<BR>";
			s += tab
					+ tab
					+ tab
					+ tab
					+ tab
					+ tab
					+ "Experimentally Turning a Lower Bound into an Upper Bound<BR>";
			s += tab + tab + tab + tab + tab + "ESA (1) 2010: 302-313<BR>";
			s += tab + tab + tab + tab
					+ "P. Crescenzi, R. Grossi, L. Lanzi, A. Marino<BR>";
			s += tab
					+ tab
					+ tab
					+ tab
					+ tab
					+ "A Comparison of Three Algorithms for Approximating the<BR>";
			s += tab + tab + tab + tab + tab + tab
					+ "Distance Distribution in Real-World Graphs<BR>";
			s += tab + tab + tab + tab + tab + "TAPAS 2011: 92-103<BR>";
			s += tab
					+ tab
					+ tab
					+ tab
					+ "P. Crescenzi, R. Grossi, M. Habib, L. Lanzi, A. Marino<BR>";
			s += tab
					+ tab
					+ tab
					+ tab
					+ tab
					+ "On Computing the Diameter of Real-World Undirected Graphs<BR>";
			s += tab + tab + tab + tab + tab + "Submitted for publication<BR>";
			s += tab + tab + tab + tab
					+ "P. Crescenzi, R. Grossi, L. Lanzi, A. Marino<BR>";
			s += tab
					+ tab
					+ tab
					+ tab
					+ tab
					+ "On Computing the Diameter of Real-World Directed (Weighted) Graphs<BR>";
			s += tab + tab + tab + tab + tab + "SEA 2012 (to appear)<BR>";
			s += tab
					+ "<EM>If you use LASAGNE, please cite the corresponding paper(s).</EM><HTML>";
			JLabel label = new JLabel(s);
			label.setFont(new Font("Courier", Font.PLAIN, 12));
			b.add(label);
			b.add(Box.createGlue());
			getContentPane().add(b, "Center");

			JPanel p2 = new JPanel();
			JButton ok = new JButton("Ok");
			p2.add(ok);
			getContentPane().add(p2, "South");
			ok.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					dispose();
				}
			});
			setLocationRelativeTo(null);
			setVisible(true);
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			Lasagne.logger
					.info("======= Java Exception: if you want you can report it\n"
							+ sw + "=======");
		}
	}
}
