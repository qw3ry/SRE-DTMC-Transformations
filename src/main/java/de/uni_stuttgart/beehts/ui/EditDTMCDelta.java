package de.uni_stuttgart.beehts.ui;

import de.uni_stuttgart.beehts.model.DTMC;
import de.uni_stuttgart.beehts.model.DTMC.Edge;
import de.uni_stuttgart.beehts.model.construction.DTMCParser;
import de.uni_stuttgart.beehts.model.DTMCDeltaResticted;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class EditDTMCDelta extends DialogController {

	@FXML
	private ComboBox<Edge> edgeSelection;
	@FXML
	private TextArea deltaTextArea;
	@FXML
	private ProgressBar progress;

	private Edge selected = null;

	@Override
	public void setInfo(Stage stage, MainWindowController parent) {
		super.setInfo(stage, parent);
		init();
	}

	@FXML
	private void initialize() {
		init();
	}

	@FXML
	private void handleSave() {
		// TODO make selected local
		selected = edgeSelection.getSelectionModel().getSelectedItem();
		if (selected == null) {
			throw new IllegalArgumentException();
		}
		DTMC dtmc = parent.getDTMC().getValue();
		DTMCDeltaResticted delta = new DTMCDeltaResticted();
		delta.addChange(selected, DTMCParser.parse(deltaTextArea.getText()));
		parent.transform(delta, dtmc);
	}

	@FXML
	private void handleClose() {
		stage.close();
	}

	private void init() {
		if (progress == null || edgeSelection == null || parent == null) {
			return;
		}
		progress.setProgress(0);
		edgeSelection.getItems().addAll(parent.getDTMC().getValue().getEdges());
	}
}
