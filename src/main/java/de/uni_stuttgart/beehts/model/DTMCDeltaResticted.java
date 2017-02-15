package de.uni_stuttgart.beehts.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import de.uni_stuttgart.beehts.model.DTMC.Edge;
import de.uni_stuttgart.beehts.model.DTMC.Node;

public class DTMCDeltaResticted implements Delta<DTMC> {

	private Map<Edge, DTMC> changes = new HashMap<>();

	public void addChange(Edge toReplace, DTMC replacement) {
		changes.put(toReplace, replacement);
	}

	@Override
	public DTMC applyChanges(DTMC model) {
		for (Edge toReplace : changes.keySet()) {
			Map<Node, Node> nMap = model.attachOther(changes.get(toReplace));
			model.removeEdge(toReplace);
			model.addEdge(toReplace.from, nMap.get(changes.get(toReplace).getInitialNode()), "",
					toReplace.getProbability());
			for (Node n : changes.get(toReplace).getFinalNodes()) {
				model.removeFinalNode(nMap.get(n));
				model.addEdge(nMap.get(n), toReplace.to, "", 1);
			}
		}
		return model;
	}

	public Map<Edge, DTMC> getChanges() {
		return Collections.unmodifiableMap(changes);
	}

}
