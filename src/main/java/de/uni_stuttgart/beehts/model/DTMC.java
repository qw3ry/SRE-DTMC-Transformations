package de.uni_stuttgart.beehts.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class models discrete time markov chains (DTMC). It consists of
 * {@link Node}s and {@link Edge}s.<br>
 * 
 * <h1>Assertions</h1>
 * <ul>
 * <li>The initial node is ALWAYS a valid node.</li>
 * <li>The set of final nodes is NEVER null, though it may be empty.</li>
 * <li>Both the initial node and all final nodes are always contained in the
 * whole node set. Therefore the set of nodes may never be null or empty as
 * well.</li>
 * <li>For any valid node, the set of incoming and outgoing edges may never be
 * null.</li>
 * <li>For any edge in this DTMC, it is guaranteed that the two adjacent nodes
 * exist in this DTMC.</li>
 * <li>In a valid DTMC, each final node has no outgoing edges. For every other
 * nodes, the probabilites of the outgoing edges sum up to 1. The second
 * property cannot be asserted, as it would be impossible to construct a DTMC
 * otherwise. You may, though, call {@link #adjustProbabilities()}. After a call
 * to this function the property is guaranteed.</li>
 * </ul>
 * 
 * @author Tobias Beeh
 */
public class DTMC {

	private static Set<Integer> freeNodeNames = new HashSet<>();
	private static int nextNodeIndex = 0;

	private BidirectionalMap<String, Node> nodes = new BidirectionalMap<>();
	private Set<Edge> edges = new HashSet<>();
	private Map<Node, Set<Edge>> incoming = new HashMap<>();
	private Map<Node, Set<Edge>> outgoing = new HashMap<>();
	private Map<Tuple<Node, Node>, Set<Edge>> fromToMapping = new HashMap<>();

	private Node initialNode;
	private Set<Node> finalNodes = new HashSet<>();

	/**
	 * Creates an "empty" DTMC, that is, a DTMC with an initial node that is
	 * final and has no edges associated. The only String that can be recognized
	 * by this machine is the empty string.
	 * 
	 * @return an empty DTMC.
	 */
	public static DTMC EPSILON() {
		DTMC retVal = new DTMC();

		Node n = retVal.addFinalNode(null);
		retVal.makeNodeInitial(n);

		return retVal;
	}

	/**
	 * Creates a DTMC that accepts the empty set. The DTMC contains no edges and
	 * exactly one node.
	 * 
	 * @return A DTMC accepting the empty set.
	 */
	public static DTMC EMPTY() {
		DTMC retVal = new DTMC();
		retVal.addInitialNode(null);
		return retVal;
	}

	/**
	 * Constructor. Does basically nothing; there are no nodes and no edges in a
	 * newly created DTMC. It recognizes the empty set (that is, nothing).
	 */
	public DTMC() {
	}

	/**
	 * Get the set of nodes this DTMC is based on. Note that the set is
	 * <i>read-only</i>.
	 * 
	 * @return a read-only set of the nodes of this DTMC.
	 */
	public Collection<Node> getNodes() {
		return Collections.unmodifiableCollection(nodes.values());
	}

	/**
	 * Get a node by its index
	 * 
	 * @param index
	 *            the index
	 * @return the node or null
	 */
	public Node getNodeByName(String name) {
		return nodes.get(name);
	}

	/**
	 * Get the set of edges associated with this DTMC. Note that the set is
	 * <i>read-only</i>.
	 * 
	 * @return a read-only set of the edges of this DTMC.
	 */
	public Set<Edge> getEdges() {
		return Collections.unmodifiableSet(edges);
	}

	public Collection<Edge> getEdges(Node from, Node to) {
		return Collections.unmodifiableSet(fromToMapping.getOrDefault(new Tuple<>(from, to), new HashSet<>(0)));
	}

	/**
	 * Get a set of Edges that are incoming to a specified node.
	 * 
	 * @param n
	 *            the node to look for incoming edges.
	 * @return a read-only set of the edges that are incoming to n.
	 */
	public Set<Edge> getIncomingEdges(Node n) {
		if (!this.nodes.getReverseView().containsKey(n)) {
			throw new IllegalArgumentException();
		}
		return Collections.unmodifiableSet(incoming.get(n));
	}

	/**
	 * Get a set of Edges that are outgoing from a specific node.
	 * 
	 * @param n
	 *            the node to look for outgoing edges.
	 * @return a read-only set of the edges that are outgoing from n.
	 */
	public Set<Edge> getOutgoingEdges(Node n) {
		if (!this.nodes.getReverseView().containsKey(n)) {
			throw new IllegalArgumentException();
		}
		return Collections.unmodifiableSet(outgoing.get(n));
	}

	/**
	 * Get the initial node of the DTMC.
	 * 
	 * @return the initial node of this DTMC.
	 */
	public Node getInitialNode() {
		return initialNode;
	}

	/**
	 * Get a set of all final nodes in this DTMC. Note that the set is
	 * <i>read-only</i>.
	 * 
	 * @return a read-only set of all final nodes in this DTMC.
	 */
	public Set<Node> getFinalNodes() {
		return Collections.unmodifiableSet(finalNodes);
	}

	/**
	 * Add a new node to the DTMC. The name may be modified if a node with this
	 * name does already exist.
	 * 
	 * @return the newly created node. You may modify the node afterwards, if
	 *         necessary.
	 * @see {@link #removeNode(Node) removeNode}
	 * @see {@link #addNodes(int) addNodes}
	 */
	public Node addNode(String name) {
		Node n = new Node();
		n.name = name;
		addNodeNoCheck(n);
		return n;
	}

	public Node addNode() {
		return addNodeNoCheck(new Node());
	}

	/**
	 * Add a new node to the DTMC.
	 * 
	 * @param n
	 *            the node to add.
	 */
	public Node addNode(Node n) {
		if (nodes.getReverseView().containsKey(n)) {
			throw new IllegalArgumentException();
		}

		return addNodeNoCheck(n);
	}

	private Node addNodeNoCheck(Node n) {
		while (n.name != null && nodes.containsKey(n.name)) {
			n.name = '_' + n.name;
		}
		nodes.put(n.name == null ? n.index + "" : n.name, n);
		incoming.put(n, new HashSet<>());
		outgoing.put(n, new HashSet<>());
		return n;
	}

	/**
	 * Add a new node to the DTMC and make it final.
	 * 
	 * @return The created node. You may modify the node afterwards, if
	 *         necessary.
	 */
	public Node addFinalNode(String name) {
		Node n = addNode(name);
		this.makeNodeFinal(n);
		return n;
	}

	public Node addInitialNode(String name) {
		Node n = addNode(name);
		this.makeNodeInitial(n);
		return n;
	}

	/**
	 * Remove a given node from the DTMC.
	 * 
	 * @param n
	 *            the node to remove.
	 * @see {@link #addNode() addNode}
	 */
	public void removeNode(Node n) {
		if (this.getInitialNode().equals(n) || !this.nodes.getReverseView().containsKey(n)) {
			throw new IllegalArgumentException();
		}
		removeEdges(getIncomingEdges(n));
		removeEdges(getOutgoingEdges(n));
		this.finalNodes.remove(n);
		this.nodes.remove(n);
		freeNodeNames.add(n.index);
	}

	/**
	 * Remove multiple nodes from the DTMC.
	 * 
	 * @param nodes
	 *            the nodes to remove.
	 */
	public void removeNodes(Collection<Node> nodes) {
		new HashSet<>(nodes).stream().forEach(n -> {
			this.removeNode(n);
		});
	}

	/**
	 * Add a new edge to the DTMC.
	 * 
	 * @param from
	 *            The start node of the edge.
	 * @param to
	 *            The destination node of the edge.
	 * @param character
	 *            The transition character of the edge.
	 * @param probability
	 *            The transition probability of the edge.
	 * @return The newly created edge.
	 * @see {@link #removeEdge(Edge) removeEdge}
	 */
	public Edge addEdge(Node from, Node to, String character, double probability) {
		Edge e = new Edge(from, to, character, probability);
		addEdge(e);
		return e;
	}

	/**
	 * Add a new edge to the DTMC.
	 * 
	 * @param e
	 *            the edge to add.
	 */
	public void addEdge(Edge e) {
		if (!this.nodes.getReverseView().containsKey(e.from) || !this.nodes.getReverseView().containsKey(e.to)
				|| this.getEdges().contains(e) || this.getFinalNodes().contains(e.from)) {
			throw new IllegalArgumentException();
		}
		edges.add(e);
		incoming.get(e.to).add(e);
		outgoing.get(e.from).add(e);
		fromToMapping.putIfAbsent(new Tuple<>(e.from, e.to), new HashSet<Edge>());
		fromToMapping.get(new Tuple<DTMC.Node, DTMC.Node>(e.from, e.to)).add(e);
	}

	/**
	 * Remove an edge from the DTMC.
	 * 
	 * @param e
	 *            The edge to remove.
	 * @see {@link #addEdge(Node, Node, String, double) addEdge}
	 */
	public void removeEdge(Edge e) {
		if (!this.getEdges().contains(e)) {
			throw new IllegalArgumentException();
		}
		edges.remove(e);
		incoming.get(e.to).remove(e);
		outgoing.get(e.from).remove(e);
		fromToMapping.get(new Tuple<>(e.from, e.to)).remove(e);
	}

	/**
	 * Remove multiple edges from the DTMC.
	 * 
	 * @param edges
	 *            the edges to remove.
	 */
	public void removeEdges(Collection<Edge> edges) {
		new HashSet<>(edges).stream().forEach(e -> {
			this.removeEdge(e);
		});
	}

	/**
	 * Replace the inital node by n. Note that the old initial node will still
	 * exist in the DTMC, but it is not inital anymore.
	 * 
	 * @param n
	 *            The node to make initial.
	 */
	public void makeNodeInitial(Node n) {
		if (!nodes.getReverseView().containsKey(n) || n == null) {
			throw new IllegalArgumentException();
		}
		initialNode = n;
	}

	/**
	 * Add a node to the set of final nodes in this DTMC.
	 * 
	 * @param n
	 *            The node to add to the final nodes.
	 * @see {@link #removeFinalNode(Node) removeFinalNode}
	 */
	public void makeNodeFinal(Node n) {
		if (!nodes.getReverseView().containsKey(n) || !getOutgoingEdges(n).isEmpty()) {
			throw new IllegalArgumentException();
		}
		finalNodes.add(n);
	}

	/**
	 * Add nodes to the set of final nodes in this DTMC.
	 * 
	 * @param nodes
	 *            The nodes to add to the final nodes.
	 */
	public void makeNodesFinal(Collection<Node> nodes) {
		if (!this.nodes.getReverseView().keySet().containsAll(nodes)) {
			throw new IllegalArgumentException();
		}
		finalNodes.addAll(nodes);
	}

	/**
	 * Remove a node from the set of final nodes.
	 * 
	 * @param n
	 *            The node to remove.
	 * @see {@link #makeNodeFinal(Node) makeNodeFinal}
	 */
	public void removeFinalNode(Node n) {
		finalNodes.remove(n);
	}

	/**
	 * Remove all nodes from the set of final nodes.
	 */
	public void clearFinalNodes() {
		finalNodes = new HashSet<>();
	}

	/**
	 * Adds the nodes and edges of the other DTMC to the this object. Will not
	 * change the other DTMC. Will not change the initial node. Will add the
	 * final nodes of other to the final node set. Note that you need to make
	 * the attached DTMC accessible from the initial node by yourself, if you
	 * want so.
	 * 
	 * @param other
	 *            the other DTMC.
	 * @return a map from the original nodes in other to the new nodes in the
	 *         this object.
	 */
	public Map<Node, Node> attachOther(DTMC other) {
		Map<Node, Node> retVal = new HashMap<>();

		other.getNodes().stream().forEachOrdered(n -> retVal.put(n, this.addNode(n.name)));
		other.getFinalNodes().stream().forEach(n -> this.makeNodeFinal(retVal.get(n)));
		other.getEdges().stream()
				.forEach(e -> this.addEdge(retVal.get(e.from), retVal.get(e.to), e.character, e.getProbability()));

		return retVal;
	}

	/**
	 * Simplify the DTMC. <br>
	 * Currently implemented simplifications:<br>
	 * <ul>
	 * <li>Remove inaccessible nodes (and edges)</li>
	 * <li>Remove nodes that do not lead to a final node</li>
	 * </ul>
	 */
	public void simplify() {
		// strip epsilon edges, wherever possible
		Set<Edge> epsilonEdges = this.getEdges().stream().filter(e -> "".equals(e.character))
				.collect(Collectors.toSet());
		epsilonEdges.forEach(e1 -> {
			if (!this.getFinalNodes().contains(e1.to)) {
				this.getOutgoingEdges(e1.to).forEach(e2 -> {
					this.addEdge(e1.from, e2.to, e2.character, e1.getProbability() * e2.getProbability());
				});
				this.removeEdge(e1);
			}
		});

		// remove inaccessible nodes and nodes that do not lead to a final node
		Collection<Node> potentiallyRemovableNodes = this.getNodes();
		while (!potentiallyRemovableNodes.isEmpty()) {
			Set<Node> nextPRN = new HashSet<>();
			Set<Node> toRemove = new HashSet<>();
			potentiallyRemovableNodes.stream().forEach(n -> {
				if (this.getIncomingEdges(n).isEmpty() && !this.getInitialNode().equals(n)) {
					this.getOutgoingEdges(n).forEach(e -> {
						// Re-add target node if it is already processed.
						nextPRN.add(e.to);
					});
					toRemove.add(n);
				} else if (this.getOutgoingEdges(n).isEmpty() && !this.getFinalNodes().contains(n)
						&& !this.getInitialNode().equals(n)) {
					this.getIncomingEdges(n).forEach(e -> {
						// Re-add from node if it is already processed.
						nextPRN.add(e.from);
					});
					toRemove.add(n);
				}
			});
			this.removeNodes(toRemove);
			potentiallyRemovableNodes = nextPRN;
		}
	}

	/**
	 * Create a deep copy of this DTMC. That means that modifications of the
	 * copy will not propagate back to the original DTMC (and the other way
	 * round).
	 * 
	 * @return A deep copy of this DTMC.
	 */
	@Override
	public DTMC clone() {
		DTMC copy = new DTMC();
		Map<Node, Node> nodeMap = new HashMap<>();

		getNodes().stream().forEach(n -> nodeMap.put(n, copy.addNode(n.name)));
		getFinalNodes().stream().forEach(n -> copy.makeNodeFinal(nodeMap.get(n)));
		copy.makeNodeInitial(nodeMap.get(getInitialNode()));

		getEdges().stream()
				.forEach(e -> copy.addEdge(nodeMap.get(e.from), nodeMap.get(e.to), e.character, e.getProbability()));

		return copy;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("I: ");
		sb.append(initialNode.toString());

		sb.append(";\nF: ");
		sb.append(finalNodes.toString().replaceAll("[\\[\\]]", ""));

		sb.append(";\n");
		sb.append(edges.stream().map(e -> e.toString()).collect(Collectors.joining(",")));

		return sb.toString();
	}

	/**
	 * This class represents edges in DTMCs.
	 * 
	 * @author Tobias Beeh
	 * @see {@link DTMC}
	 */
	public static class Edge {

		/**
		 * The starting node of this edge.
		 */
		public final Node from;
		/**
		 * The destination node of this edge.
		 */
		public final Node to;
		/**
		 * The transition character of this edge.
		 */
		public final String character;
		/**
		 * The transition probability of this edge.
		 */
		private double probability;

		/**
		 * Constructor. Initializes the final fields and does some sanity
		 * checks.
		 * 
		 * @param from
		 *            The start node.
		 * @param to
		 *            The destination node.
		 * @param character
		 *            The transition character.
		 * @param probability
		 *            The transition probability.
		 */
		public Edge(Node from, Node to, String character, double probability) {
			if (character == null) {
				throw new IllegalArgumentException();
			}
			this.from = from;
			this.to = to;
			this.character = character;
			this.probability = probability;
		}

		public double getProbability() {
			return probability;
		}

		@Override
		public boolean equals(Object other) {
			if (other instanceof Edge) {
				Edge e = (Edge) other;
				return e.from.equals(from) && e.to.equals(to) && e.character.equals(character);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			long hashCode = from.index;
			hashCode += to.index;
			hashCode += character.hashCode();
			return (int) (hashCode % Integer.MAX_VALUE);
		}

		@Override
		public String toString() {
			return from.toString() + " --> " + to.toString() + " (\"" + character + "\":" + getProbability() + ")";
		}
	}

	/**
	 * This class represents nodes in DTMCs.
	 * 
	 * @author Tobias Beeh
	 * @see {@link DTMC}
	 */
	public static class Node {

		/**
		 * The index of this node. You can think of this as an unique name of
		 * the node. Automatically assigned.
		 */
		public final int index;
		/**
		 * The name of the node. Use this if you need to give names to your
		 * nodes. This is used by the toString method, if present.
		 */
		public String name = null;

		@Override
		public String toString() {
			return name == null ? Integer.toString(index) : name;
		}

		/**
		 * Constructor. Assigns an available index to the node and adds it to
		 * the DTMC it belongs to. You should probably use {@link DTMC#addNode()
		 * DTMC.addNode} instead.
		 */
		public Node() {
			if (freeNodeNames.isEmpty()) {
				index = nextNodeIndex++;
			} else {
				index = freeNodeNames.iterator().next();
				freeNodeNames.remove(index);
			}
		}
	}

}
