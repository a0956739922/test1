/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package com.mycompany.javafxapplication1.controller;

import com.mycompany.javafxapplication1.UserService;
import java.io.File;
import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * FXML Controller class
 *
 * @author ntu-user
 */
public class RegisterController {

    @FXML
    private Button registerBtn;

    @FXML
    private Button backLoginBtn;

    @FXML
    private PasswordField passPasswordField;

    @FXML
    private PasswordField rePassPasswordField;

    @FXML
    private TextField userTextField;    

    @FXML
    private void registerBtnHandler(ActionEvent event) throws IOException, Exception {
        String username = userTextField.getText();
        String pass = passPasswordField.getText();
        String rePass = rePassPasswordField.getText();
        if (!pass.equals(rePass)) {
            error("Password Mismatch", "The two passwords do not match.");
            return;
        }
        try {
            UserService service = new UserService();
            service.createUser(username, pass, "standard");
            dialogue("Registration Successful!", "User created successfully.");
            Stage primaryStage = (Stage) registerBtn.getScene().getWindow();
            Stage secondaryStage = new Stage();
            Parent root = FXMLLoader.load(getClass().getResource("/com/mycompany/javafxapplication1/primary.fxml"));
            Scene scene = new Scene(root, 1000, 700);
            secondaryStage.setScene(scene);
            secondaryStage.setTitle("Login");
            secondaryStage.show();
            primaryStage.close();
        } catch (IllegalArgumentException e) {
            switch (e.getMessage()) {
                case "USERNAME_EMPTY" -> error("Invalid Username", "Username cannot be empty.");
                case "USERNAME_SPACE" -> error("Invalid Username", "Username cannot contain spaces.");
                case "USERNAME_EXISTS" -> error("Registration Failed", "This username is already taken.");
                case "PASSWORD_EMPTY" -> error("Invalid Password", "Password cannot be empty.");
                default -> error("Registration Failed", "Database error. Please try again.");
            }
        }
    }

    @FXML
    private void backLoginBtnHandler(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/javafxapplication1/primary.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1000, 700);
            Stage stage = (Stage) backLoginBtn.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Login");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void dialogue(String headerMsg, String contentMsg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Message");
        alert.setHeaderText(headerMsg);
        alert.setContentText(contentMsg);
        alert.showAndWait();
    }

    private void error(String headerMsg, String contentMsg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(headerMsg);
        alert.setContentText(contentMsg);
        alert.showAndWait();
    }

}
