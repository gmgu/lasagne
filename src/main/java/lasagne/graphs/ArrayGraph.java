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

package lasagne.graphs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;
import java.util.StringTokenizer;

import lasagne.gui.Lasagne;
import lasagne.utilities.ErrorMessage;
import lasagne.utilities.PairArray;

/*
 * This class implements the graph data structure by means of adjacency and
 * incidency lists. Each list is implemented as an array: for this reason, the
 * graph file format specifies for each node its in-degree and its out-degree.
 * The graph can be weighted: for this reason, each edge includes both its head
 * and its weight.
 */
public class ArrayGraph {
	class Edge {
		int head;
		int weight;

		Edge(int h, int w) {
			head = h;
			weight = w;
		}
	}

	public class Element {
		public int id;
		public int weight;
	}

	/*
	 * This class is used by the Dijkstra algorithm implementation (see
	 * Crescenzi, Gambosi, Grossi, 'Strutture di Dati e Algoritmi')
	 */
	public class Heap {
		int heapSize;
		Element[] heapArray;
		int[] heapArrayPosition;

		public Heap(int n) {
			heapArray = new Element[n];
			heapSize = 0;
			heapArrayPosition = new int[n];
		}

		public void decreaseKey(int id, int weight) {
			int i = heapArrayPosition[id];
			heapArray[i].weight = weight;
			heapify(i);
		}

		public boolean isEmpty() {
			return heapSize == 0;
		}

		public void enqueue(Element e) {
			heapArray[heapSize] = e;
			heapArrayPosition[e.id] = heapSize;
			heapSize = heapSize + 1;
			heapify(heapSize - 1);
		}

		public Element dequeue() {
			Element minimum = heapArray[0];
			heapArray[0] = heapArray[heapSize - 1];
			heapSize = heapSize - 1;
			heapify(0);
			return minimum;
		}

		private void heapify(int i) {
			while (i > 0 && heapArray[i].weight < heapArray[father(i)].weight) {
				swap(i, father(i));
				i = father(i);
			}
			while (left(i) < heapSize && i != minimumFatherSons(i)) {
				int son = minimumFatherSons(i);
				swap(i, son);
				i = son;
			}
		}

		private int minimumFatherSons(int i) {
			int j = left(i);
			int k = j;
			if (k + 1 < heapSize) {
				k = k + 1;
			}
			if (heapArray[k].weight < heapArray[j].weight) {
				j = k;
			}
			if (heapArray[i].weight < heapArray[j].weight) {
				j = i;
			}
			return j;
		}

		private int left(int i) {
			return 2 * i + 1;
		}

		private int father(int i) {
			return (i - 1) / 2;
		}

		private void swap(int i, int j) {
			Element tmp = heapArray[i];
			heapArray[i] = heapArray[j];
			heapArray[j] = tmp;
			heapArrayPosition[heapArray[i].id] = i;
			heapArrayPosition[heapArray[j].id] = j;
		}
	}

	public Edge[][] adjacencyLists;
	public Edge[][] incidencyLists;
	private int n;
	private int m;
	private boolean isOriented;
	private boolean isWeighted;
	// These fields are used by some of the class methods for computing
	// topological properties of the graph
	private int[] component;
	private int[] dfsNumber;
	private boolean[] complete;
	private Stack<Integer> partial;
	private Stack<Integer> representative;

	private int lastComponent;

	private int counter;

	public ArrayGraph() {
		n = 0;
		m = 0;
	}

	private PairArray backwardBFS(int s) {
		try {
			Lasagne.logger.info("Starting backward visit from " + s + "...");
			Queue<Integer> queue = new LinkedList<Integer>();
			int[] dist = new int[n];
			int[] pred = new int[n];
			for (int i = 0; i < n; i++) {
				dist[i] = -1;
			}
			int ecc = 0;
			queue.add(s);
			dist[s] = 0;
			pred[s] = -1;
			while (!queue.isEmpty()) {
				int u = queue.poll();
				for (int j = 1; j <= incidencyLists[u][0].weight; j++) {
					int v = incidencyLists[u][j].head;
					if (dist[v] == -1) {
						dist[v] = dist[u] + 1;
						pred[v] = u;
						ecc = Math.max(ecc, dist[v]);
						queue.add(v);
					}
				}
			}
			return new PairArray(dist, pred);
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			Lasagne.logger
					.info("======= Java Exception: if you want you can report it\n"
							+ sw + "=======");
			return null;
		}
	}

