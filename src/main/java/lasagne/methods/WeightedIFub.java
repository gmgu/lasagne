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

package lasagne.methods;

import java.io.PrintWriter;
import java.io.StringWriter;

import lasagne.graphs.ArrayGraph;
import lasagne.gui.Lasagne;
import lasagne.utilities.ArrayUtils;
import lasagne.utilities.PairArray;

public class WeightedIFub {
	/*
	 * The Distance class contains three fields: index, dist, and forward. These
	 * fields store the forward or backward shortest distance 'dist' of node
	 * 'index' from the starting node.
	 */
	class Distance {
		int index;
		int dist;
		boolean forward;

		Distance(int i, int d) {
			index = i;
			dist = d;
			forward = true;
		}

		Distance(int i, int d, boolean f) {
			index = i;
			dist = d;
			forward = f;
		}
	}

	/*
	 * This is the distribution method used by the quicksort algorithm (see
	 * Crescenzi, Gambosi, Grossi, 'Strutture di Dati e Algoritmi')
	 */
	private int distribution(Distance[] dist, int left, int pivot, int right) {
		if (pivot != right) {
			swap(dist, pivot, right);
		}
		int i = left;
		int j = right - 1;
		while (i <= j) {
			while ((i <= j) && (dist[i].dist <= dist[right].dist)) {
				i = i + 1;
			}
			while ((i <= j) && (dist[j].dist >= dist[right].dist)) {
				j = j - 1;
			}
			if (i < j) {
				swap(dist, i, j);
			}
		}
		if (i != right) {
			swap(dist, i, right);
		}
		return i;
	}

	/*
	 * This is the quicksort algorithm (see Crescenzi, Gambosi, Grossi,
	 * 'Strutture di Dati e Algoritmi'). Each element of the array to be sorted
	 * is an instance of the Distance class, and the algorithm sorts with
	 * respect to the 'dist' field.
	 */
	private void quickSort(Distance[] dist, int left, int right) {
		if (left < right) {
			int pivot = left + (int) (Math.random() * (right - left + 1));
			pivot = distribution(dist, left, pivot, right);
			quickSort(dist, left, pivot - 1);
			quickSort(dist, pivot + 1, right);
		}

	}

	/*
	 * Array swap operation
	 */
	private void swap(Distance[] dist, int i, int j) {
		Distance tmp = dist[j];
		dist[j] = dist[i];
		dist[i] = tmp;
	}

	/*
	 * This is the main method of this class: it just invokes the correct iFUB
	 * method, depending on whether the graph is directed or not.
	 */
	public long[] run(ArrayGraph graph, int k) {
		if (graph.isOriented()) {
			return directed(graph, k);
		} else {
			return undirected(graph, k);
		}
	}

	/*
	 * This method merges two sorted arrays into one sorted array, similarly to
	 * what is done in the case of the merge sort algorithm (see Crescenzi,
	 * Gambosi, Grossi, 'Strutture di Dati e Algoritmi'). The elements of the
	 * arrays are instances of the Distance class: the array are sorted with
	 * respect to the 'dist' field.
	 */
	private Distance[] merge(Distance[] f, Distance[] b) {
		Distance[] r = new Distance[f.length + b.length];
		int i = 0;
		int j = 0;
		int k = 0;
		while (i < f.length && j < b.length) {
			if (f[i].dist < b[j].dist) {
				r[k++] = f[i++];
			} else {
				r[k++] = b[j++];
			}
		}
		for (; i < f.length; i++) {
			r[k++] = f[i];
		}
		for (; j < b.length; j++) {
			r[k++] = b[j];
		}
		return r;
	}

