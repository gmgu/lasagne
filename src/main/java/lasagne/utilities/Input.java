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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.DefaultEditorKit;

public class Input {
	public static char getChar(String message) {
		InputFrame inf = new InputFrame(message);
		String text = inf.text;
		if ((text != null) && (text.length() > 0))
			return text.charAt(0);
		return (' ');
	}

	public static String getPastableString(JFrame owner, String message) {
		StringInputFrame inf = new StringInputFrame(owner, message);
		return (inf.text);
	}

	public static String getString(String message) {
		InputFrame inf = new InputFrame(message);
		return (inf.text);
	}

	public static long getLong(String message) {
		InputFrame inf = new InputFrame(message);
		String text = inf.text;
		long number;
		try {
			number = Long.parseLong(text);
		} catch (NumberFormatException nfe) {
			number = 0;
		}
		return (number);
	}

	public static int getInt(String message) {
		InputFrame inf = new InputFrame(message);
		String text = inf.text;
		int number;
		try {
			number = Integer.parseInt(text);
		} catch (NumberFormatException nfe) {
			number = 0;
		}
		return (number);
	}

	public static double getDouble(String message) {
		InputFrame inf = new InputFrame(message);
		String text = inf.text;
		double number;
		try {
			number = Double.parseDouble(text);
		} catch (NumberFormatException nfe) {
			number = 0;
		}
		return (number);
	}
}

class InputFrame extends JFrame {
	String text;

	public InputFrame(String message) {
		super();
		text = JOptionPane.showInputDialog(this, message);
		dispose();
	}
}

class StringInputFrame extends JDialog {
	public String text;
	JTextArea textArea;

	public StringInputFrame(JFrame owner, String message) {
		super(owner, true);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		Container content = getContentPane();
		textArea = new JTextArea("", 5, 100);
		JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane
				.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		content.add(scrollPane, BorderLayout.CENTER);
		Action actions[] = textArea.getActions();
		Action pasteAction = TextUtilities.findAction(actions,
				DefaultEditorKit.pasteAction);
		JPanel panel = new JPanel();
		content.add(panel, BorderLayout.SOUTH);
		JButton pasteButton = new JButton(pasteAction);
		pasteButton.setText("Paste");
		panel.add(pasteButton);
		JButton okButton = new JButton();
		okButton.setText("OK");
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				text = textArea.getText();
				dispose();
			}
		});
		panel.add(okButton);
		JButton cancelButton = new JButton();
		cancelButton.setText("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				text = null;
				dispose();
			}
		});
		panel.add(cancelButton);
		setTitle(message);
		setSize(700, 100);
		setLocationRelativeTo(null);
		setVisible(true);
	}
}

class TextUtilities {
	private TextUtilities() {
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Action findAction(Action actions[], String key) {
		Hashtable commands = new Hashtable();
		for (int i = 0; i < actions.length; i++) {
			Action action = actions[i];
			commands.put(action.getValue(Action.NAME), action);
		}
		return (Action) commands.get(key);
	}
}
