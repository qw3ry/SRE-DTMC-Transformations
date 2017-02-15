package de.uni_stuttgart.beehts.transformation;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

import de.uni_stuttgart.beehts.model.*;
import de.uni_stuttgart.beehts.model.DTMC.*;
import de.uni_stuttgart.beehts.model.SRE.*;
import de.uni_stuttgart.beehts.model.construction.SREBuilder;

public class DTMC2SREDeltaBrz implements Transformer<DTMC, SRE> {

	private DTMC dtmc;
	private SRE sre;
	private Map<Edge, SRE> atomicSREs;
	private long edgeNumber = 0;

	/**
	 * Constructor.
	 * 
	 * @param dtmc
	 *            the dtmc to transform
	 */
	public DTMC2SREDeltaBrz(DTMC dtmc) {
		this.dtmc = dtmc;
		this.atomicSREs = new HashMap<>(dtmc.getEdges().size());
		transform();
	}

	/**
	 * Get the DTMC that is transformed.
	 * 
	 * @return the original DTMC
	 */
	@Override
	public DTMC getOriginal() {
		return dtmc;
	}

	/**
	 * Get the transformed SRE. It may be null prior to the transformation. It
	 * may not always be the result either, if the transformation is still
	 * running.
	 * 
	 * Additionally, please note that this SRE may contain identical subtrees
	 * (i.e. the same SRE object multiple times). If you want to do further
	 * processing it is necessary to clone the SRE before that. Furthermore it
	 * may occur that the obtained SRE is changed by the transformer if further
	 * processing (e.g. the application of a delta) is done.
	 * 
	 * @return the transformed SRE.
	 */
	@Override
	public SRE getTransformed() {
		return sre;
	}

	@Override
	public Delta<SRE> applyDelta(Delta<DTMC> delta) {
		if (!(delta instanceof DTMCDeltaResticted)) {
			throw new UnsupportedOperationException();
		}

		dtmc = delta.applyChanges(dtmc);

		Map<Edge, DTMC> inputDelta = ((DTMCDeltaResticted) delta).getChanges();
		SREDelta resultDelta = new SREDelta();

		for (Edge e : inputDelta.keySet()) {
			DTMC2SREDeltaBrz subTransformer = new DTMC2SREDeltaBrz(inputDelta.get(e));

			resultDelta.addChange(atomicSREs.get(e), subTransformer.getTransformed());
			sre = sre.traverse(new Traverser() {

				@Override
				protected SRE postOrder(SRE sre, OptionalInt weight) {
					if (sre instanceof SREAtomicRef) {
						if (((SREAtomicRef) sre).e.equals(e)) {
							return subTransformer.sre;
						}
					}
					return sre;
				}
			}).y;
			this.atomicSREs.putAll(subTransformer.atomicSREs);
		}

		return resultDelta;
	}

	@Override
	public void transform() {
		// algorithm similar to http://cs.stackexchange.com/a/2392
		Map<Node, Tuple<SRE, Double>> b = new HashMap<>();
		Map<Edge, SRE> a = new HashMap<>();
		Collection<Edge> toReAddAfter = new HashSet<>(dtmc.getEdges());

		for (Node n : dtmc.getFinalNodes()) {
			b.put(n, new Tuple<>(SREAtomic.EPSILON(), 1.));
		}
		for (Edge e : dtmc.getEdges()) {
			a.put(e, new SREAtomicRef(e));
			this.atomicSREs.put(e, SREBuilder.atomic(e.character));
		}

		List<Node> toProcess = new LinkedList<>(dtmc.getNodes());
		toProcess.remove(dtmc.getInitialNode());
		toProcess.add(dtmc.getInitialNode());

		for (Node n : toProcess) {
			for (Edge e : new HashSet<>(dtmc.getOutgoingEdges(n))) {
				mergeDuplicateEdges(n, e.to, a);
			}
		}

		for (Node n : toProcess) {
			handleLoops(n, a, b);
			eliminateState(n, a, b);
		}
		this.sre = b.getOrDefault(dtmc.getInitialNode(), new Tuple<SRE, Double>(SREBuilder.parse("a*1"), 0.0)).x;

		dtmc.removeEdges(dtmc.getEdges());
		toReAddAfter.forEach(e -> dtmc.addEdge(e));
	}

