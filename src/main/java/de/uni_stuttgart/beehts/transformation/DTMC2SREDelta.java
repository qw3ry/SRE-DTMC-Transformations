package de.uni_stuttgart.beehts.transformation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.uni_stuttgart.beehts.model.DTMC;
import de.uni_stuttgart.beehts.model.DTMC.Node;
import de.uni_stuttgart.beehts.model.Delta;
import de.uni_stuttgart.beehts.model.SRE;
import de.uni_stuttgart.beehts.model.SRE.SREAtomic;
import de.uni_stuttgart.beehts.model.SRE.SREKleene;
import de.uni_stuttgart.beehts.model.Tuple;
import de.uni_stuttgart.beehts.model.construction.SREBuilder;

@Deprecated
public class DTMC2SREDelta implements Transformer<DTMC, SRE> {

	private DTMC original;
	private SRE transformed = null;

	private Map<Tuple<Node, Node>, SRE> a = new HashMap<>();
	private Map<Tuple<Node, Node>, Double> p = new HashMap<>();

	public DTMC2SREDelta(DTMC original) {
		this.original = original;
		transform();
	}

	@Override
	public void transform() {
		Map<Tuple<Node, Node>, SRE> aTmp = new HashMap<>();
		Map<Tuple<Node, Node>, PRef> pTmp = new HashMap<>();
		Map<Node, SRE> b = new HashMap<>();

		// 1. : init
		initAlgorithm(aTmp, pTmp, b);

		// 2. : do the alg
		transform(aTmp, pTmp, b);

		// 3. : remove the refs as well as unnecessary parts and return
		transformed = cleanUp(b.get(original.getInitialNode()));
	}

	/**
	 * Perform the initialization for the algorithm. aTmp and bTmp are
	 * initialized and supposed to use to store intermediate SREs. In pTmp the
	 * probabilities for the intermediate SREs for an aTmp are stored.
	 * Furthermore, the class members a and p are filled.
	 */
	private void initAlgorithm(Map<Tuple<Node, Node>, SRE> aTmp, Map<Tuple<Node, Node>, PRef> pTmp,
			Map<Node, SRE> bTmp) {
		for (Node n1 : original.getNodes()) {
			for (Node n2 : original.getNodes()) {
				Tuple<Node, Node> t = new Tuple<>(n1, n2);
				switch (original.getEdges(n1, n2).size()) {
				case 0:
					break;
				case 1:
					a.put(t, SREBuilder.atomic(original.getEdges(n1, n2).iterator().next().character));
					break;
				default:
					@SuppressWarnings("unchecked")
					Tuple<SRE, Integer>[] tmp = original.getEdges(n1, n2).stream()
							.map(e -> new Tuple<>(SREBuilder.atomic(e.character),
									(int) (Math.pow(2, 30) * e.getProbability())))
							.toArray(Tuple[]::new);
					a.put(t, SREBuilder.sum(tmp));
					break;
				}
				p.put(t, original.getEdges(n1, n2).stream().mapToDouble(e -> e.getProbability()).sum());
				aTmp.put(t, new SREAtomicRef(n1, n2));
				pTmp.put(t, new PRef(n1, n2));
			}
		}
		for (Node n : original.getFinalNodes()) {
			bTmp.put(n, SREBuilder.atomic(""));
		}
	}

	private void transform(Map<Tuple<Node, Node>, SRE> aTmp, Map<Tuple<Node, Node>, PRef> pTmp, Map<Node, SRE> b) {
		List<Node> nodes = new ArrayList<>(original.getNodes().size());
		for (Node n : original.getNodes()) {
			nodes.add(original.getInitialNode().equals(n) ? 0 : nodes.size(), n);
		}

		for (int i = nodes.size() - 1; i >= 0; i++) {
			// FIXME
			SRE loop = null;
			PRef loopP = null;
			b.put(nodes.get(i), SREBuilder.concat(new SREKleeneRef(loop, loopP), b.get(nodes.get(i))));
			for (int j = 0; j < i; j++) {
				//Tuple<Node, Node> key = new Tuple<>(nodes.get(i), nodes.get(j));
				//aTmp.put(key, SREBuilder.concat(new SREKleeneRef(loop.clone(), loopP)));
			}
			for (int j = 0; j < i; j++) {
				// Moar stuff
				for (int k = 0; k < i; k++) {
					// Still even moar stuff
				}
			}
		}
	}

	private SRE cleanUp(SRE sre) {
		// TODO
		return null;
	}

	@Override
	public DTMC getOriginal() {
		return original;
	}

	@Override
	public SRE getTransformed() {
		return transformed;
	}

	@Override
	public Delta<SRE> applyDelta(Delta<DTMC> delta) {
		// TODO
		throw new UnsupportedOperationException();
	}

	private class PRef {

		public final Node from;
		public final Node to;

		public PRef(Node from, Node to) {
			this.from = from;
			this.to = to;
		}
	}

	@SuppressWarnings("unused")
	private class SREAtomicRef extends SREAtomic {

		public final Node from;
		public final Node to;

		public SREAtomicRef(Node from, Node to) {
			super(a.get(new Tuple<>(from, to)).toString());
			this.from = from;
			this.to = to;
		}

	}

	@SuppressWarnings("unused")
	private class SREKleeneRef extends SREKleene {

		public final SRE sub;
		public final PRef pRef;

		public SREKleeneRef(SRE sre, PRef rate) {
			super(sre, p.get(new Tuple<>(rate.from, rate.to)));
			this.sub = sre;
			this.pRef = rate;
		}

	}
}
