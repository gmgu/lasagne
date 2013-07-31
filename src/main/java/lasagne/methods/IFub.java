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

public class IFub {
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
			// Compute lower bound l and starting node u via the 4-sweep method
			long[] fs = FourSweep.run(graph);
			int l = (int) fs[0];
			int u = (int) fs[1];
			// We keep trace of the number of BFSes executed
			int vis = 4;
			// Compute the forward eccentricity of u
			PairArray ru = graph.visit(u);
			vis++;
			int[] resu = ru.getFirstArray();
			int a = ArrayUtils.getIndexOfMax(resu);
			int eccFu = resu[a];
			// Compute the backward eccentricity of u
			PairArray bru = graph.backwardVisit(u);
			vis++;
			int[] bresu = bru.getFirstArray();
			int ba = ArrayUtils.getIndexOfMax(bresu);
			int eccBu = bresu[ba];
			// Set i equal to the maximum between eccFu and eccBu
			int i = eccFu;
			if (eccBu > i) {
				i = eccBu;
			}
			// Set lb equal to the maximum between eccFu, eccBu, and l
			int lb = l;
			if (lb < i) {
				lb = i;
			}
			// Set ub equal to 2i
			int ub = 2 * i;
			while ((ub - lb) > k) {
				// Compute the maximum between the current lower bound and the
				// maximum eccentricity in BiB(u) and in BiF(u)
				int Biu = lb;
				for (int j = 0; j < n; j++) {
					if (bresu[j] == i) {
						// Node j is in BiB(u)
						int[] bfs = graph.visit(j).getFirstArray();
						vis++;
						int eccj = bfs[ArrayUtils.getIndexOfMax(bfs)];
						// Update maximum found eccentricity if necessary
						if (eccj > Biu) {
							Biu = eccj;
						}
						// Optimization: it is useless going on since we have
						// found
						// a lower bound equal to the upper bound
						if (Biu == ub) {
							break;
						}
					}
				}
				// Optimization: we explore the forward fringe only if necessary
				if (Biu < ub) {
					for (int j = 0; j < n; j++) {
						// Node j is in BiF(u)
						if (resu[j] == i) {
							int[] bfs = graph.backwardVisit(j).getFirstArray();
							vis++;
							int beccj = bfs[ArrayUtils.getIndexOfMax(bfs)];
							// Update maximum found eccentricity if necessary
							if (beccj > Biu) {
								Biu = beccj;
							}
						}
						// Optimization: it is useless going on since we have
						// found
						// a lower bound equal to the upper bound
						if (Biu == ub) {
							break;
						}
					}
				}
				if (Biu > 2 * (i - 1)) {
					ub = Biu;
					lb = Biu;
					break;
				} else {
					lb = Biu;
					ub = 2 * (i - 1);
				}
				i = i - 1;
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
			long l = fs[0];
			int u = (int) fs[1];
			// We keep trace of the number of BFSes executed
			int vis = 4;
			// Run a BFS from node u: let a be the farthest node
			PairArray ru = graph.visit(u);
			int[] resu = ru.getFirstArray();
			vis++;
			int a = ArrayUtils.getIndexOfMax(resu);
			// Set i equal to the eccentricity of u
			int i = resu[a];
			// Set lower bound
			int lb = (int) l;
			if (resu[a] > lb) {
				lb = resu[a];
			}
			// Set upper bound
			int ub = 2 * resu[a];
			while ((ub - lb) > k) {
				// Compute the maximum between the current lower bound and the
				// maximum eccentricity in Bi(u)
				int Biu = lb;
				for (int j = 0; j < n; j++) {
					if (resu[j] == i) {
						// Node j is in Bi(u)
						int[] bfs = graph.visit(j).getFirstArray();
						vis++;
						int eccj = bfs[ArrayUtils.getIndexOfMax(bfs)];
						// Update maximum found eccentricity if necessary
						if (eccj > Biu) {
							Biu = eccj;
						}
						// Optimization: it is useless going on since we have
						// found
						// a lower bound equal to the upper bound
						if (Biu == ub) {
							break;
						}
					}
				}
				// If the current maximum eccentricity is greater than 2(i-1),
				// then
				// we have found the diameter
				if (Biu > 2 * (i - 1)) {
					ub = Biu;
					lb = Biu;
					break;
				} else {
					// Otherwise we update the lower and the upper bound
					lb = Biu;
					ub = 2 * (i - 1);
				}
				i = i - 1;
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
