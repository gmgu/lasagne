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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.StringTokenizer;

import javax.swing.filechooser.FileSystemView;
import javax.swing.table.AbstractTableModel;

/*
 * This class implements the table model for showing NDE files contained in the
 * directory selected in the tree list. Only NDE files are included in the
 * table. For each file, the following information are shown: the name, the size
 * in bytes, the last modification date, the number of nodes, and whether the
 * graph is directed or weighted. These last two information are read from the
 * first line of the file.
 */
class FileTableModel extends AbstractTableModel {
	private File[] files;
	private FileSystemView fileSystemView = FileSystemView.getFileSystemView();
	private String[] columns = { "File", "Size", "Last Modified", "n", "D", "W" };
	public static String[] columnToolTips = { null, "Size in bytes", null,
			"Number of nodes", "Directed", "Weighted" };
	private boolean exception = false;

	FileTableModel() {
		this(new File[0]);
	}

	FileTableModel(File[] files) {
		this.files = files;
	}

	public Object getValueAt(int row, int column) {
		File file = files[row];
		boolean isOriented = false;
		boolean isWeighted = false;
		int n = 0;
		if (file.exists() && !file.isDirectory()
				&& file.getName().endsWith(".nde")) {
			try {
				BufferedReader br = new BufferedReader(new FileReader(file));
				String line = br.readLine();
				StringTokenizer lineTokens = new StringTokenizer(line, " ");
				n = Integer.parseInt(lineTokens.nextToken());
				if (lineTokens.hasMoreTokens()) {
					if (lineTokens.nextToken().equals("1")) {
						isOriented = true;
					}
					if (lineTokens.hasMoreTokens()) {
						if (lineTokens.nextToken().equals("1")) {
							isWeighted = true;
						}
					}
				}
				br.close();
				exception = false;
			} catch (Exception e) {
				if (!exception) {
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					e.printStackTrace(pw);
					Lasagne.logger
							.info("======= Java Exception: if you want you can report it\n"
									+ sw + "=======");
					exception = true;
				}
				return "";
			}
		}
		switch (column) {
		case 0:
			return fileSystemView.getSystemDisplayName(file);
		case 1:
			return file.length();
		case 2:
			return file.lastModified();
		case 3:
			return n;
		case 4:
			return isOriented;
		case 5:
			return isWeighted;
		default:
			System.err.println("Logic Error");
		}
		return "";
	}

	public int getColumnCount() {
		return columns.length;
	}

	public Class<?> getColumnClass(int column) {
		switch (column) {
		case 0:
			return String.class;
		case 1:
			return Long.class;
		case 2:
			return Date.class;
		case 3:
			return Long.class;
		case 4:
			return Boolean.class;
		case 5:
			return Boolean.class;
		}
		return String.class;
	}

	public String getColumnName(int column) {
		return columns[column];
	}

	public int getRowCount() {
		return files.length;
	}

	public File getFile(int row) {
		return files[row];
	}

	public void setFiles(File[] files) {
		int nf = 0;
		for (int f = 0; f < files.length; f++) {
			if (files[f].isFile() && files[f].getName().endsWith(".nde")) {
				nf = nf + 1;
			}
		}
		this.files = new File[nf];
		int cf = 0;
		for (int f = 0; f < files.length; f++) {
			if (files[f].isFile() && files[f].getName().endsWith(".nde")) {
				this.files[cf++] = files[f];
			}
		}
		fireTableDataChanged();
	}
}