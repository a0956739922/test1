package com.mycompany.javafxapplication1;

import com.mycompany.javafxapplication1.controller.SecondaryController;
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
    
    @Override
    public void start(Stage stage) throws IOException {
        try {
            MqttSubUI sub = new MqttSubUI();
            sub.start();
            SQLiteDB sqlite = new SQLiteDB();
            User cached = sqlite.loadSession();
            boolean mysqlOnline = true;
            try {
                MySQLDB mysql = new MySQLDB();
                mysql.testConnection();
            } catch (Exception e) {
                mysqlOnline = false;
            }
            if (!mysqlOnline && cached != null) {
                openSecondary(stage, cached, true);
                return;
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("primary.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 640, 480);
            stage.setScene(scene);
            stage.setTitle("Login");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void openSecondary(Stage stage, User user, boolean offline) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("secondary.fxml"));
        Parent root = loader.load();
        SecondaryController controller = loader.getController();
        controller.initialise(user);
        Scene scene = new Scene(root, 640, 480);
        stage.setScene(scene);
        String title = "Welcome, " + user.getUsername();
        if (offline) title += " (Offline Mode)";
        stage.setTitle(title);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }

}