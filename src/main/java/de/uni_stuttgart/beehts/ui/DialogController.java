package de.uni_stuttgart.beehts.ui;

import javafx.stage.Stage;

public class DialogController {

	protected Stage stage = null;
	protected MainWindowController parent = null;

	public void setInfo(Stage stage, MainWindowController parent) {
		this.stage = stage;
		this.parent = parent;
	}
}