	private PairArray backwardDijkstra(int s) {
		try {
			Heap pq = new Heap(n);
			int[] dist = new int[n];
			int[] pred = new int[n];
			for (int u = 0; u < n; u = u + 1) {
				dist[u] = Integer.MAX_VALUE;
				pred[u] = -1;
			}
			dist[s] = 0;
			pred[s] = s;
			for (int i = 0; i < n; i = i + 1) {
				Element e = new Element();
				e.id = i;
				e.weight = dist[i];
				pq.enqueue(e);
			}
			while (!pq.isEmpty()) {
				Element e = pq.dequeue();
				int v = e.id;
				for (int j = 1; j <= incidencyLists[v][0].weight; j++) {
					int u = incidencyLists[v][j].head;
					int w = incidencyLists[v][j].weight;
					if (dist[u] > dist[v] + w) {
						dist[u] = dist[v] + w;
						pred[u] = v;
						pq.decreaseKey(u, dist[u]);
					}
				}
			}
			return new PairArray(dist, pred);
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			Lasagne.logger
					.info("======= Java Exception: if you want you can report it\n"
							+ sw + "=======");
			return null;
		}
	}

	public PairArray backwardVisit(int s) {
		if (isWeighted) {
			return backwardDijkstra(s);
		} else {
			return backwardBFS(s);
		}
	}

	private PairArray BFS(int s) {
		try {
			Lasagne.logger.info("Starting visit from " + s + "...");
			Queue<Integer> queue = new LinkedList<Integer>();
			int[] dist = new int[n];
			int[] pred = new int[n];
			for (int i = 0; i < n; i++) {
				dist[i] = -1;
			}
			int ecc = 0;
			queue.add(s);
			dist[s] = 0;
			pred[s] = -1;
			while (!queue.isEmpty()) {
				int u = queue.poll();
				for (int j = 1; j <= adjacencyLists[u][0].weight; j++) {
					int v = adjacencyLists[u][j].head;
					if (dist[v] == -1) {
						dist[v] = dist[u] + 1;
						pred[v] = u;
						ecc = Math.max(ecc, dist[v]);
						queue.add(v);
					}
				}
			}
			return new PairArray(dist, pred);
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			Lasagne.logger
					.info("======= Java Exception: if you want you can report it\n"
							+ sw + "=======");
			return null;
		}
	}

	public void connectedComponents() {
		component = new int[n];
		lastComponent = 0;
		for (int u = 0; u < n; u++) {
			component[u] = -1;
		}
		for (int u = 0; u < n; u++) {
			if (component[u] == -1) {
				PairArray res = BFS(u);
				for (int j = 0; j < n; j++) {
					if (res.getFirstArray()[j] >= 0) {
						component[j] = lastComponent;
					}
				}
				lastComponent++;
			}
		}
	}

	private PairArray dijkstra(int s) {
		try {
			Heap pq = new Heap(n);
			int[] dist = new int[n];
			int[] pred = new int[n];
			for (int u = 0; u < n; u = u + 1) {
				dist[u] = Integer.MAX_VALUE;
				pred[u] = -1;
			}
			dist[s] = 0;
			pred[s] = s;
			for (int i = 0; i < n; i = i + 1) {
				Element e = new Element();
				e.id = i;
				e.weight = dist[i];
				pq.enqueue(e);
			}
			while (!pq.isEmpty()) {
				Element e = pq.dequeue();
				int v = e.id;
				for (int j = 1; j <= adjacencyLists[v][0].weight; j++) {
					int u = adjacencyLists[v][j].head;
					int w = adjacencyLists[v][j].weight;
					if (dist[u] > dist[v] + w) {
						dist[u] = dist[v] + w;
						pred[u] = v;
						pq.decreaseKey(u, dist[u]);
					}
				}
			}
			return new PairArray(dist, pred);
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			Lasagne.logger
					.info("======= Java Exception: if you want you can report it\n"
							+ sw + "=======");
			return null;
		}
	}

