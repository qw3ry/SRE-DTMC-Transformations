package de.uni_stuttgart.beehts.ui;

import java.io.IOException;

import de.uni_stuttgart.beehts.model.DTMC;
import de.uni_stuttgart.beehts.model.Delta;
import de.uni_stuttgart.beehts.model.SRE;
import de.uni_stuttgart.beehts.transformation.Transformer;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MainWindowController {

	private Stage stage;
	private final GraphDrawer graphDrawer;

	private final Property<SRE> sre;
	private final Property<DTMC> dtmc;
	private Transformer<SRE, DTMC> s2dTransformer;
	private Transformer<DTMC, SRE> d2sTransformer;

	@FXML
	private Label sreLabel;
	@FXML
	private Label statusBarLabel;
	@FXML
	private ProgressBar statusBarProgress;

	public MainWindowController() {
		sre = new SimpleObjectProperty<>(SRE.SREAtomic.EPSILON());
		sre.addListener((oberservable, oldVal, newVal) -> {
			if (newVal != null)
				updateSRELabel(newVal);
		});

		dtmc = new SimpleObjectProperty<>(DTMC.EPSILON());
		graphDrawer = new GraphDrawer(dtmc.getValue());
		graphDrawer.displayGraph();
		dtmc.addListener((oberservable, oldVal, newVal) -> {
			if (newVal != null)
				updateDTMCPane(newVal);
		});
	}

	public void setStage(Stage stage) {
		this.stage = stage;
		this.stage.setTitle("SRE and DTMC transformer");
	}

	@FXML
	private void initialize() {
	}

	@FXML
	private void handleEditSRE() {
		loadDialog("EditSRE.fxml", "Edit SRE");
	}

	@FXML
	private void handleEditDTMC() {
		loadDialog("EditDTMC.fxml", "Edit DTMC");
	}

	@FXML
	private void handleEditSREDelta() {
		loadDialog("EditSREDelta.fxml", "Apply Delta to SRE");
	}

	@FXML
	private void handleEditDTMCDelta() {
		loadDialog("EditDTMCDelta.fxml", "Apply Delta to DTMC");
	}

	private void loadDialog(String resource, String title) {
		try {
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainWindowController.class.getResource(resource));
			Stage dialog = new Stage();
			dialog.setTitle(title);
			dialog.initModality(Modality.NONE);
			dialog.initOwner(stage);
			dialog.setScene(new Scene(loader.load()));

			DialogController controller = loader.getController();
			controller.setInfo(dialog, this);

			dialog.show();
		} catch (IOException s) {
			s.printStackTrace();
		}
	}

	@FXML
	private void handleConvertToSRE() {
		d2sTransformer = Transformer.getNewTransformer(dtmc.getValue());
		s2dTransformer = null; // invalidate because it does not match the model
		this.sre.setValue(d2sTransformer.getTransformed());
	}

	@FXML
	private void handleConvertToDTMC() {
		s2dTransformer = Transformer.getNewTransformer(sre.getValue());
		d2sTransformer = null; // invalidate because it does not match the model
		this.dtmc.setValue(s2dTransformer.getTransformed());
	}

	private void updateSRELabel(SRE sre) {
		if (sreLabel == null || sre == null)
			return;
		sreLabel.setText(sre.toString());
	}

	private void updateDTMCPane(DTMC dtmc) {
		graphDrawer.updateDTMC(dtmc);
	}

	public void transform(Delta<SRE> delta, SRE sre) {
		if (s2dTransformer == null) {
			handleConvertToDTMC();
		}
		s2dTransformer.applyDelta(delta);
		this.sre.setValue(null);
		this.dtmc.setValue(null);
		this.sre.setValue(s2dTransformer.getOriginal());
		this.dtmc.setValue(s2dTransformer.getTransformed());
	}

	public void transform(Delta<DTMC> delta, DTMC dtmc) {
		if (d2sTransformer == null) {
			handleConvertToSRE();
		}
		d2sTransformer.applyDelta(delta);
		this.sre.setValue(null);
		this.dtmc.setValue(null);
		this.sre.setValue(d2sTransformer.getTransformed());
		this.dtmc.setValue(d2sTransformer.getOriginal());
	}

	public Property<DTMC> getDTMC() {
		return dtmc;
	}

	public Property<SRE> getSRE() {
		return sre;
	}
}
