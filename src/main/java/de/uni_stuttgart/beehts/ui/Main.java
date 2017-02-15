package de.uni_stuttgart.beehts.ui;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

	@Override
	public void start(Stage primaryStage) {
		Stage stage = new Stage();
		try {
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainWindowController.class.getResource("MainWindow.fxml"));
			
			stage.setScene(new Scene(loader.load()));
			
			MainWindowController controller = loader.getController();
			controller.setStage(stage);
			
			stage.show();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		launch(args);
	}
}
