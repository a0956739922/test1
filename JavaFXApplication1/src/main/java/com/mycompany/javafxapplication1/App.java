package com.mycompany.javafxapplication1;

import com.mycompany.javafxapplication1.controller.PrimaryController;
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
    private DbStatusClient statusClient;
    private DbStatusServer statusServer;
    private SyncService syncService;

    @Override
    public void start(Stage stage) throws IOException {
        try {
            mqttSubUI = new MqttSubUI();
            mqttSubUI.start();
            statusServer = new DbStatusServer();
            statusServer.start();
            statusClient = new DbStatusClient();
            statusClient.start();
            syncService = new SyncService(statusClient);
            syncService.start();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("primary.fxml"));
            Parent root = loader.load();
            PrimaryController controller = loader.getController();
            controller.initialise(statusClient);
            Scene scene = new Scene(root, 1000, 700);
            scene.getStylesheets().add(getClass().getResource("app.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("Login");
            stage.show();
            System.out.println("[APP] Application started");
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
        if (syncService != null) {
            syncService.interrupt();
        }
    }
}