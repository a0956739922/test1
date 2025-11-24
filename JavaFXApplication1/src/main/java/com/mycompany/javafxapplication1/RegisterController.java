/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package com.mycompany.javafxapplication1;

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

    /**
     * Initializes the controller class.
     */
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
    private Text fileText;
    
    @FXML
    private Button selectBtn;
    
    @FXML
    private void selectBtnHandler(ActionEvent event) throws IOException {
        Stage primaryStage = (Stage) selectBtn.getScene().getWindow();
        primaryStage.setTitle("Select a File");

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Resource File");
        File selectedFile = fileChooser.showOpenDialog(primaryStage);
        
        if(selectedFile!=null){
            fileText.setText((String)selectedFile.getCanonicalPath());
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

    @FXML
    private void registerBtnHandler(ActionEvent event) throws IOException {
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
            Parent root = FXMLLoader.load(getClass().getResource("primary.fxml"));
            Scene scene = new Scene(root, 640, 480);
            secondaryStage.setScene(scene);
            secondaryStage.setTitle("Login");
            secondaryStage.show();
            primaryStage.close();
        } catch (IllegalArgumentException e) {
            switch (e.getMessage()) {

                case "USERNAME_EMPTY":
                    error("Invalid Username", "Username cannot be empty.");
                    break;

                case "USERNAME_SPACE":
                    error("Invalid Username", "Username cannot contain spaces.");
                    break;

                case "USERNAME_EXISTS":
                    error("Registration Failed", "This username is already taken.");
                    break;

                case "PASSWORD_EMPTY":
                    error("Invalid Password", "Password cannot be empty.");
                    break;

                default:
                    error("Registration Failed", "Database error. Please try again.");
            }
        }
    }

    @FXML
    private void backLoginBtnHandler(ActionEvent event) {
        try {
            Stage primaryStage = (Stage) backLoginBtn.getScene().getWindow();
            Stage secondaryStage = new Stage();
            Parent root = FXMLLoader.load(getClass().getResource("primary.fxml"));
            Scene scene = new Scene(root, 640, 480);
            secondaryStage.setScene(scene);
            secondaryStage.setTitle("Login");
            secondaryStage.show();
            primaryStage.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
