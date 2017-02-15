package de.uni_stuttgart.beehts.transformation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.uni_stuttgart.beehts.model.*;
import de.uni_stuttgart.beehts.model.DTMC.Edge;
import de.uni_stuttgart.beehts.model.DTMC.Node;
import de.uni_stuttgart.beehts.model.SRE.*;
import de.uni_stuttgart.beehts.util.CollectionHelpers;

public class SRE2DTMCDelta implements Transformer<SRE, DTMC> {

	private SRE sre;
	private DTMC dtmc = null;

	/** Maps sub-sres to the corresponding initial and final node */
	private Map<SRE, Tuple<Node, Set<Node>>> map = new HashMap<>();

	public SRE2DTMCDelta(SRE sre) {
		this.sre = sre;
		transform();
	}

	@Override
	public void transform() {
		dtmc = transform(sre);
	}

	@Override
	public SRE getOriginal() {
		return sre;
	}

	@Override
	public DTMC getTransformed() {
		return dtmc;
	}

	@Override
	public Delta<DTMC> applyDelta(Delta<SRE> delta) {
		if (!(delta instanceof SREDelta)) {
			throw new UnsupportedOperationException();
		} else {
			return applyDelta((SREDelta) delta);
		}
	}

	private Delta<DTMC> applyDelta(SREDelta delta) {
		Map<SRE, SRE> d = delta.getChanges();
		DTMCDelta result = new DTMCDelta();

		for (SRE key : d.keySet()) {
			transformDelta(key, d.get(key), result);
		}

		sre = delta.applyChanges(sre);

		return result;
	}

	/**
	 * Create a delta for the dtmc that exchanges the key for the sre.
	 * 
	 * @param key
	 *            the part to exchange.
	 * @param sre
	 *            the target.
	 * @param result
	 *            the delta (output parameter). Changes are added here.
	 */
	private void transformDelta(SRE key, SRE sre, DTMCDelta result) {
		Tuple<Node, Set<Node>> initialAndFinal = map.get(key);
		Set<Edge> incToOldInitial = new HashSet<>(dtmc.getIncomingEdges(initialAndFinal.x));
		Set<Edge> outFromOldFinal = new HashSet<>();
		initialAndFinal.y.forEach(n -> outFromOldFinal.addAll(dtmc.getOutgoingEdges(n)));

		transformDelta(sre, result);

		Tuple<Node, Set<Node>> newInitialAndFinal = map.get(sre);

		incToOldInitial.forEach(e -> {
			Edge edge = new Edge(e.from, newInitialAndFinal.x, e.character, e.getProbability());
			changeEdge(result, edge, e);
		});
		outFromOldFinal.forEach(e -> {
			newInitialAndFinal.y.forEach(n -> {
				Edge edge = new Edge(n, e.to, e.character, e.getProbability());
				addEdge(result, edge);
			});
			removeEdge(result, e);
		});
	}

	private DTMC transform(SRE sre) {
		DTMC retVal = new DTMC();
		transform(sre, retVal);
		return retVal;
	}

	private void transform(SRE sre, DTMC dtmc) {
		switch (sre.getType()) {
		case ATOMIC:
			transform((SREAtomic) sre, dtmc);
			break;
		case CAT:
			transform((SREConcat) sre, dtmc);
			break;
		case KLEENE:
			transform((SREKleene) sre, dtmc);
			break;
		case SUM:
			transform((SRESum) sre, dtmc);
			break;
		default:
			throw new IllegalArgumentException();
		}
	}

	private void transformDelta(SRE sre, DTMCDelta delta) {
		if (map.containsKey(sre)) {
			return;
		}
		switch (sre.getType()) {
		case ATOMIC:
			transformDelta((SREAtomic) sre, delta);
			break;
		case CAT:
			transformDelta((SREConcat) sre, delta);
			break;
		case KLEENE:
			transformDelta((SREKleene) sre, delta);
			break;
		case SUM:
			transformDelta((SRESum) sre, delta);
			break;
		default:
			throw new IllegalArgumentException();
		}
	}

	private void transform(SREAtomic sre, DTMC dtmc) {
		dtmc.clearFinalNodes();
		Node n = dtmc.addInitialNode(null);
		Node n2 = dtmc.addFinalNode(null);
		dtmc.addEdge(n, n2, sre.getCharacter(), 1.);
		map.put(sre, new Tuple<DTMC.Node, Set<Node>>(n, CollectionHelpers.setOf(n2)));
	}

	private void transformDelta(SREAtomic sre, DTMCDelta delta) {
		Node n1 = dtmc.addNode(), n2 = dtmc.addNode();
		addEdge(delta, new Edge(n1, n2, sre.getCharacter(), 1.0));
		map.put(sre, new Tuple<DTMC.Node, Set<Node>>(n1, CollectionHelpers.setOf(n2)));
	}

	private void transform(SREConcat sre, DTMC dtmc) {
		Node initialNode = dtmc.addNode();
		Set<Node> lastFinal = CollectionHelpers.setOf(initialNode);
		for (SRE subSre : sre.getSubnodes()) {
			transform(subSre, dtmc);
			for (Node n : lastFinal) {
				dtmc.addEdge(n, dtmc.getInitialNode(), "", 1);
			}
			lastFinal.clear();
			lastFinal.addAll(dtmc.getFinalNodes());
		}
		dtmc.makeNodeInitial(initialNode);
		dtmc.clearFinalNodes();
		Node finalNode = dtmc.addFinalNode(null);
		for (Node n : lastFinal) {
			dtmc.addEdge(n, finalNode, "", 1);
		}
		map.put(sre, new Tuple<DTMC.Node, Set<Node>>(initialNode, CollectionHelpers.setOf(finalNode)));
	}

