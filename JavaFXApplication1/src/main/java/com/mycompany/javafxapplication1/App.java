package com.mycompany.javafxapplication1;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
/**
 * JavaFX App
 */
public class App extends Application {
    
    private MqttSubUI mqttSubUI;
    private SyncService syncService;
    
    @Override
    public void start(Stage stage) throws IOException {
        try {
            mqttSubUI = new MqttSubUI();
            mqttSubUI.start();
            syncService = new SyncService();
            syncService.start();
            boolean mysqlOnline = true;
            try {
                MySQLDB mysql = new MySQLDB();
                mysql.testConnection();
            } catch (Exception e) {
                mysqlOnline = false;
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("primary.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1000, 700);
            scene.getStylesheets().add(getClass().getResource("app.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("Login");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch();
    }
    
    @Override
    public void stop() {
        System.out.println("[APP] stop() called");
        if (mqttSubUI != null) {
            mqttSubUI.stop();
        }
    }

}