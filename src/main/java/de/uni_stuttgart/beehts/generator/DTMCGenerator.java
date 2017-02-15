package de.uni_stuttgart.beehts.generator;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import de.uni_stuttgart.beehts.model.DTMC;
import de.uni_stuttgart.beehts.model.DTMC.Node;

public class DTMCGenerator {

	private static Random random = ThreadLocalRandom.current();

	public static DTMC generateSparseDTMC(int numberOfNodes) {
		DTMC dtmc = new DTMC();
		dtmc.addInitialNode(null);
		for (int i = 1; i < numberOfNodes; i++) {
			dtmc.addNode();
		}

		// add some edges
		Flags<Node> flags = new Flags<>();
		flags.setState(dtmc.getNodes(), State.UNREACHABLE);
		flags.setState(dtmc.getInitialNode(), State.REACHABLE);

		while (!flags.getByState(State.REACHABLE).isEmpty()) {
			Node n = flags.getByState(State.REACHABLE).iterator().next();
			flags.setState(n, State.PROCESSED);
			if (flags.getByState(State.UNREACHABLE).isEmpty()
					&& (dtmc.getFinalNodes().isEmpty() || random.nextInt(5) < 1)) {
				dtmc.makeNodeFinal(n);
				continue;
			}
			int numOfEdges = random.nextInt(2) + 1;
			for (int i = 0; i < numOfEdges; i++) {
				Node other;
				if (!flags.getByState(State.UNREACHABLE).isEmpty()) {
					other = flags.getByState(State.UNREACHABLE).iterator().next();
					flags.setState(other, State.REACHABLE);
				} else {
					other = dtmc.getNodes().stream().skip(random.nextInt(dtmc.getNodes().size())).findFirst().get();
				}
				try {
					dtmc.addEdge(n, other, getRandomChar(), getEdgeProbability(dtmc, n, numOfEdges - i));
				} catch (IllegalArgumentException e) {
					// if the dtmc already contains this edge
					i--;
				}
			}
		}

		return dtmc;
	}

	public static DTMC generateDenseDTMC(int numberOfNodes) {
		DTMC dtmc = new DTMC();
		Map<Integer, Node> nodeMap = new HashMap<>();

		nodeMap.put(0, dtmc.addInitialNode(null));
		for (int i = 1; i < numberOfNodes; i++)
			nodeMap.put(i, dtmc.addNode());

		for (int i = 0; i < numberOfNodes; i++) {
			Node n = nodeMap.get(i);
			if (i + 1 == numberOfNodes || (i != 0 && random.nextDouble() < 0.1)) {
				dtmc.makeNodeFinal(n);
				continue;
			}
			for (Node other : dtmc.getNodes()) {
				if (other == n) {
					continue;
				}
				double p = getEdgeProbability(dtmc, other, numberOfNodes - i);
				dtmc.addEdge(n, other, getRandomChar(), p);
			}
		}

		return dtmc;
	}

	private static double getEdgeProbability(DTMC dtmc, Node other, int remaining) {
		double currP = dtmc.getOutgoingEdges(other).stream().mapToDouble(e -> e.getProbability()).sum();
		double remP = currP >= 1 ? 0 : 1 - currP;
		if (remaining <= 1)
			return remP;
		else
			return distRandom(0, remP / remaining, 1);
	}

	private static String getRandomChar() {
		return "" + ((char) (random.nextInt(26) + 'a'));
	}

	private static double distRandom(double min, double avg, double max) {
		double dist = Math.pow(random.nextDouble(), 4);
		if (random.nextBoolean()) {
			return avg - dist * (avg - min);
		} else {
			return avg + dist * (max - avg);
		}
	}

	private static class Flags<T> {

		private Map<State, Collection<T>> fromState = new HashMap<>();
		private Map<T, State> fromT = new HashMap<>();

		public Flags() {
			for (State s : State.values()) {
				fromState.put(s, new HashSet<>());
			}
		}

		public Collection<T> getByState(State state) {
			return Collections.unmodifiableCollection(fromState.get(state));
		}

		public State getState(T t) {
			return fromT.get(t);
		}

		public void setState(T t, State s) {
			if (fromT.containsKey(t))
				fromState.get(getState(t)).remove(t);
			fromState.get(s).add(t);
			fromT.put(t, s);
		}

		public void setState(Collection<? extends T> ts, State s) {
			ts.forEach(t -> setState(t, s));
		}
	}

	private enum State {
		UNREACHABLE, REACHABLE, PROCESSED,
	}
}
