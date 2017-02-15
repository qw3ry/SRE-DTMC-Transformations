package de.uni_stuttgart.beehts.ui;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.ui.swingViewer.ViewPanel;
import org.graphstream.ui.view.Viewer;

import de.uni_stuttgart.beehts.model.DTMC;

public class GraphDrawer {
	private DTMC dtmc;
	private final Graph graph;

	private static final String stylesheet = Messages.getString("GraphDrawer.stylesheet");

	public GraphDrawer(DTMC dtmc) {
		System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");

		graph = new MultiGraph(Integer.toHexString(dtmc.hashCode()));

		updateDTMC(dtmc);
	}

	public void updateDTMC(DTMC dtmc) {
		this.dtmc = dtmc;
		makeGraph();
	}

	private void makeGraph() {
		graph.clear();

		graph.addAttribute("ui.stylesheet", stylesheet);

		dtmc.getNodes().stream().forEach(n -> {
			String idx = Integer.toString(n.index);
			String label = (n.name == null || n.name.isEmpty()) ? idx : n.name;
			Node node = graph.addNode(idx);
			node.addAttribute("ui.label", label);
			int classFlags = 0;
			if (dtmc.getFinalNodes().contains(n)) {
				classFlags += 1;
			}
			if (dtmc.getInitialNode().equals(n)) {
				classFlags += 2;
			}
			switch (classFlags) {
			case 3:
				node.addAttribute("ui.class", "final,initial");
				break;
			case 2:
				node.addAttribute("ui.class", "initial");
				break;
			case 1:
				node.addAttribute("ui.class", "final");
				break;
			default:
				break;
			}
		});

		dtmc.getEdges().forEach(e -> {
			Edge edge = graph.addEdge(e.from.index + "," + e.to.index + "," + e.character + "," + e.getProbability(),
					Integer.toString(e.from.index), Integer.toString(e.to.index), true);
			edge.addAttribute("ui.label", "\"" + e.character + "\" " + e.getProbability());
		});
	}

	public ViewPanel getView() {
		if (graph == null) {
			return null;
		} else {
			return new Viewer(graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD).addDefaultView(false);
		}
	}

	public Viewer displayGraph() {
		if (graph == null) {
			return null;
		} else {
			return graph.display();
		}
	}

	public static Viewer displayGraph(DTMC dtmc) {
		GraphDrawer gd = new GraphDrawer(dtmc);
		gd.makeGraph();
		return gd.displayGraph();
	}
}
