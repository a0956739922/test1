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
            Stage stage = (Stage) registerBtn.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("register.fxml"));
            Scene scene = new Scene(root, 640, 480);
            stage.setScene(scene);
            stage.setTitle("Register a new User");
            stage.show();
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
        UserService service = new UserService();
        try {
            service.login(username, password);
            User sessionUser = service.getSessionUser();
            Stage stage = (Stage) registerBtn.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("secondary.fxml"));
            Parent root = loader.load();
            SecondaryController controller = loader.getController();
            controller.initialise(sessionUser);
            Scene scene = new Scene(root, 640, 480);
            stage.setScene(scene);
            stage.setTitle("Welcome, " + sessionUser.getUsername());
        } catch (IllegalArgumentException e) {
            switch (e.getMessage()) {
                case "USER_NOT_FOUND":
                    dialogue("Login Failed", "User does not exist.");
                    break;
                case "PASSWORD_WRONG":
                    dialogue("Login Failed", "Incorrect password.");
                    break;
                default:
                    dialogue("Error", "Unknown validation error.");
            }
        } catch (Exception e) {
            dialogue("Login Failed", "Cannot connect to MySQL.");
        }
    }

}