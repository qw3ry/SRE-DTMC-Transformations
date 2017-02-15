package de.uni_stuttgart.beehts.ui;

import de.uni_stuttgart.beehts.model.construction.DTMCParser;
import javafx.fxml.FXML;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class EditDTMC extends DialogController {

	@FXML
	private TextArea textArea;
	@FXML
	private ProgressBar progress;

	@Override
	public void setInfo(Stage stage, MainWindowController parent) {
		super.setInfo(stage, parent);
		setText();
	}

	@FXML
	private void initialize() {
		progress.setProgress(0);
		setText();
	}

	@FXML
	private void handleSave() {
		parent.getDTMC().setValue(DTMCParser.parse(textArea.getText()));
	}

	@FXML
	private void handleClose() {
		stage.close();
	}

	private void setText() {
		if (textArea == null || parent == null)
			return;
		textArea.setText(parent.getDTMC().getValue().toString());
		parent.getDTMC().addListener((oberservable, oldVal, newVal) -> {
			if (newVal != null)
				textArea.setText(newVal.toString());
		});
	}
}
