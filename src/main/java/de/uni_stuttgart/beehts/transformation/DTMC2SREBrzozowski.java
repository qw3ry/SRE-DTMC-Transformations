package de.uni_stuttgart.beehts.transformation;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.uni_stuttgart.beehts.model.*;
import de.uni_stuttgart.beehts.model.DTMC.*;
import de.uni_stuttgart.beehts.model.SRE.SREAtomic;
import de.uni_stuttgart.beehts.model.construction.SREBuilder;

public class DTMC2SREBrzozowski implements Transformer<DTMC, SRE> {

	private DTMC dtmc;
	private SRE sre;

	/**
	 * Constructor.
	 * 
	 * @param dtmc
	 *            the dtmc to transform
	 */
	public DTMC2SREBrzozowski(DTMC dtmc) {
		this.dtmc = dtmc.clone();
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
	 * @return the transformed SRE.
	 */
	@Override
	public SRE getTransformed() {
		return sre;
	}

	@Override
	public void transform() {
		// algorithm similar to http://cs.stackexchange.com/a/2392
		Map<Node, Tuple<SRE, Double>> b = new HashMap<>();
		Map<Edge, SRE> a = new HashMap<>();

		for (Node n : dtmc.getFinalNodes()) {
			b.put(n, new Tuple<>(SREAtomic.EPSILON(), 1.));
		}
		for (Edge e : dtmc.getEdges()) {
			a.put(e, SREBuilder.atomic(e.character));
		}

		List<Node> toProcess = new LinkedList<>(dtmc.getNodes());
		toProcess.remove(dtmc.getInitialNode());
		toProcess.add(dtmc.getInitialNode());

		for (Node n : toProcess) {
			handleLoops(n, a, b);
			eliminateState(n, a, b);
		}
		this.sre = b.getOrDefault(dtmc.getInitialNode(), new Tuple<SRE, Double>(SREBuilder.parse("a*1"), 0.0)).x;
	}

	@Override
	public Delta<SRE> applyDelta(Delta<DTMC> delta) {
		throw new UnsupportedOperationException();
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
				// it is probably necessary to add an edge here
				// e = new Edge(e1.start, e2.end)
				Edge e = dtmc.addEdge(e1.from, e2.to, e1.character + e2.character,
						e1.getProbability() * e2.getProbability());
				// a[e] = a[e1]:a[e2]
				a.put(e, SREBuilder.concat(a.get(e1), a.get(e2)));
			}
		}
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
				loopSREs[i] = new Tuple<SRE, Integer>(a.get(loop), (int) (Math.pow(2, 30) * loop.getProbability()));
				loopProbability += loop.getProbability();
				i++;
			}
			SRE loopSRE = SREBuilder.kleene(SREBuilder.sum(loopSREs), loopProbability);
			// b[n] = loop* : b[n]
			if (b.containsKey(n)) {
				b.get(n).x = SREBuilder.concat(loopSRE, b.get(n).x);
			}
			for (Edge e : dtmc.getOutgoingEdges(n)) {
				// a[e] = loop* : a[e]
				a.put(e, SREBuilder.concat(loopSRE, a.get(e)));
			}
			dtmc.removeEdges(loops);
		}
	}
}