	private void transformDelta(SREConcat sre, DTMCDelta delta) {
		Node newInitial = dtmc.addNode();
		Node newFinal = dtmc.addNode();
		Set<Node> lastFinal = Stream.of(newInitial).collect(Collectors.toSet());
		for (SRE sub : sre.getSubnodes()) {
			transformDelta(sub, delta);
			for (Node n : lastFinal) {
				addEdge(delta, new Edge(n, map.get(sub).x, "", 1));
			}
			lastFinal.clear();
			lastFinal.addAll(map.get(sub).y);
			for (Node n : lastFinal) {
				for (Edge e : new HashSet<>(dtmc.getOutgoingEdges(n))) {
					removeEdge(delta, e);
				}
			}
		}
		for (Node n : lastFinal) {
			addEdge(delta, new Edge(n, newFinal, "", 1));
		}
		map.put(sre, new Tuple<DTMC.Node, Set<Node>>(newInitial, CollectionHelpers.setOf(newFinal)));
	}

	private void transform(SREKleene sre, DTMC dtmc) {
		transform(sre.getChild(), dtmc);
		dtmc.clearFinalNodes();
		Node newInitial = dtmc.addInitialNode(null);
		Node intermediate = dtmc.addNode();
		Node newFinal = dtmc.addFinalNode(null);

		dtmc.addEdge(newInitial, intermediate, "", 1);
		map.get(sre.getChild()).y.forEach(n -> dtmc.addEdge(n, intermediate, "", 1));
		dtmc.addEdge(intermediate, map.get(sre.getChild()).x, "", sre.getRepetitionRate());
		dtmc.addEdge(intermediate, newFinal, "", 1 - sre.getRepetitionRate());

		map.put(sre, new Tuple<DTMC.Node, Set<Node>>(newInitial, CollectionHelpers.setOf(newFinal)));
	}

	private void transformDelta(SREKleene sre, DTMCDelta delta) {
		transformDelta(sre.getChild(), delta);
		Tuple<Node, Set<Node>> iAndfNode = map.get(sre.getChild());
		Node newFinal = dtmc.addNode();
		Node newInitial = dtmc.addNode();

		iAndfNode.y.forEach(n -> addEdge(delta, new Edge(n, newInitial, "", 1)));
		addEdge(delta, new Edge(newInitial, iAndfNode.x, "", sre.getRepetitionRate()));
		addEdge(delta, new Edge(newInitial, newFinal, "", 1 - sre.getRepetitionRate()));

		map.put(sre, new Tuple<DTMC.Node, Set<Node>>(newInitial, CollectionHelpers.setOf(newFinal)));
	}

	private void transform(SRESum sre, DTMC dtmc) {
		Node initialNode = dtmc.addNode();
		Node finalNode = dtmc.addNode();
		double sum = Arrays.stream(sre.getRates()).sum();

		for (int i = 0; i < sre.getSubnodes().length; i++) {
			transform(sre.getSubnodes()[i], dtmc);
			dtmc.addEdge(initialNode, dtmc.getInitialNode(), "", sre.getRates()[i] / sum);
			dtmc.getFinalNodes().forEach(n -> {
				dtmc.removeFinalNode(n);
				dtmc.addEdge(n, finalNode, "", 1);
			});
		}

		dtmc.clearFinalNodes();
		dtmc.makeNodeInitial(initialNode);
		dtmc.makeNodeFinal(finalNode);
		map.put(sre, new Tuple<DTMC.Node, Set<Node>>(initialNode, CollectionHelpers.setOf(finalNode)));
	}

	private void transformDelta(SRESum sre, DTMCDelta delta) {
		Node initialNode = dtmc.addNode();
		Node finalNode = dtmc.addNode();
		double sum = Arrays.stream(sre.getRates()).sum();

		for (int i = 0; i < sre.getSubnodes().length; i++) {
			transformDelta(sre.getSubnodes()[i], delta);
			Tuple<Node, Set<Node>> iAF = map.get(sre.getSubnodes()[i]);
			addEdge(delta, new Edge(initialNode, iAF.x, "", sre.getRates()[i] / sum));
			iAF.y.forEach(n -> addEdge(delta, new Edge(n, finalNode, "", 1)));
		}

		map.put(sre, new Tuple<DTMC.Node, Set<Node>>(initialNode, CollectionHelpers.setOf(finalNode)));
	}

	private void changeEdge(DTMCDelta result, Edge add, Edge remove) {
		addEdge(result, add);
		removeEdge(result, remove);
	}

	private void removeEdge(DTMCDelta result, Edge remove) {
		result.addChange(remove, DTMCDelta.Type.REMOVE);
		dtmc.removeEdge(remove);
	}

	private void addEdge(DTMCDelta result, Edge add) {
		result.addChange(add, DTMCDelta.Type.ADD);
		dtmc.addEdge(add);
	}
}
