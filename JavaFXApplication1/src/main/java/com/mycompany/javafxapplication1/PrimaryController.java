package com.mycompany.javafxapplication1;

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
            Stage primaryStage = (Stage) registerBtn.getScene().getWindow();
            Stage secondaryStage = new Stage();
            Parent root = FXMLLoader.load(getClass().getResource("register.fxml"));
            Scene scene = new Scene(root, 640, 480);
            secondaryStage.setScene(scene);
            secondaryStage.setTitle("Register a new User");
            secondaryStage.show();
            primaryStage.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void dialogue(String headerMsg, String contentMsg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(headerMsg);
        alert.setContentText(contentMsg);
        alert.showAndWait();
    }

    @FXML
    private void switchToSecondary() {
        String username = userTextField.getText();
        String password = passPasswordField.getText();
        if (username.isEmpty() || password.isEmpty()) {
            dialogue("Missing Information", "Please enter username and password.");
            return;
        }
        try {
            UserService service = new UserService();
            boolean ok = service.login(username, password);
            if (!ok) {
                dialogue("Invalid Login", "Username or Password incorrect. Please try again.");
                return;
            }
            User sessionUser = service.getSessionUser();
            Stage primaryStage = (Stage) registerBtn.getScene().getWindow();
            Stage secondaryStage = new Stage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("secondary.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 640, 480);
            secondaryStage.setScene(scene);
            secondaryStage.setTitle("Welcome, " + sessionUser.getUsername());
            secondaryStage.show();
            primaryStage.close();
        } catch (Exception e) {
            e.printStackTrace();
            dialogue("Error", "Login failed due to system error.");
        }
    }
}