	private void eliminateState(Node n, Map<Edge, SRE> a, Map<Node, Tuple<SRE, Double>> b) {
		Collection<Edge> inc = dtmc.getIncomingEdges(n);
		Collection<Edge> out = dtmc.getOutgoingEdges(n);

		// for e1 in getIncoming(n) (beware of direct loops)
		for (Edge e1 : inc) {
			// b[e1.start] = b[e1.start] + (a[e1] : b[n])
			if (b.containsKey(n)) {
				SRE newSRE = SREBuilder.concat(a.get(e1), b.get(n).x);
				Double p = e1.getProbability() * b.get(n).y;
				if (b.containsKey(e1.from)) {
					SRE sum = SREBuilder.sum(new Tuple<>(newSRE, (int) (Math.pow(2, 30) * p)),
							new Tuple<>(b.get(e1.from).x, (int) (Math.pow(2, 30) * b.get(e1.from).y)));
					b.put(e1.from, new Tuple<>(sum, b.get(e1.from).y + p));
				} else {
					b.put(e1.from, new Tuple<>(newSRE, p));
				}
			}
			// for e2 in getOutgoing(n) (beware of loops again)
			for (Edge e2 : out) {
				// a[e] = a[e1]:a[e2]
				Edge e = dtmc.addEdge(e1.from, e2.to, getNewEdgeNumber(), e1.getProbability() * e2.getProbability());
				a.put(e, SREBuilder.concat(a.get(e1), a.get(e2)));
				mergeDuplicateEdges(e1.from, e2.to, a);
			}
		}
		for (Edge e : inc)
			a.remove(e);
		for (Edge e : out)
			a.remove(e);
		dtmc.removeEdges(inc);
		dtmc.removeEdges(out);
	}

	private void handleLoops(Node n, Map<Edge, SRE> a, Map<Node, Tuple<SRE, Double>> b) {
		Collection<Edge> loops = dtmc.getEdges(n, n);
		// put self loops together to one kleene operation
		if (!loops.isEmpty()) {
			@SuppressWarnings("unchecked")
			Tuple<SRE, Integer>[] loopSREs = new Tuple[loops.size()];
			double loopProbability = 0;
			int i = 0;
			for (Edge loop : loops) {
				loopSREs[i] = new Tuple<SRE, Integer>(a.remove(loop), (int) (Math.pow(2, 30) * loop.getProbability()));
				loopProbability += loop.getProbability();
				i++;
			}
			dtmc.removeEdges(loops);
			SRE loopSRE = SREBuilder.kleene(SREBuilder.sum(loopSREs), loopProbability);
			// b[n] = loop* : b[n]
			if (b.containsKey(n)) {
				b.get(n).x = SREBuilder.concat(loopSRE, b.get(n).x);
			}
			for (Edge e : dtmc.getOutgoingEdges(n)) {
				// a[e] = loop* : a[e]
				a.put(e, SREBuilder.concat(loopSRE, a.get(e)));
			}
		}
	}

	private void mergeDuplicateEdges(Node n1, Node n2, Map<Edge, SRE> a) {
		Collection<Edge> edges = dtmc.getEdges(n1, n2);
		if (edges.isEmpty())
			return;

		@SuppressWarnings("unchecked")
		Tuple<SRE, Integer>[] sres = new Tuple[edges.size()];
		double p = 0;
		int i = 0;
		for (Edge e : edges) {
			sres[i] = new Tuple<SRE, Integer>(a.remove(e), (int) (Math.pow(2, 30) * e.getProbability()));
			p += e.getProbability();
			i++;
		}
		SRE sumSRE = SREBuilder.sum(sres);

		dtmc.removeEdges(edges);
		Edge e = new Edge(n1, n2, getNewEdgeNumber(), p);
		a.put(e, sumSRE);
		dtmc.addEdge(e);
	}

	private String getNewEdgeNumber() {
		return "_" + edgeNumber++;
	}

	private class SREAtomicRef extends SREAtomic {

		public final Edge e;

		public SREAtomicRef(Edge e) {
			super(e.character);
			this.e = e;
		}

		@Override
		public String getCharacter() {
			return e.character;
		}
	}
}
