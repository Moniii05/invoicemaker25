package com.meinunternehmen.invoicemaker;

import javafx.application.Application;  // liefert launch,...
import javafx.application.Platform; // für exit
import javafx.fxml.FXMLLoader; // load()    
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage; // Fenster

import java.sql.SQLException; 

public class App extends Application {

    @Override    
    public void start(Stage stage) throws Exception {
    	
    	Database db = new Database("jdbc:sqlite:invoicemaker.db");
    	
     try {
            db.initSchema();      
        } catch (SQLException e) { 
            e.printStackTrace();  
            Platform.exit(); 
            return;  
        }
       
        FXMLLoader fxml = new FXMLLoader(getClass().getResource("/layout.fxml")); 
        Parent root = fxml.load();  
        
        LayoutController controller = fxml.getController();
        controller.setDatabase(db);   // hier bekommt controller die DB
        controller.loadRecipientData(); 

        Scene scene = new Scene(root);          
        stage.setScene(scene); 
        stage.setTitle("Rechnungserzeuger");
        stage.setMinWidth(600);
        stage.setMinHeight(400);
        stage.sizeToScene();
        stage.show();    
    }

    public static void main(String[] args) {
        launch(args); 
    }
}

