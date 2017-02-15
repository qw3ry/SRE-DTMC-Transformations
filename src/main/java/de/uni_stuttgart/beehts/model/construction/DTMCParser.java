package de.uni_stuttgart.beehts.model.construction;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.uni_stuttgart.beehts.model.DTMC;
import de.uni_stuttgart.beehts.model.DTMC.Node;

public class DTMCParser {

	public static DTMC parse(String s) {
		if (isCorrectDTMCSyntax(s)) {
			return parseDTMC(s);
		} else if (isCorrectMatrixSyntax(s)) {
			return parseMatrix(s);
		} else {
			throw new IllegalArgumentException();
		}
	}

	private static DTMC parseMatrix(String s) {
		DTMC dtmc = new DTMC();
		Map<String, Node> nameToNode = new HashMap<>();

		String[] edges = s.trim().split("\\R", -1);

		String initial = edges[0].trim();
		nameToNode.put(initial, dtmc.addInitialNode(initial));
		Arrays.stream(edges[1].split("\\s+")).forEach(n -> {
			n = n.trim();
			nameToNode.put(n, dtmc.addFinalNode(n));
		});

		for (String edge : Arrays.asList(edges).subList(2, edges.length)) {
			edge = edge.trim().replaceAll("\\s\\s+", " ");
			String[] parts = edge.split(" ");

			for (int i = 0; i < 2; i++) {
				if (!nameToNode.containsKey(parts[i])) {
					nameToNode.put(parts[i], dtmc.addNode(parts[i]));
				}
			}

			String character = "a" + parts[1];
			if (parts.length == 4) {
				character = parts[3];
			}

			dtmc.addEdge(nameToNode.get(parts[0]), nameToNode.get(parts[1]), character,
					Double.parseDouble(parts[2].replaceAll(",", ".")));
		}

		dtmc.simplify();
		return dtmc;
	}

	private static DTMC parseDTMC(String s) {
		DTMC dtmc = DTMC.EMPTY();
		Map<String, Node> nameToNode = new HashMap<>();

		String[] split = s.replaceAll("[\r\n]", " ").trim().split(";");

		String initial = split[0].split(":")[1].trim();
		nameToNode.put(initial, dtmc.addInitialNode(initial));

		String[] finalNodes = split[1].split(":")[1].split(",");
		Arrays.stream(finalNodes).map(string -> string.trim()).forEach(string -> {
			if (!nameToNode.containsKey(string)) {
				nameToNode.put(string, dtmc.addFinalNode(string));
			} else {
				dtmc.makeNodeFinal(nameToNode.get(string));
			}
		});

		if (split.length == 3) {
			splitEdges(split[2]).forEach(edge -> {
				for (int i = 0; i < 2; i++) {
					if (!nameToNode.containsKey(edge[i])) {
						nameToNode.put(edge[i], dtmc.addNode(edge[i]));
					}
				}

				dtmc.addEdge(nameToNode.get(edge[0]), nameToNode.get(edge[1]), edge[2], Double.parseDouble(edge[3]));
			});
		}

		dtmc.simplify();
		return dtmc;
	}

	private static List<String[]> splitEdges(String edgeString) {
		List<String[]> edges = new LinkedList<>();
		Arrays.stream(edgeString.split(",")).forEach(string -> {
			String[] edge = new String[4];
			string = string.trim();

			// syntax: n1 --> n2 ("char" : rate)
			int firstSplit = string.indexOf("-->");
			edge[0] = string.substring(0, firstSplit).trim();
			int secondSplit = string.indexOf("(", firstSplit);
			edge[1] = string.substring(firstSplit + 3, secondSplit).trim();
			int thirdSplit = string.indexOf(":", secondSplit);
			edge[2] = string.substring(secondSplit + 1, thirdSplit).trim();
			edge[2] = edge[2].substring(1, edge[2].length() - 1);
			edge[3] = string.substring(thirdSplit + 1, string.length() - 1).trim();

			edges.add(edge);
		});
		return edges;
	}

	private static boolean isCorrectDTMCSyntax(String s) {
		String regexInitial = "i:\\s*\\w+\\s*;";
		String regexFinal = "f:((\\s*\\w+,)*\\s*\\w+)?\\s*;";
		String regexEdge1 = "\\w+\\s*\\-\\-\\>\\s*\\w+";
		String regexEdge2 = "\\\"[^\\s]*\\\"\\s*\\:\\s*[01](\\.\\d+)?";
		String regexEdge = regexEdge1 + "\\s*\\(\\s*" + regexEdge2 + "\\s*\\)";
		return s.replaceAll("[\r\n]", " ").trim().toLowerCase().matches(regexInitial + "\\s*" + regexFinal + "\\s*"
				+ "((" + regexEdge + "\\s*,\\s*)*" + regexEdge + "\\s*)?");
	}

	private static boolean isCorrectMatrixSyntax(String s) {
		String[] edges = s.trim().split("\\R", -1);
		if (edges.length < 2)
			return false;
		if (!edges[0].trim().matches("\\w+"))
			return false;
		if (!edges[1].trim().matches("((\\w+\\s+)*\\w+)?"))
			return false;

		String regexProbability = "\\d([\\.\\,]\\d+)?(e-\\d+)?";
		String regexEdge = "\\w+\\s+\\w+\\s+" + regexProbability + "(\\s+\\w+)?";
		for (int i = 2; i < edges.length; i++) {
			if (!edges[i].trim().matches(regexEdge))
				return false;
		}
		return true;
	}

}
