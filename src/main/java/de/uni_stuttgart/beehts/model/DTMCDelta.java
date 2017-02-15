package de.uni_stuttgart.beehts.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.uni_stuttgart.beehts.model.DTMC.Edge;
import de.uni_stuttgart.beehts.model.DTMC.Node;

public class DTMCDelta implements Delta<DTMC> {

	public enum Type {
		ADD, REMOVE,
	}

	private Set<Edge> toRemove = new HashSet<>();
	private Set<Edge> toAdd = new HashSet<>();
	private Map<String, Node> nodeMap = new HashMap<>();

	public static DTMCDelta parse(String s, DTMC model) {
		if (!isValid(s)) {
			throw new IllegalArgumentException();
		}

		DTMCDelta delta = new DTMCDelta();

		for (String line : s.split("\\R")) {
			line = line.trim();
			if (line.startsWith("+")) {
				delta.toAdd.add(delta.parseEdge(line.substring(1).trim(), model));
			} else if (line.startsWith("-")) {
				delta.toRemove.add(delta.parseEdge(line.substring(1).trim(), model));
			} else {
				String[] edges = line.split(">");
				delta.toRemove.add(delta.parseEdge(edges[0].trim(), model));
				Edge adding = delta.parseEdge(edges[1].trim(), model);
				if (adding.getProbability() == Double.NaN)
					throw new IllegalArgumentException();
				delta.toAdd.add(adding);
			}
		}

		return delta;
	}

	private Edge parseEdge(String s, DTMC model) {
		String[] parts = s.split("\\s+");
		if (parts.length < 3 || parts.length > 4)
			throw new IllegalArgumentException();
		double p = parts.length == 4 ? Double.parseDouble(parts[2]) : Double.NaN;
		String character = parts[parts.length - 1];
		return new Edge(getNode(model, parts[0]), getNode(model, parts[1]), character, p);
	}

	private static boolean isValid(String s) {
		String edgeRef = "\\d+\\s+\\d+\\s+\\w+";
		String edgeAdd = "\\d+\\s+\\d+\\s+\\d[\\.\\,]\\d*\\s+\\w+";
		String adding = "\\+\\s*" + edgeAdd;
		String removing = "\\-\\s*" + edgeRef;
		String changing = edgeRef + "\\s*>\\s*" + edgeAdd;
		String line = "(" + adding + "|" + removing + "|" + changing + ")";
		return s.matches("(" + line + "\\R)*" + line);
	}

	/**
	 * Removes the edge from the model when applied. Convenience for
	 * {@link #addChange(Node, Node, String, Double.NaN, Type.REMOVE)}
	 * 
	 * @param from
	 *            the start node index
	 * @param to
	 *            the end node index
	 * @param character
	 *            the character
	 */
	public void addChange(Node from, Node to, String character) {
		addChange(from, to, character, Type.REMOVE);
	}

	/**
	 * Adds the edge to the model when applied. Convenience for
	 * {@link #addChange(Node, Node, String, double, Type.ADD)}
	 * 
	 * @param from
	 *            the start node index
	 * @param to
	 *            the end node index
	 * @param character
	 *            the character
	 * @param p
	 *            the probability
	 */
	public void addChange(Node from, Node to, String character, double p) {
		addChange(from, to, character, p, Type.ADD);
	}

	/**
	 * Add an additional change to the delta. Use only to remove edges, you need
	 * to specify a probability otherwise. Convenience for
	 * {@link #addChange(Node, Node, String, Double.NaN, Type)}
	 * 
	 * @param from
	 *            the start node index
	 * @param to
	 *            the end node index
	 * @param character
	 *            the character
	 * @param Type
	 *            must be Type.REMOVE
	 */
	public void addChange(Node from, Node to, String character, Type t) {
		addChange(from, to, character, Double.NaN, t);
	}

	/**
	 * Add a change of an edge to the delta.
	 * 
	 * @param from
	 *            the node index the edge comes from
	 * @param to
	 *            the node index the edge goes to
	 * @param character
	 *            the character associated with the edge
	 * @param p
	 *            the probability associated with the edge
	 * @param t
	 *            the type of the change (add or remove edge)
	 */
	public void addChange(Node from, Node to, String character, double p, Type t) {
		if (character == null || t == null) {
			throw new IllegalArgumentException();
		}
		addChange(new Edge(from, to, character, p), t);
	}

	public void addChange(Edge e, Type t) {
		if (t == null) {
			throw new IllegalArgumentException();
		}
		switch (t) {
		case ADD:
			toAdd.add(e);
			break;
		case REMOVE:
			toRemove.add(e);
			break;
		default:
			throw new IllegalArgumentException();
		}
	}

	@Override
	public DTMC applyChanges(DTMC dtmc) {
		for (Edge e : toRemove) {
			dtmc.removeEdge(e);
		}
		for (Edge e : toAdd) {
			dtmc.addEdge(e);
		}
		return dtmc;
	}

	private Node getNode(DTMC model, String name) {
		if (model.getNodeByName(name) != null) {
			return model.getNodeByName(name);
		} else if (!nodeMap.containsKey(name)) {
			nodeMap.put(name, new Node());
			nodeMap.get(name).name = name;
		}
		return nodeMap.get(name);
	}
}