	public int[] exportLargestCC(String fn) {
		int[] rst = new int[2];
		if (isOriented) {
			stronglyConnectedComponents(false);
		} else {
			connectedComponents();
		}
		rst[0] = lastComponent;
		int[] componentSize = new int[lastComponent];
		for (int ci = 0; ci < lastComponent; ci++) {
			componentSize[ci] = 0;
		}
		for (int i = 0; i < n; i++) {
			componentSize[component[i]]++;
		}
		int maxci = 0, maxcc = componentSize[0];
		for (int ci = 1; ci < lastComponent; ci++) {
			if (componentSize[ci] > maxcc) {
				maxcc = componentSize[ci];
				maxci = ci;
			}
		}
		rst[1] = maxcc;
		for (int i = 0; i < n; i++) {
			if (component[i] != maxci) {
				component[i] = -1;
			} else {
				component[i] = 1;
			}
		}
		int[] map = new int[n];
		int mappedI = 0;
		for (int i = 0; i < n; i++) {
			if (component[i] != -1) {
				map[i] = mappedI;
				mappedI++;
			}
		}
		File outFile = new File(fn);
		if (outFile.getParent() == null) {
			ErrorMessage.showErrorMessage("The output file does not exist",
					"Warning");
			return null;
		}
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(outFile));
			if (isOriented && !isWeighted) {
				bw.write(mappedI + " 1 0\n");
			} else if (isOriented && isWeighted) {
				bw.write(mappedI + " 1 1\n");
			} else if (!isOriented && !isWeighted) {
				bw.write(mappedI + " 0 0\n");
			} else {
				bw.write(mappedI + " 0 1\n");
			}
			for (int i = 0; i < n; i++) {
				if (component[i] != -1) {
					int od = 0;
					for (int j = 1; j <= adjacencyLists[i][0].weight; j++) {
						if (component[adjacencyLists[i][j].head] != -1) {
							od = od + 1;
						}
					}
					if (isOriented) {
						int id = 0;
						for (int j = 1; j <= incidencyLists[i][0].weight; j++) {
							if (component[incidencyLists[i][j].head] != -1) {
								id = id + 1;
							}
						}
						bw.write(map[i] + " " + od + " " + id + "\n");
					} else {
						bw.write(map[i] + " " + od + "\n");
					}
				}
			}
			for (int i = 0; i < n; i++) {
				if (component[i] != -1)
					for (int j = 1; j <= adjacencyLists[i][0].weight; j++) {
						if (component[adjacencyLists[i][j].head] != -1) {
							if (isOriented) {
								bw.write(map[i] + " "
										+ map[adjacencyLists[i][j].head] + " "
										+ adjacencyLists[i][j].weight + "\n");
							} else {
								if (i < adjacencyLists[i][j].head) {
									bw.write(map[i] + " "
											+ map[adjacencyLists[i][j].head]
											+ " " + adjacencyLists[i][j].weight
											+ "\n");
								}
							}
						}
					}
			}
			bw.close();
			return rst;
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			Lasagne.logger
					.info("======= Java Exception: if you want you can report it\n"
							+ sw + "=======");
			return null;
		}
	}

	public void extendedRecursiveDFS(int u) {
		dfsNumber[u] = counter;
		counter = counter + 1;
		partial.push(u);
		representative.push(u);
		for (int j = 1; j <= adjacencyLists[u][0].weight; j++) {
			int v = adjacencyLists[u][j].head;
			if (dfsNumber[v] == -1) {
				extendedRecursiveDFS(v);
			} else if (!complete[v]) {
				while (dfsNumber[representative.peek()] > dfsNumber[v]) {
					representative.pop();
				}
			}
		}
		if (u == representative.peek()) {
			int z;
			do {
				z = partial.pop();
				component[z] = lastComponent;
				complete[z] = true;
			} while (z != u);
			representative.pop();
			lastComponent = lastComponent + 1;
		}
	}

	public int getM() {
		return m;
	}

	public int getN() {
		return n;
	}

	public boolean isOriented() {
		return isOriented;
	}

	public boolean isWeighted() {
		return isWeighted;
	}

	public boolean readFile(String fileName) {
		long startTime = System.currentTimeMillis();
		File inFile = new File(fileName);
		try {
			BufferedReader br = new BufferedReader(new FileReader(inFile));
			String line = br.readLine();
			StringTokenizer lineTokens = new StringTokenizer(line, " ");
			n = Integer.parseInt(lineTokens.nextToken());
			isOriented = false;
			isWeighted = false;
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
			Lasagne.logger.info("Graph is oriented: " + isOriented);
			Lasagne.logger.info("Graph is weighted: " + isWeighted);
			Lasagne.logger.info("Number of nodes: " + n);
			adjacencyLists = new Edge[n][];
			incidencyLists = new Edge[n][];
			m = 0;
			for (int i = 0; i < n; i++) {
				line = br.readLine();
				lineTokens = new StringTokenizer(line, " ");
				int u = Integer.parseInt(lineTokens.nextToken());
				int od = Integer.parseInt(lineTokens.nextToken());
				adjacencyLists[u] = new Edge[od + 1];
				adjacencyLists[u][0] = new Edge(u, 0);
				if (isOriented) {
					int id = Integer.parseInt(lineTokens.nextToken());
					incidencyLists[u] = new Edge[id + 1];
					incidencyLists[u][0] = new Edge(u, 0);
				}
				m = m + od;
			}
			if (!isOriented) {
				m = m / 2;
			}
			Lasagne.logger.info("Number of edges: " + m);
			line = br.readLine();
			while (line != null && line.length() > 0) {
				lineTokens = new StringTokenizer(line, " ");
				int s = Integer.parseInt(lineTokens.nextToken());
				int t = Integer.parseInt(lineTokens.nextToken());
				adjacencyLists[s][0].weight++;
				int w = 1;
				if (isWeighted) {
					w = Integer.parseInt(lineTokens.nextToken());
				}
				adjacencyLists[s][adjacencyLists[s][0].weight] = new Edge(t, w);
				if (!isOriented) {
					adjacencyLists[t][0].weight++;
					adjacencyLists[t][adjacencyLists[t][0].weight] = new Edge(
							s, w);
				} else {
					incidencyLists[t][0].weight++;
					incidencyLists[t][incidencyLists[t][0].weight] = new Edge(
							s, w);
				}
				line = br.readLine();
			}
			br.close();
			long endTime = System.currentTimeMillis();
			Lasagne.logger.info("Execution time: "
					+ (int) (((endTime - startTime) / 1000.0) * 1000) / 1000.0
					+ " seconds");
			return true;
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			Lasagne.logger
					.info("======= Java Exception: if you want you can report it\n"
							+ sw + "=======");
			return false;
		}
	}

	public void stronglyConnectedComponents(boolean verb) {
		try {
			long startTime = System.currentTimeMillis();
			dfsNumber = new int[n];
			complete = new boolean[n];
			component = new int[n];
			for (int s = 0; s < n; s++) {
				dfsNumber[s] = -1;
				complete[s] = false;
				component[s] = -1;
			}
			partial = new Stack<Integer>();
			representative = new Stack<Integer>();
			lastComponent = 0;
			counter = 0;
			for (int s = 0; s < n; s++) {
				if (dfsNumber[s] == -1) {
					extendedRecursiveDFS(s);
				}
			}
			long endTime = System.currentTimeMillis();
			Lasagne.logger.info("Number of SCCs: " + lastComponent);
			Lasagne.logger.info("Execution time: " + (endTime - startTime));
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			Lasagne.logger
					.info("======= Java Exception: if you want you can report it\n"
							+ sw + "=======");
		}
	}

	public PairArray visit(int s) {
		if (isWeighted) {
			return dijkstra(s);
		} else {
			return BFS(s);
		}
	}
}
