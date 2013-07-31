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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import lasagne.graphs.ArrayGraph;
import lasagne.methods.FourSweep;
import lasagne.methods.IFub;
import lasagne.methods.WeightedIFub;
import lasagne.utilities.ArrayUtils;
import lasagne.utilities.ErrorMessage;
import lasagne.utilities.Input;
import lasagne.utilities.DistanceDistributionPlot;
import lasagne.utilities.PairArray;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class Lasagne {
	/*
	 * This inner class implements the task of computing the diameter of the
	 * currently opened graph by executing a BFS or a Dijkstra visit from each
	 * node. At the end of the execution, the value of the diameter is shown
	 * besides the corresponding label in the user interface.
	 */
	private class DiameterWorker extends ProgressTask {
		public void execute() {
			logger.warn("Network file: " + openedFile.getName());
			logger.warn("Number of nodes: " + graph.getN());
			logger.warn("Number of edges: " + graph.getM());
			long ed = 0;
			PairArray rst = null;
			for (int u = 0; u < graph.getN(); u++) {
				rst = graph.visit(u);
				if (rst == null) {
					break;
				}
				int[] dist = rst.getFirstArray();
				int edu = 0;
				for (int v = 0; v < graph.getN(); v++) {
					if (dist[v] < Integer.MAX_VALUE && edu < dist[v]) {
						edu = dist[v];
					}
					if (dist[v] < Integer.MAX_VALUE && ed < dist[v]) {
						ed = dist[v];
					}
				}
				step("Percentage of done BFSes/Dijkstras");
			}
			if (rst != null) {
				Lasagne.logger.warn("Diameter is " + ed);
				setDiameterDetails(ed);
			} else {
				ErrorMessage.showErrorMessage(
						"The diameter could not be computed", "Warning");
			}
		}
	}

	/*
	 * This inner class implements the task of computing the distance
	 * distribution of the currently opened graph by executing a sampling of its
	 * nodes. This method is described in Crescenzi, Grossi, Lanzi, Marino, 'A
	 * Comparison of Three Algorithms for Approximating the Distance
	 * Distribution in Real-World Graphs' (presented at TAPAS 2011).
	 */
	private class DistDistWorker extends ProgressTask {
		public void execute() {
			logger.warn("Network file: " + openedFile.getName());
			logger.info("Number of nodes: " + graph.getN());
			logger.info("Number of edges: " + graph.getM());
			int u = (int) (Math.random() * graph.getN());
			int[] res = graph.visit(u).getFirstArray();
			int ub = 2 * res[ArrayUtils.getIndexOfMax(res)];
			logger.info("Upper bound on maximum distance: " + ub);
			int[] dd = new int[ub];
			for (int i = 0; i < ub; i++) {
				dd[i] = 0;
			}
			for (int i = 0; i < runs; i++) {
				u = (int) (Math.random() * graph.getN());
				logger.info("Sampled node " + u);
				res = graph.visit(u).getFirstArray();
				for (int v = 0; v < graph.getN(); v++) {
					if (res[v] > 0) {
						dd[res[v]]++;
					}
				}
				step("Percentage of sampled nodes");
			}
			logger.info("Starting normalization of distribution");
			long total = 0;
			for (int v = 0; v < ub; v++) {
				total += dd[v];
			}
			long curr = 0;
			double[] dres = new double[ub];
			for (int cx = 0; cx < ub; cx++) {
				curr += dd[cx] / total;
				dres[cx] = dd[cx] / (double) total;
				if (cx != 0 && curr == 1) {
					break;
				}
			}
			String out = "";
			for (int cx = 0; cx < ub; cx++) {
				out = out + dres[cx] + " ";
			}
			logger.warn("Distance distribution:");
			logger.warn(out);
			new DistanceDistributionPlot((JFrame) gui.getTopLevelAncestor(), dres,
					openedFile.getName());
		}
	}

	/*
	 * This inner class implements the task of downloading a NDE file (possibly
	 * ZIPPED) from the web. The extension of the file must be either '.nde' or
	 * '.zip'. The file is saved in the current directory.
	 */
	private class DownloadFileWorker extends ProgressTask {
		URLConnection uc;
		int contentLength;

		public DownloadFileWorker(URLConnection uc, int contentLength) {
			this.uc = uc;
			this.contentLength = contentLength;
		}

		public void execute() {
			logger.warn("Network file: " + uc.getURL().getFile());
			try {
				InputStream raw = uc.getInputStream();
				InputStream in = new BufferedInputStream(raw);
				byte[] data = new byte[contentLength];
				int bytesRead = 0;
				int offset = 0;
				while (offset < contentLength) {
					if (data.length - offset > 10000) {
						bytesRead = in.read(data, offset, 10000);
					} else {
						bytesRead = in.read(data, offset, data.length - offset);
					}
					if (bytesRead == -1)
						break;
					offset += bytesRead;
					step("Percentage of downloaded bytes");
				}
				in.close();

				if (offset != contentLength) {
					throw new IOException("Only read " + offset
							+ " bytes; Expected " + contentLength + " bytes");
				}
				String fileName = uc.getURL().getFile();
				fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
				fileName = tree.getSelectionPath().getLastPathComponent() + "/"
						+ fileName;
				FileOutputStream out = new FileOutputStream(fileName);
				out.write(data);
				out.flush();
				out.close();
				if (fileName.endsWith(".zip")) {
					final int BUFFER = 2048;
					BufferedOutputStream dest = null;
					FileInputStream fis = new FileInputStream(fileName);
					ZipInputStream zis = new ZipInputStream(
							new BufferedInputStream(fis));
					ZipEntry entry;
					while ((entry = zis.getNextEntry()) != null) {
						logger.warn("Extracting: " + entry);
						int count;
						data = new byte[BUFFER];
						FileOutputStream fos = new FileOutputStream(tree
								.getSelectionPath().getLastPathComponent()
								+ "/" + entry.getName());
						dest = new BufferedOutputStream(fos, BUFFER);
						while ((count = zis.read(data, 0, BUFFER)) != -1) {
							dest.write(data, 0, count);
						}
						dest.flush();
						dest.close();
					}
					zis.close();
					File file = new File(fileName);
					file.delete();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			logger.warn("Download completed");
		}
	}

	/*
	 * This inner class implements the task of computing a lower bound for the
	 * diameter of the currently opened graph by executing a given number of
	 * times (stored in the runs field) the 4-sweep method.
	 */
	private class FourSweepWorker extends ProgressTask {
		public void execute() {
			logger.warn("Network file: " + openedFile.getName());
			logger.warn("Number of nodes: " + graph.getN());
			logger.warn("Number of edges: " + graph.getM());
			long lb = 0;
			for (int i = 0; i < runs; i++) {
				long st = System.currentTimeMillis();
				long[] d = FourSweep.run(graph);
				if (d[0] > lb) {
					lb = d[0];
					long et = System.currentTimeMillis();
					if (et - st < 100)
						try {
							Thread.sleep(500 - (et - st));
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
				}
				step("Lower bound is " + lb + " at iteration " + i);
				Lasagne.logger.info("Lower bound is " + lb + " at iteration "
						+ i);
			}
			Lasagne.logger.warn("Lower bound is " + lb);
		}
	}

	/*
	 * This inner class implements the task of computing the diameter of the
	 * currently opened graph by executing a given number of times (stored in
	 * the runs field) the iFUB method.
	 */
	private class IFUBWorker extends ProgressTask {
		public void execute() {
			logger.warn("Network file: " + openedFile.getName());
			logger.warn("Number of nodes: " + graph.getN());
			logger.warn("Number of edges: " + graph.getM());
			long[][] d = new long[runs][];
			boolean error = false;
			if (!graph.isWeighted()) {
				IFub sf = new IFub();
				for (int e = 0; e < runs; e++) {
					d[e] = sf.run(graph, 0);
					if (d[e] == null) {
						error = true;
						break;
					} else {
						step("Run " + e + " completed");
					}
				}
			} else {
				WeightedIFub wsf = new WeightedIFub();
				for (int e = 0; e < runs; e++) {
					d[e] = wsf.run(graph, 0);
					if (d[e] == null) {
						error = true;
						break;
					} else {
						step("Run " + e + " completed");
					}
				}
			}
			if (!error) {
				logger.warn("Diameter is " + d[0][0]);
				setDiameterDetails(d[0][0]);
				long sum = 0;
				for (int e = 0; e < runs; e++) {
					logger.info("Run " + e + ": " + d[e][2]
							+ " BFSes/Dijkstras");
					sum = sum + d[e][2];
				}
				logger.warn("Average number of BFSes/Dijkstras: " + (float) sum
						/ runs);
			} else {
				ErrorMessage.showErrorMessage(
						"The diameter could not be computed", "Warning");				
			}
		}
	}

	/*
	 * This inner class implements the task of computing the maximum (strongly)
	 * connected component of the currently opened graph by executing a series
	 * of depth-first search visits. The maximum (strongly) connected component
	 * is then stored into a file with the suffix '-mcc' added before the final
	 * extension '.nde'.
	 */
	private class MaximumConnectedComponentWorker extends ProgressTask {
		private String fn;

		public MaximumConnectedComponentWorker(String fn) {
			this.fn = fn;
		}

		public void execute() {
			logger.warn("Network file: " + openedFile.getName());
			logger.warn("Number of nodes: " + graph.getN());
			logger.warn("Number of edges: " + graph.getM());
			int[] r = graph.exportLargestCC(fn);
			if (r != null) {
				logger.warn("Number of connected components: " + r[0]);
				logger.warn("Size of maximum connected component: " + r[1]);
			} else {
				ErrorMessage.showErrorMessage(
						"Out of memory: try with greater stack space",
						"Warning");
			}
		}
	}

	/*
	 * This inner class implements the task of opening a graph stored into a NDE
	 * file. The format of the file is the following: the first line contains
	 * the number n of nodes and two flags that indicate whether the graph is
	 * directed and/or weighted, the next n lines contain, for each node, the
	 * node index, its out-degree and its in-degree (which is mandatory if the
	 * graph is directed), and finally the next lines contain, for each edge,
	 * the tail node index, the head node index, and the weight of the edge
	 * (which is mandatory if the graph is directed).
	 */
	private class OpenFileWorker extends ProgressTask {
		public void execute() {
			try {
				logger.info("File: " + currentFile);
				graph = new ArrayGraph();
				if (graph.readFile(currentFile.getAbsolutePath())) {
					setGraphDetails(graph);
				} else {
					graph = null;
				}
			} catch (OutOfMemoryError ome) {
				ErrorMessage
						.showErrorMessage(
								"Out of memory: try with greater heap space",
								"Warning");
			}
		}
	}

	static final String APP_TITLE = "LASAGNE";
	public static Logger logger = Logger.getLogger(Lasagne.class.getName());
	static Properties logProperties = new Properties();

	/*
	 * The program requires one argument which is the starting directory of the
	 * tree file navigator.
	 */
	public static void main(String[] args) {
		if (args.length > 0) {
			new Lasagne(args);
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					Lasagne app = new Lasagne();
					app.run();
				}
			});
		}
	}

	private JButton activeConsole;
	private JButton activeLog;
	private boolean cellSizesSet = false;
	private JTextArea console;
	private boolean consoleIsActive;
	private boolean logIsActive;
	private File currentFile;
	private JLabel diameter;
	private JLabel edges;
	private JLabel fileName;
	private FileSystemView fileSystemView;
	private FileTableModel fileTableModel;
	private ArrayGraph graph;
	private JPanel gui;
	private JFrame lasagneFrame;
	private ListSelectionListener listSelectionListener;
	private JLabel nodes;
	private File openedFile;
	private JLabel oriented;
	private int runs;
	private JLabel size;
	private JTable table;
	private JTree tree;
	private DefaultTreeModel treeModel;

	private JLabel weighted;

	public Lasagne() {
		setGUILoggerConfiguration();
	}

	public Lasagne(String[] args) {
		runOnTerminal(args);
	}

	/*
	 * Activate the full console logger if LASAGNE is executed via the terminal.
	 */
	private void activateFullTerminalConsoleLoggerConfiguration() {
		logProperties.put("log4j.appender.CONSOLE.Threshold", "INFO");
		PropertyConfigurator.configure(logProperties);
	}

	/*
	 * Activate or deactivate the console logger if LASAGNE is executed via the
	 * GUI.
	 */
	private void changeGUIConsoleLoggerConfiguration() {
		ClassLoader cl = getClass().getClassLoader();
		if (consoleIsActive) {
			activeConsole.setIcon(new ImageIcon(cl
					.getResource("img/inactiveconsole.png")));
			consoleIsActive = false;
			logProperties.put("log4j.appender.TEXTAREA.Threshold", "WARN");
		} else {
			activeConsole.setIcon(new ImageIcon(cl
					.getResource("img/activeconsole.png")));
			consoleIsActive = true;
			logProperties.put("log4j.appender.TEXTAREA.Threshold", "INFO");
		}
		PropertyConfigurator.configure(logProperties);
	}

	/*
	 * Activate or deactivate the file logger.
	 */
	private void changeFileLoggerConfiguration() {
		ClassLoader cl = getClass().getClassLoader();
		if (logIsActive) {
			activeLog.setIcon(new ImageIcon(cl.getResource("img/nolog.png")));
			logIsActive = false;
			logger.setLevel(Level.OFF);
		} else {
			activeLog.setIcon(new ImageIcon(cl.getResource("img/log.png")));
			logIsActive = true;
			logger.setLevel(Level.INFO);
		}
	}

	/*
	 * This method the task of computing the distance distribution of a graph by
	 * executing a limited number of BFSes, when LASAGNE has been executed via
	 * the terminal.
	 */
	private void distDistOnTerminal(CommandLine cl) {
		String fn = cl.getOptionValues("dd")[0];
		if (!(new File(fn)).exists()) {
			logger.warn("File does not exist");
			System.exit(-1);
		}
		int k = Integer.parseInt(cl.getOptionValues("dd")[1]);
		if (k <= 0) {
			logger.warn("The factor k must be positive");
			System.exit(-1);
		}
		try {
			logger.info("File: " + fn);
			graph = new ArrayGraph();
			graph.readFile(fn);
			logger.warn("Network file: " + fn);
			logger.info("Number of nodes: " + graph.getN());
			logger.info("Number of edges: " + graph.getM());
			int u = (int) (Math.random() * graph.getN());
			int[] res = graph.visit(u).getFirstArray();
			int ub = 2 * res[ArrayUtils.getIndexOfMax(res)];
			logger.info("Upper bound on maximum distance: " + ub);
			int[] dd = new int[ub];
			for (int i = 0; i < ub; i++) {
				dd[i] = 0;
			}
			int runs = k * ((int) Math.log(graph.getN()));
			for (int i = 0; i < runs; i++) {
				u = (int) (Math.random() * graph.getN());
				logger.info("Sampled node " + u);
				res = graph.visit(u).getFirstArray();
				for (int v = 0; v < graph.getN(); v++) {
					if (res[v] > 0) {
						dd[res[v]]++;
					}
				}
			}
			logger.info("Starting normalization of distribution");
			long total = 0;
			for (int v = 0; v < ub; v++) {
				total += dd[v];
			}
			long curr = 0;
			double[] dres = new double[ub];
			for (int cx = 0; cx < ub; cx++) {
				curr += dd[cx] / total;
				dres[cx] = dd[cx] / (double) total;
				if (cx != 0 && curr == 1) {
					break;
				}
			}
			String out = "";
			for (int cx = 0; cx < ub; cx++) {
				out = out + dres[cx] + " ";
			}
			logger.warn("Distance distribution:");
			logger.warn(out);
		} catch (OutOfMemoryError ome) {
			System.out.println("Out of memory: try with greater heap space");
		}
	}

	/*
	 * Create the graphical user interface of LASAGNE. The GUI contains a file
	 * tree navigator on the left, a file table on the top right, a tool-bar on
	 * the middle right, and a console on the bottom right. The tree navigator
	 * starts from the directory specified as the first program argument, while
	 * the file table shows only files with extension '.nde'.
	 */
	public Container getGui() {
		if (gui == null) {
			ClassLoader cl = getClass().getClassLoader();
			gui = new JPanel(new BorderLayout(3, 3));
			gui.setBorder(new EmptyBorder(5, 5, 5, 5));
			fileSystemView = FileSystemView.getFileSystemView();
			JPanel detailView = new JPanel(new BorderLayout(3, 3));
			table = new JTable(new FileTableModel()) {
				protected JTableHeader createDefaultTableHeader() {
					return new JTableHeader(columnModel) {
						public String getToolTipText(MouseEvent e) {
							java.awt.Point p = e.getPoint();
							int index = columnModel.getColumnIndexAtX(p.x);
							int realIndex = columnModel.getColumn(index)
									.getModelIndex();
							return FileTableModel.columnToolTips[realIndex];
						}
					};
				}
			};
			table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			table.setAutoCreateRowSorter(false);
			table.setShowVerticalLines(false);
			listSelectionListener = new ListSelectionListener() {
				@Override
				public void valueChanged(ListSelectionEvent lse) {
					int row = table.getSelectionModel().getLeadSelectionIndex();
					setFileDetails(((FileTableModel) table.getModel())
							.getFile(row));
				}
			};
			table.getSelectionModel().addListSelectionListener(
					listSelectionListener);
			JScrollPane tableScroll = new JScrollPane(table);
			Dimension d = tableScroll.getPreferredSize();
			tableScroll.setPreferredSize(new Dimension((int) d.getWidth(),
					(int) d.getHeight() / 2));
			detailView.add(tableScroll, BorderLayout.CENTER);
			DefaultMutableTreeNode root = new DefaultMutableTreeNode();
			treeModel = new DefaultTreeModel(root);

			TreeSelectionListener treeSelectionListener = new TreeSelectionListener() {
				public void valueChanged(TreeSelectionEvent tse) {
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) tse
							.getPath().getLastPathComponent();
					showChildren(node);
					setFileDetails((File) node.getUserObject());
				}
			};
			File startingDir = new File("NETWORKS");
			if (!startingDir.exists()) {
				startingDir.mkdir();
			}
			File[] roots = new File[] { startingDir };
			for (File fileSystemRoot : roots) {
				DefaultMutableTreeNode node = new DefaultMutableTreeNode(
						fileSystemRoot);
				root.add(node);
				File[] files = fileSystemView.getFiles(fileSystemRoot, true);
				for (File file : files) {
					if (file.isDirectory()) {
						node.add(new DefaultMutableTreeNode(file));
					}
				}
			}
			tree = new JTree(treeModel);
			tree.setRootVisible(false);
			tree.addTreeSelectionListener(treeSelectionListener);
			tree.setCellRenderer(new FileTreeCellRenderer());
			tree.expandRow(0);
			JScrollPane treeScroll = new JScrollPane(tree);
			tree.setVisibleRowCount(15);
			Dimension preferredSize = treeScroll.getPreferredSize();
			Dimension widePreferred = new Dimension(200,
					(int) preferredSize.getHeight());
			treeScroll.setPreferredSize(widePreferred);
			JPanel fileMainDetails = new JPanel(new BorderLayout(4, 2));
			fileMainDetails.setBorder(new EmptyBorder(0, 6, 0, 6));
			JPanel fileDetailsLabels = new JPanel(new GridLayout(0, 1, 2, 2));
			fileMainDetails.add(fileDetailsLabels, BorderLayout.WEST);
			JPanel fileDetailsValues = new JPanel(new GridLayout(0, 1, 2, 2));
			fileMainDetails.add(fileDetailsValues, BorderLayout.CENTER);
			fileDetailsLabels
					.add(new JLabel("OPENED NETWORK", JLabel.TRAILING));
			fileDetailsValues.add(new JLabel());
			fileDetailsLabels
					.add(new JLabel("Graph file name", JLabel.TRAILING));
			fileName = new JLabel();
			fileDetailsValues.add(fileName);
			fileDetailsLabels
					.add(new JLabel("Graph file size", JLabel.TRAILING));
			size = new JLabel();
			fileDetailsValues.add(size);
			fileDetailsLabels.add(new JLabel("Nodes", JLabel.TRAILING));
			nodes = new JLabel();
			fileDetailsValues.add(nodes);
			fileDetailsLabels.add(new JLabel("Edges", JLabel.TRAILING));
			edges = new JLabel();
			fileDetailsValues.add(edges);
			fileDetailsLabels.add(new JLabel("Oriented", JLabel.TRAILING));
			oriented = new JLabel();
			fileDetailsValues.add(oriented);
			fileDetailsLabels.add(new JLabel("Weighted", JLabel.TRAILING));
			weighted = new JLabel();
			fileDetailsValues.add(weighted);
			fileDetailsLabels.add(new JLabel("Diameter", JLabel.TRAILING));
			diameter = new JLabel();
			fileDetailsValues.add(diameter);
			JToolBar toolBar = new JToolBar();
			toolBar.setFloatable(false);
			/*
			 * This button open a window containing some information about
			 * LASAGNE.
			 */
			JButton about = new JButton(new ImageIcon(
					cl.getResource("img/about.png")));
			about.setToolTipText("About LASAGNE");
			about.setMnemonic('b');
			about.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					new AboutDialog((JFrame) gui.getTopLevelAncestor());
				}
			});
			toolBar.add(about);
			/*
			 * This button allows the user to create a new sub-directory of the
			 * currently selected directory. If no directory is currently
			 * selected, then a warning message is shown.
			 */
			JButton newDir = new JButton(new ImageIcon(
					cl.getResource("img/newdir.png")));
			newDir.setToolTipText("Create new directory");
			newDir.setMnemonic('n');
			newDir.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					TreePath path = tree.getSelectionPath();
					if (path != null) {
						DefaultMutableTreeNode node = (DefaultMutableTreeNode) path
								.getLastPathComponent();
						String name = Input
								.getString("Name of the new directory");
						if (name != null && name.length() > 0) {
							File dir = new File(node.toString() + "/" + name);
							if (!dir.exists()) {
								try {
									dir.mkdir();
									int index = 0;
									@SuppressWarnings("rawtypes")
									Enumeration cn = node.children();
									while (cn.hasMoreElements()) {
										DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) cn
												.nextElement();
										if (dmtn.toString()
												.toUpperCase()
												.compareTo(
														(node.toString() + "/" + name)
																.toUpperCase()) < 0) {
											index = index + 1;
										} else {
											break;
										}
									}
									node.insert(
											new DefaultMutableTreeNode(dir),
											index);
									treeModel.reload(node);
									tree.repaint();
									gui.repaint();
								} catch (Exception e) {
									StringWriter sw = new StringWriter();
									PrintWriter pw = new PrintWriter(sw);
									e.printStackTrace(pw);
									Lasagne.logger
											.info("======= Java Exception: if you want you can report it\n"
													+ sw + "=======");
								}
							} else {
								ErrorMessage.showErrorMessage(
										"The directory already exists",
										"Warning");
							}
						}
					} else {
						ErrorMessage.showErrorMessage(
								"One directory has to be selected", "Warning");
					}
				}
			});
			toolBar.add(newDir);
			/*
			 * This button allows the user to download a NDE file from the web.
			 * The file can be either in NDE format or a zipped version of a
			 * file in NDE format. The URL of the file can be either written or
			 * pasted into a text area.
			 */
			JButton download = new JButton(new ImageIcon(
					cl.getResource("img/download.png")));
			download.setToolTipText("Download a NDE (possibly zipped) file from the web");
			download.setMnemonic('w');
			toolBar.add(download);
			download.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					TreePath path = tree.getSelectionPath();
					if (path != null) {
						String url = Input.getPastableString(
								(JFrame) gui.getTopLevelAncestor(),
								"Specify the URL of the file");
						if (url != null) {
							if (!url.endsWith(".nde") && !url.endsWith(".zip")) {
								ErrorMessage
										.showErrorMessage(
												"The file must be a NDE file (possibly zipped) ",
												"Warning");
							} else {
								try {
									URL u = new URL(url);
									URLConnection uc = u.openConnection();
									int contentLength = uc.getContentLength();
									DownloadFileWorker dfw = new DownloadFileWorker(
											uc, contentLength);
									new JProgressDialog(lasagneFrame,
											"Downloading NDE file", "Wait...",
											true, 0, contentLength / 10000, dfw);
								} catch (Exception e) {
									ErrorMessage.showErrorMessage(
											"The file could not be downloaded",
											"Warning");
								}
							}
						}
						showChildren((DefaultMutableTreeNode) path
								.getLastPathComponent());
						gui.repaint();
					} else {
						ErrorMessage.showErrorMessage(
								"One directory has to be selected", "Warning");
					}
				}
			});
			toolBar.add(download);
			/*
			 * This button allows the user to open the currently selected file.
			 * If no file is currently selected, then a warning message is
			 * shown.
			 */
			JButton openFile = new JButton(new ImageIcon(
					cl.getResource("img/open.png")));
			openFile.setToolTipText("Open the selected network file");
			openFile.setMnemonic('o');
			openFile.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					if (currentFile.exists() && currentFile.isFile()) {
						OpenFileWorker ofw = new OpenFileWorker();
						new JProgressDialog(lasagneFrame,
								"Opening current file", "Wait...", true, ofw);
					} else {
						ErrorMessage.showErrorMessage(
								"One existing file has to be selected",
								"Warning");
					}
				}
			});
			toolBar.add(openFile);
			/*
			 * This button allows the user to compute the maximum connected
			 * component of the currently opened file. If no file is currently
			 * opened, then a warning message is shown.
			 */
			JButton maxConnectedComponent = new JButton(new ImageIcon(
					cl.getResource("img/lcc.png")));
			maxConnectedComponent
					.setToolTipText("Compute the maximum strongly connected component of the opened network");
			maxConnectedComponent.setMnemonic('m');
			maxConnectedComponent.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					if (graph != null && graph.getN() > 0) {
						String f = openedFile.getAbsolutePath();
						int n = JOptionPane
								.showConfirmDialog(
										null,
										"A new file containing the MCC will be created. Continue?",
										"Confirmation Question",
										JOptionPane.YES_NO_OPTION);
						if (n == 0 && f.endsWith(".nde")) {
							String nf = f.substring(0, f.lastIndexOf(".nde"));
							nf = nf + "-mcc.nde";
							MaximumConnectedComponentWorker mccw = new MaximumConnectedComponentWorker(
									nf);
							new JProgressDialog(lasagneFrame, "Computing MCC",
									"Wait...", true, mccw);
							showChildren((DefaultMutableTreeNode) tree
									.getSelectionPath().getLastPathComponent());
							gui.repaint();
						}
					} else {
						ErrorMessage
								.showErrorMessage(
										"One non-empty network file has to be opened first",
										"Warning");
					}

				}
			});
			toolBar.add(maxConnectedComponent);
			/*
			 * This button allows the user to delete the currently selected
			 * file. If no file is currently selected, then a warning message is
			 * shown.
			 */
			JButton deleteFile = new JButton(new ImageIcon(
					cl.getResource("img/delete.png")));
			deleteFile.setToolTipText("Delete the selected network file");
			deleteFile.setMnemonic('t');
			deleteFile.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					if (currentFile.exists() && currentFile.isFile()) {
						int n = JOptionPane.showConfirmDialog(null,
								"Are you sure you want to delete the file "
										+ currentFile.getName() + "?",
								"Confirmation Question",
								JOptionPane.YES_NO_OPTION);
						if (openedFile != null
								&& openedFile.equals(currentFile)) {
							resetGraphDetails();
							openedFile = null;
						}
						if (n == 0) {
							delete(currentFile);
							logger.warn(currentFile.getAbsolutePath()
									+ " has been deleted");
							currentFile = null;
						}
					} else {
						ErrorMessage.showErrorMessage(
								"One existing file has to be selected",
								"Warning");
					}
					showChildren((DefaultMutableTreeNode) tree
							.getSelectionPath().getLastPathComponent());
					gui.repaint();
				}
			});
			toolBar.add(deleteFile);
			/*
			 * This button allows the user to delete the currently selected
			 * directory (and all its contents). If no directory is currently
			 * selected or if the directory is the root, then a warning message
			 * is shown.
			 */
			JButton deleteDir = new JButton(new ImageIcon(
					cl.getResource("img/deldir.png")));
			deleteDir.setToolTipText("Delete the current directory");
			deleteDir.setMnemonic('t');
			deleteDir.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					try {
						DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree
								.getSelectionPath().getLastPathComponent();
						if (node != null && !node.toString().equals("NETWORKS")) {
							File dir = new File(node.toString());
							int n = JOptionPane
									.showConfirmDialog(
											null,
											"Are you sure you want to delete the current directory with all its contents?",
											"Confirmation Question",
											JOptionPane.YES_NO_OPTION);
							if (n == 0) {
								delete(dir);
								logger.warn(currentFile.getAbsolutePath()
										+ " has been deleted");
								resetGraphDetails();
								currentFile = null;
								openedFile = null;
								DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node
										.getParent();
								treeModel.removeNodeFromParent(node);
								showChildren(parentNode);
								gui.repaint();
							}
						} else {
							ErrorMessage
									.showErrorMessage(
											"One non root directory has to be selected",
											"Warning");
						}
					} catch (Exception e) {
						ErrorMessage.showErrorMessage(
								"One directory has to be selected", "Warning");

					}
				}
			});
			toolBar.add(deleteDir);
			/*
			 * This button allows the user to compute the diameter of the
			 * currently opened graph. If no graph is currently opened, then a
			 * warning message is shown. The diameter is computed by executing a
			 * BFS/Dijkstra starting from each node of the graph.
			 */
			JButton computeDiameter = new JButton(new ImageIcon(
					cl.getResource("img/delta.png")));
			computeDiameter
					.setToolTipText("Compute the diameter of the opened network via BFS/Dijkstra from each node");
			computeDiameter.setMnemonic('d');
			computeDiameter.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					if (graph != null && graph.getN() > 0) {
						DiameterWorker dw = new DiameterWorker();
						new JProgressDialog(lasagneFrame, "Computing diameter",
								"Percentage of BFSes/Dijkstras done", true, 0,
								graph.getN(), dw);
					} else {
						ErrorMessage
								.showErrorMessage(
										"One non-empty network file has to be opened first",
										"Warning");
					}
					gui.repaint();
				}
			});
			toolBar.add(computeDiameter);
			/*
			 * This button allows the user to compute a lower bound of the
			 * diameter of the currently opened graph. If no graph is currently
			 * opened, then a warning message is shown. The lower bound is
			 * computed by executing a the appropriate version of the 4-sweep
			 * method. The user can specify how many times this method has to be
			 * executed: the largest lower bound will then be reported.
			 */
			JButton fourSweep = new JButton(new ImageIcon(
					cl.getResource("img/foursweep.png")));
			fourSweep
					.setToolTipText("Compute a lower bound for the diameter of the opened network via 4-sweep");
			fourSweep.setMnemonic('f');
			fourSweep.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					if (graph != null && graph.getN() > 0) {
						runs = Input.getInt("How many runs?");
						if (runs > 0) {
							FourSweepWorker fsw = new FourSweepWorker();
							new JProgressDialog(lasagneFrame,
									"Running 4-sweep", "Lower bound is 0",
									true, 0, runs, fsw);
						}
					} else {
						ErrorMessage
								.showErrorMessage(
										"One non-empty network file has to be opened first",
										"Warning");
					}
					gui.repaint();
				}
			});
			toolBar.add(fourSweep);
			/*
			 * This button allows the user to compute the diameter of the
			 * currently opened graph. If no graph is currently opened, then a
			 * warning message is shown. The diameter is computed by executing
			 * the appropriate version of the iFUB method. The user can specify
			 * how many times this method has to be executed: the average number
			 * of BFSes/Dijkstras executed will then be reported.
			 */
			JButton iFUB = new JButton(new ImageIcon(
					cl.getResource("img/ifub.png")));
			iFUB.setToolTipText("Compute the diameter of the opened network via iFUB");
			iFUB.setMnemonic('i');
			iFUB.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					if (graph != null && graph.getN() > 0) {
						runs = Input.getInt("How many runs?");
						if (runs > 0) {
							IFUBWorker ifub = new IFUBWorker();
							new JProgressDialog(lasagneFrame, "Running iFUB",
									"Executing " + runs + " runs", true, 0,
									runs, ifub);
						}
					} else {
						ErrorMessage
								.showErrorMessage(
										"One non-empty network file has to be opened first",
										"Warning");
					}
					gui.repaint();
				}
			});
			toolBar.add(iFUB);
			/*
			 * This button allows the user to compute the distance distribution
			 * of the currently opened graph, which has to be undirected and
			 * un-weighted. If no such graph is currently opened, then a warning
			 * message is shown. The distance distribution is computed by
			 * executing the sampling method. The user can specify the constant
			 * k of this method: the number of sample nodes will be k times the
			 * logarithm of the number of nodes. The line chart of the distance
			 * distribution will then be shown.
			 */
			JButton distanceDistribution = new JButton(new ImageIcon(
					cl.getResource("img/distdist.png")));
			distanceDistribution
					.setToolTipText("Compute the distance distribution the opened network via sampling");
			distanceDistribution.setMnemonic('s');
			distanceDistribution.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					if (graph != null && graph.getN() > 0) {
						if (graph != null
								&& (graph.isOriented() || graph.isWeighted())) {
							ErrorMessage
									.showErrorMessage(
											"Not yet implemented for oriented and/or weighted graphs",
											"Warning");
						} else {
							runs = Input.getInt("What is the value of k?");
							runs = runs * ((int) Math.log(graph.getN()));
							if (runs > 0) {
								DistDistWorker ddw = new DistDistWorker();
								new JProgressDialog(lasagneFrame,
										"Computing distribution", "Executing "
												+ runs + " runs", true, 0,
										runs, ddw);
							}
						}
					} else {
						ErrorMessage
								.showErrorMessage(
										"One non-empty network file has to be opened first",
										"Warning");
					}
				}
			});
			toolBar.add(distanceDistribution);
			/*
			 * The console shows a subset of the messages produced by the
			 * application. In particular, the WARN messages are shown, while
			 * the INFO messages are shown only if the full logging is
			 * activated. The logging can also be completely deactivated. These
			 * features are managed by the following three buttons.
			 */
			console = new JTextArea();
			TextAreaAppender.setTextArea(console);
			activeConsole = new JButton(new ImageIcon(
					cl.getResource("img/inactiveconsole.png")));
			consoleIsActive = false;
			activeConsole
					.setToolTipText("Activate/deactivate full log in the console");
			activeConsole.setMnemonic('a');
			activeConsole.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					changeGUIConsoleLoggerConfiguration();
				}
			});
			toolBar.add(activeConsole);
			JButton cleanConsole = new JButton(new ImageIcon(
					cl.getResource("img/eraser.png")));
			cleanConsole.setToolTipText("Clean the console");
			cleanConsole.setMnemonic('c');
			cleanConsole.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					int n = JOptionPane.showConfirmDialog(null,
							"Are you sure you want to clean the console?",
							"Confirmation Question", JOptionPane.YES_NO_OPTION);
					if (n == 0) {
						console.setText("");
					}
				}
			});
			toolBar.add(cleanConsole);
			activeLog = new JButton(
					new ImageIcon(cl.getResource("img/log.png")));
			logIsActive = true;
			activeLog.setToolTipText("Activate/deactivate log in file");
			activeLog.setMnemonic('l');
			activeLog.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					changeFileLoggerConfiguration();
				}
			});
			toolBar.add(activeLog);
			int count = fileDetailsLabels.getComponentCount();
			for (int ii = 0; ii < count; ii++) {
				fileDetailsLabels.getComponent(ii).setEnabled(false);
			}
			JPanel fileView = new JPanel(new BorderLayout(3, 3));
			fileView.add(toolBar, BorderLayout.NORTH);
			fileView.add(fileMainDetails, BorderLayout.CENTER);
			detailView.add(fileView, BorderLayout.SOUTH);
			JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
					treeScroll, detailView);
			gui.add(splitPane, BorderLayout.CENTER);
			JScrollPane consoleScroll = new JScrollPane(console);
			console.setRows(10);
			gui.add(consoleScroll, BorderLayout.SOUTH);
		}
		return gui;
	}

	/*
	 * Delete a file or a directory with all its contents.
	 */
	private void delete(File file) {
		if (file.isDirectory()) {
			if (file.list().length == 0) {
				file.delete();
			} else {
				String files[] = file.list();
				for (String temp : files) {
					File fileDelete = new File(file, temp);
					delete(fileDelete);
				}
				if (file.list().length == 0) {
					file.delete();
				}
			}
		} else {
			file.delete();
		}
	}

	/*
	 * This method the task of computing the diameter of a graph by executing a
	 * BFS or a Dijkstra visit from each node when LASAGNE has been executed via
	 * the terminal.
	 */
	private void iFUBOnTerminal(CommandLine cl) {
		String fn = cl.getOptionValues("ifub")[0];
		if (!(new File(fn)).exists()) {
			logger.warn("File does not exist");
			System.exit(-1);
		}
		int runs = Integer.parseInt(cl.getOptionValues("ifub")[1]);
		if (runs <= 0) {
			logger.warn("The number of runs must be positive");
			System.exit(-1);
		}
		try {
			logger.info("File: " + fn);
			graph = new ArrayGraph();
			graph.readFile(fn);
			logger.warn("Network file: " + fn);
			logger.warn("Number of nodes: " + graph.getN());
			logger.warn("Number of edges: " + graph.getM());
			long[][] d = new long[runs][];
			if (!graph.isWeighted()) {
				IFub sf = new IFub();
				for (int e = 0; e < runs; e++) {
					d[e] = sf.run(graph, 0);
					logger.warn("Executed run " + e);
				}
			} else {
				WeightedIFub wsf = new WeightedIFub();
				for (int e = 0; e < runs; e++) {
					d[e] = wsf.run(graph, 0);
					logger.warn("Executed run " + e);
				}
			}
			logger.warn("Diameter is " + d[0][0]);
			long sum = 0;
			for (int e = 0; e < runs; e++) {
				logger.info("Run " + e + ": " + d[e][2] + " BFSes/Dijkstras");
				sum = sum + d[e][2];
			}
			logger.warn("Average number of BFSes/Dijkstras: " + (float) sum
					/ runs);
		} catch (OutOfMemoryError ome) {
			System.out.println("Out of memory: try with greater heap space");
		}
	}

	/*
	 * Configure the logger if LASAGNE is invoked via a terminal. The logger
	 * appends every WARN message on the console.
	 */
	private void setTerminalLoggerConfiguration() {
		logProperties.put("log4j.rootLogger", "INFO, CONSOLE");
		logProperties.put("log4j.appender.CONSOLE",
				"org.apache.log4j.ConsoleAppender");
		logProperties.put("log4j.appender.CONSOLE.Threshold", "WARN");
		logProperties.put("log4j.appender.CONSOLE.layout",
				"org.apache.log4j.PatternLayout");
		logProperties.put("log4j.appender.CONSOLE.layout.ConversionPattern",
				"%m%n");
		PropertyConfigurator.configure(logProperties);
	}

	/*
	 * Configure the logger if LASAGNE is invoked via the GUI. Initially, the
	 * logger appends every INFO and WARN message in the file 'lasagne.log' and
	 * appends all the WARN messages in the console. If the full logging in the
	 * console is activated, then every INFO and WARN message is also appended
	 * in the console.
	 */
	private void setGUILoggerConfiguration() {
		logProperties.put("log4j.rootLogger", "INFO, FILE, TEXTAREA");
		logProperties.put("log4j.appender.FILE",
				"org.apache.log4j.FileAppender");
		logProperties.put("log4j.appender.FILE.File", "lasagne.log");
		logProperties.put("log4j.appender.FILE.layout",
				"org.apache.log4j.PatternLayout");
		logProperties.put("log4j.appender.FILE.layout.ConversionPattern",
				"%d{HH:mm:ss} - %m%n");
		logProperties.put("log4j.appender.TEXTAREA",
				"lasagne.gui.TextAreaAppender");
		logProperties.put("log4j.appender.TEXTAREA.Threshold", "WARN");
		logProperties.put("log4j.appender.TEXTAREA.layout",
				"org.apache.log4j.PatternLayout");
		logProperties.put("log4j.appender.TEXTAREA.layout.ConversionPattern",
				"%m%n");
		PropertyConfigurator.configure(logProperties);
	}

	/*
	 * Start method invoked by the main if LASAGNE is executed via the GUI.
	 */
	private void run() {
		lasagneFrame = new JFrame(APP_TITLE);
		lasagneFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		lasagneFrame.setSize(828, 600);
		lasagneFrame.setLocationRelativeTo(null);
		lasagneFrame.setContentPane(getGui());
		lasagneFrame.setLocationByPlatform(true);
		lasagneFrame.setMinimumSize(lasagneFrame.getSize());
		lasagneFrame.setVisible(true);
		showRootFile();
	}

	/*
	 * Start method invoked by the main if LASAGNE is executed via the terminal.
	 */
	@SuppressWarnings("static-access")
	private void runOnTerminal(String[] args) {
		setTerminalLoggerConfiguration();
		Options opts = new Options();
		opts.addOption("h", false, "Print the help message");
		opts.addOption("v", false, "Activate the full logging of the execution");
		Option ifub = OptionBuilder.withArgName("<file> <e>").hasArgs(2)
				.withValueSeparator()
				.withDescription("Execute iFUB on file k times").create("ifub");
		opts.addOption(ifub);
		Option dd = OptionBuilder.withArgName("<file> <k>").hasArgs(2)
				.withValueSeparator()
				.withDescription("Execute the EW method on file klog(n) times")
				.create("dd");
		opts.addOption(dd);
		BasicParser bp = new BasicParser();
		try {
			CommandLine cl = bp.parse(opts, args);
			if (cl.hasOption("h")) {
				HelpFormatter f = new HelpFormatter();
				f.printHelp("OptionsTip", opts);
			} else if (cl.hasOption("ifub")) {
				if (cl.hasOption("v")) {
					activateFullTerminalConsoleLoggerConfiguration();
				}
				iFUBOnTerminal(cl);
			} else if (cl.hasOption("dd")) {
				if (cl.hasOption("v")) {
					activateFullTerminalConsoleLoggerConfiguration();
				}
				distDistOnTerminal(cl);
			}
		} catch (UnrecognizedOptionException uoe) {
			HelpFormatter f = new HelpFormatter();
			f.printHelp("avalaible options", opts);

		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	/*
	 * Set the width of a column of the table of files.
	 */
	private void setColumnWidth(int column, int width) {
		TableColumn tableColumn = table.getColumnModel().getColumn(column);
		if (width < 0) {
			JLabel label = new JLabel((String) tableColumn.getHeaderValue());
			Dimension preferred = label.getPreferredSize();
			width = (int) preferred.getWidth() + 14;
		}
		tableColumn.setPreferredWidth(width);
		tableColumn.setMaxWidth(width);
		tableColumn.setMinWidth(width);
	}

	private void setDiameterDetails(long d) {
		diameter.setText(d + "");
		gui.repaint();
	}

	private void setFileDetails(File file) {
		currentFile = file;
		JFrame f = (JFrame) gui.getTopLevelAncestor();
		if (f != null) {
			f.setTitle(APP_TITLE + " :: "
					+ fileSystemView.getSystemDisplayName(file));
		}
		gui.repaint();
	}

	private void resetGraphDetails() {
		graph = null;
		fileName.setText("");
		size.setText("");
		nodes.setText("");
		edges.setText("");
		oriented.setText("");
		weighted.setText("");
		diameter.setText("");
		gui.repaint();
	}

	/*
	 * Set the information of the opened graph in the GUI panel.
	 */
	private void setGraphDetails(ArrayGraph graph) {
		openedFile = currentFile;
		fileName.setText(currentFile.getName());
		size.setText(currentFile.length() + " bytes");
		if (graph != null) {
			nodes.setText("" + graph.getN());
			edges.setText("" + graph.getM());
			oriented.setText("" + graph.isOriented());
			weighted.setText("" + graph.isWeighted());
		} else {
			nodes.setText("");
			edges.setText("");
		}
		diameter.setText("");
		gui.repaint();
	}

	/*
	 * Set the content and the width of the file table.
	 */
	private void setTableData(final File[] files) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if (fileTableModel == null) {
					fileTableModel = new FileTableModel();
					table.setModel(fileTableModel);
				}
				table.getSelectionModel().removeListSelectionListener(
						listSelectionListener);
				fileTableModel.setFiles(files);
				table.getSelectionModel().addListSelectionListener(
						listSelectionListener);
				if (!cellSizesSet) {
					setColumnWidth(0, 280);
					setColumnWidth(1, 100);
					setColumnWidth(2, 100);
					setColumnWidth(3, 80);
					setColumnWidth(4, 20);
					setColumnWidth(5, 20);
					cellSizesSet = true;
				}
			}
		});
	}

	/*
	 * Show children method of the file system tree. It shows the files
	 * contained in the selected node, if there are any.
	 */
	private void showChildren(final DefaultMutableTreeNode node) {
		tree.setEnabled(false);
		SwingWorker<Void, File> worker = new SwingWorker<Void, File>() {
			@Override
			public Void doInBackground() {
				File file = (File) node.getUserObject();
				if (file.isDirectory()) {
					File[] files = fileSystemView.getFiles(file, true);
					if (node.isLeaf()) {
						for (File child : files) {
							if (child.isDirectory()) {
								publish(child);
							}
						}
					}
					setTableData(files);
				}
				return null;
			}

			@Override
			protected void done() {
				tree.setEnabled(true);
			}

			@Override
			protected void process(List<File> chunks) {
				for (File child : chunks) {
					node.add(new DefaultMutableTreeNode(child));
				}
			}
		};
		worker.execute();
	}

	/*
	 * Show the root of the file tree.
	 */
	public void showRootFile() {
		tree.setSelectionInterval(0, 0);
	}
}