	/*
	 * In the directed case, the iFUB method is described in Crescenzi, Grossi,
	 * Lanzi, Marino, 'On Computing the Diameter of Real-World Directed
	 * (Weighted) Graphs' (to be presented at SEA 2012). The argument k is the
	 * desired absolute error (usually set to 0). The method returns the values
	 * of the lower and the upper bounds, the number of BFSes executed, and the
	 * execution time (in milliseconds).
	 */
	public long[] directed(ArrayGraph graph, int k) {
		try {
			long startTime = System.currentTimeMillis();
			int n = graph.getN();
			// Compute a lower bound via the 4-sweep algorithm
			long[] fs = FourSweep.run(graph);
			int l = (int) fs[0];
			int u = (int) fs[1];
			// We keep trace of the number of Dijkstras executed
			int vis = 4;
			// Run a Dijkstra from node u
			PairArray ru = graph.visit(u);
			vis++;
			// Identify forward eccentricity of u
			int[] resu = ru.getFirstArray();
			int a = ArrayUtils.getIndexOfMax(resu);
			int eccF = resu[a];
			// Create and sort in nondecreasing way array of forward distances
			// that
			// will be merged with the sorted backward distances (differently
			// from
			// the paper we admit repeated values)
			Distance[] fdist = new Distance[n];
			for (int i = 0; i < n; i++) {
				fdist[i] = new Distance(i, resu[i]);
			}
			quickSort(fdist, 0, n - 1);
			// Run a backward Dijkstra from node u
			PairArray bru = graph.backwardVisit(u);
			vis++;
			// Identify backward eccentricity of u
			int[] bresu = bru.getFirstArray();
			int ba = ArrayUtils.getIndexOfMax(bresu);
			int eccB = bresu[ba];
			// Create and sort in nondecreasing way array of backward distances
			// that
			// will be merged with the sorted forward distances (differently
			// from
			// the paper we admit repeated values)
			Distance[] bdist = new Distance[n];
			for (int i = 0; i < n; i++) {
				bdist[i] = new Distance(i, bresu[i], false);
			}
			quickSort(bdist, 0, n - 1);
			// Merge forward and backward distances from u obtaining an array
			// sorted
			// in non decreasing way (differently from the paper we admit
			// repeated
			// values)
			Distance[] dist = merge(fdist, bdist);
			// Set i equal to the index of the maximum distance
			int i = dist.length - 1;
			// Initialize lower bound lb
			int lb = l;
			if (eccB > lb) {
				lb = eccB;
			}
			if (lb < eccF) {
				lb = eccF;
			}
			// Initialize upper bound ub
			int ub = 2 * dist[i].dist;
			while ((ub - lb) > k) {
				int Biu = lb;
				int j = i;
				while (j >= 0 && dist[j].dist == dist[i].dist) {
					if (!dist[j].forward) {
						// Node j is in BdiB(u)
						int[] bfs = graph.visit(dist[j].index).getFirstArray();
						vis++;
						int eccj = bfs[ArrayUtils.getIndexOfMax(bfs)];
						// Update maximum found eccentricity if necessary
						if (eccj > Biu) {
							Biu = eccj;
						}
					} else {
						// Node j is in BdiF(u)
						int[] bfs = graph.backwardVisit(dist[j].index)
								.getFirstArray();
						vis++;
						int beccj = bfs[ArrayUtils.getIndexOfMax(bfs)];
						// Update maximum found eccentricity if necessary
						if (beccj > Biu) {
							Biu = beccj;
						}
					}
					j = j - 1;
					// Optimization: it is useless going on since we have found
					// a lower bound equal to the upper bound
					if (Biu == ub) {
						while (j >= 0 && dist[j].dist == dist[i].dist) {
							j = j - 1;
						}
						break;
					}
				}
				// If the current maximum eccentricity is greater than twice the
				// distance immediately smaller than the current one, then
				// we have found the diameter
				if (Biu > 2 * dist[j].dist) {
					ub = Biu;
					lb = Biu;
					break;
				} else {
					// Otherwise we update the lower and the upper bound
					lb = Biu;
					ub = 2 * dist[j].dist;
				}
				i = j;
			}
			long[] rst = new long[4];
			rst[0] = l;
			rst[1] = ub;
			rst[2] = vis;
			long endTime = System.currentTimeMillis();
			rst[3] = endTime - startTime;
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

	/*
	 * In the undirected case, the iFUB method is described in Crescenzi,
	 * Grossi, Habib, Lanzi, Marino, 'On Computing the Diameter of Real-World
	 * Undirected Graphs' (currently submitted to TCS). The argument k is the
	 * desired absolute error (usually set to 0). The method returns the values
	 * of the lower and the upper bounds, the number of BFSes executed, and the
	 * execution time (in milliseconds).
	 */
	public long[] undirected(ArrayGraph graph, int k) {
		try {
			long startTime = System.currentTimeMillis();
			int n = graph.getN();
			// Compute lower bound l and starting node u via the 4-sweep method
			long[] fs = FourSweep.run(graph);
			int l = (int) fs[0];
			int u = (int) fs[1];
			// We keep trace of the number of Dijkstras executed
			int vis = 4;
			// Run a Dijkstra from node u: let a be the farthest node
			PairArray ru = graph.visit(u);
			vis++;
			int[] resu = ru.getFirstArray();
			// Create array of distances that will be sorted in nondecreasing
			// way
			// (differently from the paper we admit repeated values)
			Distance[] dist = new Distance[n];
			for (int i = 0; i < n; i++) {
				dist[i] = new Distance(i, resu[i]);
			}
			quickSort(dist, 0, n - 1);
			// Set i equal to the index of the greatest distance
			int i = n - 1;
			// Initialize lower bound
			int lb = l;
			if (dist[n - 1].dist > lb) {
				lb = dist[n - 1].dist;
			}
			// Initialize upper bound
			int ub = 2 * dist[n - 1].dist;
			while ((ub - lb) > k) {
				int Biu = lb;
				int j = i;
				while (j >= 0 && dist[j].dist == dist[i].dist) {
					// Node j is in Bdi(u)
					int[] bfs = graph.visit(dist[j].index).getFirstArray();
					vis++;
					int eccj = bfs[ArrayUtils.getIndexOfMax(bfs)];
					// Update maximum found eccentricity if necessary
					if (eccj > Biu) {
						Biu = eccj;
					}
					j = j - 1;
					// Optimization: it is useless going on since we have found
					// a lower bound equal to the upper bound
					if (Biu == ub) {
						while (j >= 0 && dist[j].dist == dist[i].dist) {
							j = j - 1;
						}
						break;
					}
				}
				// If the current maximum eccentricity is greater than twice the
				// distance immediately smaller than the current one, then
				// we have found the diameter
				if (Biu > 2 * dist[j].dist) {
					ub = Biu;
					lb = Biu;
					break;
				} else {
					// Otherwise we update the lower and the upper bound
					lb = Biu;
					ub = 2 * dist[j].dist;
				}
				i = j;
			}
			long[] rst = new long[4];
			rst[0] = lb;
			rst[1] = ub;
			rst[2] = vis;
			long endTime = System.currentTimeMillis();
			rst[3] = endTime - startTime;
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
}
