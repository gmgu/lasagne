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

public class FourSweep {
	/*
	 * This is the main method of this class: it just invokes the correct
	 * 4-sweep method, depending on whether the graph is directed or not.
	 */
	public static long[] run(ArrayGraph graph) {
		if (graph.isOriented()) {
			return directed(graph);
		} else {
			return undirected(graph);
		}

	}

	/*
	 * In the undirected case, the 4-sweep executes four BFSes/Dijkstras as
	 * described in Crescenzi, Grossi, Habib, Lanzi, Marino, 'On Computing the
	 * Diameter of Real-World Undirected Graphs' (currently submitted to TCS).
	 */
	private static long[] undirected(ArrayGraph graph) {
		try {
			long startTime = System.currentTimeMillis();
			int n = graph.getN();
			int lowerb = 0, u = -1;
			Lasagne.logger.info("4-Sweep invoked on undirected graph with " + n
					+ " nodes and " + graph.getM() + " edges.");
			// Run a BFS/Dijkstra from a random node r1: let a1 be the farthest
			// node
			int r1 = (int) (Math.random() * n);
			int[] resu = graph.visit(r1).getFirstArray();
			int a1 = ArrayUtils.getIndexOfMax(resu);
			// Update lower bound
			if (lowerb < resu[a1]) {
				lowerb = resu[a1];
			}
			// Run a BFS/Dijkstra from a1: let b1 be the farthest node
			PairArray ru = graph.visit(a1);
			resu = ru.getFirstArray();
			int b1 = ArrayUtils.getIndexOfMax(resu);
			// Update lower bound
			if (resu[b1] > lowerb) {
				lowerb = resu[b1];
			}
			// Let r2 be the node in the middle of the path between a1 and b1
			int r2 = ArrayUtils.getMiddleNode(b1, ru);
			// Run a BFS/Dijkstra from r2: let a2 be the farthest node
			ru = graph.visit(r2);
			resu = ru.getFirstArray();
			int a2 = ArrayUtils.getIndexOfMax(resu);
			// Update lower bound
			if (resu[a2] > lowerb) {
				lowerb = resu[a2];
			}
			// Run a BFS/Dijkstra from a2: let b2 be the farthest node
			ru = graph.visit(a2);
			resu = ru.getFirstArray();
			int b2 = ArrayUtils.getIndexOfMax(resu);
			// Update lower bound
			if (resu[b2] > lowerb) {
				lowerb = resu[b2];
			}
			// Let u be the node in the middle of the path between a2 and b2
			u = ArrayUtils.getMiddleNode(b2, ru);
			long endTime = System.currentTimeMillis();
			Lasagne.logger.info("Execution time: " + (endTime - startTime));
			return new long[] { lowerb, u, a2, b2 };
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
	 * In the directed case, the 4-sweep executes four BFSes as described in
	 * Crescenzi, Grossi, Lanzi, Marino, 'On Computing the Diameter of
	 * Real-World Directed (Weighted) Graphs' (to be presented at SEA 2012).
	 */
	private static long[] directed(ArrayGraph graph) {
		try {
			long startTime = System.currentTimeMillis();
			int n = graph.getN();
			Lasagne.logger.info("4-Sweep invoked on directed graph with " + n
					+ " nodes and " + graph.getM() + " edges.");
			// Run a forward BFS/Dijkstra from a random node r: let a1 be the
			// farthest node
			int r = (int) (Math.random() * n);
			int[] resu1 = graph.visit(r).getFirstArray();
			int a1 = ArrayUtils.getIndexOfMax(resu1);
			// Run a backward BFS/Dijkstra from a1: let b1 be the farthest node
			PairArray ru1 = graph.backwardVisit(a1);
			resu1 = ru1.getFirstArray();
			int b1 = ArrayUtils.getIndexOfMax(resu1);
			// Derive eccB(a1)
			int eccBa1 = resu1[b1];
			// Run a backward BFS/Dijkstra from r: let a2 be the farthest node
			int[] resu2 = graph.backwardVisit(r).getFirstArray();
			int a2 = ArrayUtils.getIndexOfMax(resu2);
			// Run a forward BFS/Dijkstra from a2: let b2 be the farthest node
			PairArray ru2 = graph.visit(a2);
			resu2 = ru2.getFirstArray();
			int b2 = ArrayUtils.getIndexOfMax(resu2);
			// Derive eccF(a2)
			int eccFa2 = resu2[b2];
			// If eccB(a1) > eccF(a2), then set u equal to the middle node
			// between
			// a1 and b1 and l equal to eccB(a1). Otherwise, set u equal to the
			// middle node between a2 and b2 and l equal to eccF (a2).
			if (eccBa1 > eccFa2) {
				int u = ArrayUtils.getMiddleNode(b1, ru1);
				int l = eccBa1;
				long endTime = System.currentTimeMillis();
				Lasagne.logger.info("Execution time: " + (endTime - startTime));
				return new long[] { l, u };
			} else {
				int u = ArrayUtils.getMiddleNode(b2, ru2);
				int l = eccFa2;
				long endTime = System.currentTimeMillis();
				Lasagne.logger.info("Execution time: " + (endTime - startTime));
				return new long[] { l, u };
			}
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
