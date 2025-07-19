package com.meinunternehmen.invoicemaker;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.sql.SQLException;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // 1) Datenbank initialisieren
        try {
            Database.init();
        } catch (SQLException e) {
            e.printStackTrace();
            Platform.exit();
        }

        // 2) FXML laden
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/layout.fxml"));
        Parent root = loader.load();

        // 3) Szene und Stage
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Rechnungserzeuger");
        primaryStage.setMinWidth(600);
        primaryStage.setMinHeight(400);
        primaryStage.sizeToScene();
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

