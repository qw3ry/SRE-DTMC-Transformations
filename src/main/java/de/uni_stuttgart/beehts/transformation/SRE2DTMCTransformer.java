package de.uni_stuttgart.beehts.transformation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import de.uni_stuttgart.beehts.model.*;
import de.uni_stuttgart.beehts.model.DTMC.*;
import de.uni_stuttgart.beehts.model.SRE.*;

/**
 * This class helps with converting SREs to DTMCs.
 * 
 * @author Tobias Beeh
 */
public class SRE2DTMCTransformer implements Transformer<SRE, DTMC> {

	private SRE sre;
	private DTMC dtmc = null;

	/**
	 * The constructor.
	 * 
	 * @param sre
	 *            the SRE to transform.
	 */
	public SRE2DTMCTransformer(SRE sre) {
		this.sre = sre;
		transform();
	}

	@Override
	public void transform() {
		dtmc = transform(sre);
	}

	/**
	 * Get the SRE that is transformed.
	 * 
	 * @return The SRE to transform.
	 */
	@Override
	public SRE getOriginal() {
		return sre;
	}

	/**
	 * Get the resulting DTMC. It may be null prior to the transformation. It
	 * may not always be the result either, if the transformation is still
	 * running.
	 * 
	 * @return The transformed DTMC.
	 */
	@Override
	public DTMC getTransformed() {
		return dtmc;
	}

	@Override
	public Delta<DTMC> applyDelta(Delta<SRE> delta) {
		sre = delta.applyChanges(sre);
		transform();
		return null;
	}

	private DTMC transform(SRE sre) {
		DTMC retVal = null;
		switch (sre.getType()) {
		case SUM:
			retVal = createSumDTMC((SRESum) sre);
			break;
		case CAT:
			retVal = createCatDTMC((SREConcat) sre);
			break;
		case KLEENE:
			retVal = createKleeneDTMC((SREKleene) sre);
			break;
		case ATOMIC:
			retVal = createAtomicDTMC((SREAtomic) sre);
			break;
		default:
			throw new IllegalArgumentException("It seems the argument you provided"
					+ "is neither an atomic, sum, concatenation nor kleene closure SRE. This argument is illegal.");
		}
		return retVal;
	}

	private DTMC createAtomicDTMC(SREAtomic sre) {
		if ("".equals(sre.getCharacter())) {
			return DTMC.EPSILON();
		}

		DTMC dtmc = DTMC.EMPTY();

		if (sre.getCharacter() != null) {
			Node n1 = dtmc.getInitialNode();
			Node n2 = dtmc.addFinalNode(null);
			dtmc.addEdge(n1, n2, sre.getCharacter(), 1.0);
		}
		return dtmc;
	}

	private DTMC createCatDTMC(SREConcat sre) {
		SRE[] subnodes = sre.getSubnodes();
		DTMC retVal = DTMC.EPSILON();
		for (SRE subSre : subnodes) {
			retVal = catDTMCs(retVal, transform(subSre));
		}

		return retVal;
	}

	private DTMC createSumDTMC(SRESum sre) {
		SRE[] subnodes = sre.getSubnodes();
		int[] rates = sre.getRates();
		double sum = Arrays.stream(rates).sum();

		return sumDTMCs(IntStream.range(0, subnodes.length)
				.mapToObj(x -> new Tuple<DTMC, Double>(transform(subnodes[x]), ((double) rates[x]) / sum))
				.collect(Collectors.toList()));
	}

	private DTMC createKleeneDTMC(SREKleene sre) {
		DTMC innerDTMC = transform(sre.getChild());
		double rate = sre.getRepetitionRate();

		innerDTMC.getFinalNodes().stream().forEach(n -> {
			new HashSet<>(innerDTMC.getIncomingEdges(n)).stream().forEach(e -> {
				innerDTMC.removeEdge(e);
				innerDTMC.addEdge(e.from, innerDTMC.getInitialNode(), e.character, e.getProbability());
			});
		});
		new HashSet<>(innerDTMC.getOutgoingEdges(innerDTMC.getInitialNode())).stream().forEach(e -> {
			innerDTMC.removeEdge(e);
			innerDTMC.addEdge(e.from, e.to, e.character, e.getProbability() * rate);
		});

		innerDTMC.addEdge(innerDTMC.getInitialNode(), innerDTMC.addFinalNode(null), "", 1 - rate);

		return innerDTMC;
	}

	private DTMC catDTMCs(DTMC m1, DTMC m2) {
		DTMC ret = m1.clone();
		Set<Node> finalNodes = new HashSet<>(ret.getFinalNodes());
		Map<Node, Node> map = ret.attachOther(m2);

		finalNodes.stream().forEach(n -> {
			if (!m2.getFinalNodes().contains(m2.getInitialNode())) {
				ret.removeFinalNode(n);
			}
			Set<Edge> edges = ret.getOutgoingEdges(map.get(m2.getInitialNode()));
			edges.stream().forEach(e -> {
				ret.addEdge(n, e.to, e.character, e.getProbability());
			});
		});

		return ret;
	}

	private DTMC sumDTMCs(List<Tuple<DTMC, Double>> DTMCs) {
		DTMC ret = DTMC.EMPTY();
		Node initial = ret.getInitialNode();
		Node finalNode = ret.addFinalNode(null);

		for (Tuple<DTMC, Double> dtmc : DTMCs) {
			Map<Node, Node> nodeMap = new HashMap<>(dtmc.x.getNodes().size());
			dtmc.x.getNodes().stream().forEachOrdered(n -> {
				nodeMap.put(n, ret.addNode());
			});
			if (dtmc.x.getFinalNodes().size() == 1) {
				dtmc.x.getIncomingEdges(dtmc.x.getFinalNodes().iterator().next()).forEach(e -> {
					ret.addEdge(nodeMap.get(e.from), finalNode, e.character, e.getProbability());
				});
			} else {
				dtmc.x.getFinalNodes().forEach(n -> {
					ret.makeNodeFinal(nodeMap.get(n));
				});
			}
			dtmc.x.getEdges().stream()
					.forEach(e -> ret.addEdge(nodeMap.get(e.from), nodeMap.get(e.to), e.character, e.getProbability()));
			Node oldInitial = nodeMap.get(dtmc.x.getInitialNode());
			if (ret.getFinalNodes().contains(oldInitial)) {
				ret.addEdge(initial, oldInitial, "", dtmc.y);
			} else {
				ret.getOutgoingEdges(nodeMap.get(dtmc.x.getInitialNode())).stream()
						.forEach(e -> ret.addEdge(initial, e.to, e.character, dtmc.y));
			}
		}

		return ret;
	}
}
