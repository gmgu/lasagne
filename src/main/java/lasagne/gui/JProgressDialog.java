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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JToolBar;

public class JProgressDialog extends JDialog {
	private int height;
	private int width;
	private JLabel label;
	private JProgressBar progressbar;
	private ProgressTask task;
	private Thread t;
	private JProgressDialog thisJPD;
	private JToolBar toolbar;
	private boolean indeterminate;
	private int startValue;
	private int endValue;

	public JProgressDialog(Frame owner, String title, String label, boolean modal, ProgressTask pt) throws HeadlessException {
		super(owner, title, modal);
		height = 130;
		width = owner.getBounds().width / 2;
		this.task = pt;
		thisJPD = this;
		indeterminate = true;
		init(label);
		start();
	}

	public JProgressDialog(Frame owner, String title, String label, boolean modal, int start, int end, ProgressTask pt) throws HeadlessException {
		super(owner, title, modal);
		height = 130;
		width = owner.getBounds().width / 3;
		this.task = pt;
		thisJPD = this;
		indeterminate = false;
		startValue = start;
		endValue = end;
		init(label);
		start();
	}

	private JToolBar getToolBar() {
		JToolBar res = new JToolBar();
		res.setLayout(new FlowLayout());
		res.setFloatable(false);
		JButton stop = new JButton("Cancel");
		stop.addActionListener(new ActionListener() {
			@SuppressWarnings("deprecation")
			public void actionPerformed(ActionEvent ev) {
				t.stop();
				thisJPD.setVisible(false);
				thisJPD.dispose();
				System.gc();
			}
		});
		res.add(stop);
		return res;
	}

	private void start() {
		task.setDialog(this);
		t = new Thread(task);
		t.start();
		this.setVisible(true);
	}

	private void init(String l) {
		JPanel internal = new JPanel(new BorderLayout());
		JPanel foo = new JPanel();
		label = new JLabel(l);
		foo.add(label);
		internal.add(foo, BorderLayout.NORTH);
		progressbar = new JProgressBar(JProgressBar.HORIZONTAL);
		progressbar.setIndeterminate(indeterminate);
		if (!indeterminate) {
			progressbar.setStringPainted(true);
		}
		progressbar.setMinimum(startValue);
		progressbar.setMaximum(endValue);
		progressbar.setValue(startValue);
		foo = new JPanel();
		foo.add(progressbar);
		internal.add(foo, BorderLayout.CENTER);
		toolbar = getToolBar();
		internal.add(toolbar, BorderLayout.SOUTH);
		setContentPane(internal);
		setSize(new Dimension(width, height));
		setLocationRelativeTo(getOwner());
	}

	public void step(String step) {
		label.setText(step);
		if (!indeterminate) {
			progressbar.setValue(progressbar.getValue() + 1);
		}
		repaint();
	}

	public void removeCancel() {
		this.remove(toolbar);
		repaint();
	}

	public void endStep() {
		label.setText("");
		repaint();
	}

	public void completed() {
		this.setVisible(false);
		this.dispose();
	}
}