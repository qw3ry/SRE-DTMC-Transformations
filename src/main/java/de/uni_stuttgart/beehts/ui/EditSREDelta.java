package de.uni_stuttgart.beehts.ui;

import de.uni_stuttgart.beehts.model.Delta;
import de.uni_stuttgart.beehts.model.SRE;
import de.uni_stuttgart.beehts.model.SREDelta;
import javafx.fxml.FXML;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class EditSREDelta extends DialogController {

	@FXML
	private TextArea deltaTextArea;
	@FXML
	private TextArea sreTextArea;
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
		Delta<SRE> delta = SREDelta.parse(parent.getSRE().getValue(), deltaTextArea.getText());
		parent.transform(delta, parent.getSRE().getValue());
	}

	@FXML
	private void handleClose() {
		stage.close();
	}

	private void setText() {
		if (sreTextArea == null || parent == null)
			return;
		sreTextArea.setText(SREDelta.printIndices(parent.getSRE().getValue()));
		parent.getSRE().addListener((oberservable, oldVal, newVal) -> {
			if (newVal != null)
				sreTextArea.setText(SREDelta.printIndices(newVal));
		});
	}
}
