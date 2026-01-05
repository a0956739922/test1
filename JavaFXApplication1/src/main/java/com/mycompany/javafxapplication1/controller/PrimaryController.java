package com.mycompany.javafxapplication1.controller;

import com.mycompany.javafxapplication1.MySQLDB;
import com.mycompany.javafxapplication1.RemoteFile;
import com.mycompany.javafxapplication1.SQLiteDB;
import com.mycompany.javafxapplication1.User;
import com.mycompany.javafxapplication1.UserService;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class PrimaryController {

    @FXML
    private Button registerBtn;

    @FXML
    private TextField userTextField;

    @FXML
    private PasswordField passPasswordField;
    
    @FXML
    private void registerBtnHandler(ActionEvent event) {
        try {
            Stage stage = (Stage) registerBtn.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/com/mycompany/javafxapplication1/register.fxml"));
            Scene scene = new Scene(root, 1000, 700);
            scene.getStylesheets().add(getClass().getResource("/com/mycompany/javafxapplication1/app.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("Register a new User");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void switchToSecondary() {
        String username = userTextField.getText();
        String password = passPasswordField.getText();
        if (username.isEmpty() || password.isEmpty()) {
            dialogue("Missing Information", "Please enter username and password.");
            return;
        }
        UserService service = new UserService();
        try {
            User sessionUser = service.login(username, password);
            SQLiteDB sqlite = new SQLiteDB();
            try {
                MySQLDB mysql = new MySQLDB();
                List<RemoteFile> remoteFiles = mysql.getAllFilesByUser(sessionUser.getUserId());
                sqlite.cacheRemoteOwnedFiles(sessionUser.getUserId(), remoteFiles);
            } catch (Exception e) {
            }
            Stage stage = (Stage) registerBtn.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/javafxapplication1/secondary.fxml"));
            Parent root = loader.load();
            SecondaryController controller = loader.getController();
            controller.initialise(sessionUser);
            Scene scene = new Scene(root, 1000, 700);
            scene.getStylesheets().add(getClass().getResource("/com/mycompany/javafxapplication1/app.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("Welcome, " + sessionUser.getUsername());
        } catch (IllegalArgumentException e) {
            switch (e.getMessage()) {
                case "USER_NOT_FOUND" -> dialogue("Login Failed", "User does not exist.");
                case "PASSWORD_WRONG" -> dialogue("Login Failed", "Incorrect password.");
                default -> dialogue("Error", "Unknown validation error.");
            }
        } catch (Exception e) {
            dialogue("Login Failed", "Cannot connect to MySQL.");
        }
    }

    private void dialogue(String headerMsg, String contentMsg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(headerMsg);
        alert.setContentText(contentMsg);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/com/mycompany/javafxapplication1/app.css").toExternalForm());
        alert.showAndWait();
    }
}
