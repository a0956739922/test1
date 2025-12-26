/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1.controller;

import com.mycompany.javafxapplication1.SQLiteDB;
import com.mycompany.javafxapplication1.User;
import com.mycompany.javafxapplication1.UserService;
import java.io.IOException;
import java.util.Optional;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
/**
 *
 * @author ntu-user
 */
public class UserManagementController {

    @FXML
    private TextField usernameField;
    
    @FXML
    private PasswordField currentPassField;
    
    @FXML
    private PasswordField newPassField;
    
    @FXML
    private PasswordField retypeField;

    @FXML
    private Button updatePasswordBtn;
    
    @FXML
    private Button deleteAccountBtn;
    
    @FXML
    private Button backBtn;

    @FXML 
    private Button logoutBtn;
    
    private User sessionUser;
    private UserService userService = new UserService();

    @FXML
    private void updatePassword() {
        String currentPass = currentPassField.getText();
        String newPass = newPassField.getText();
        String retype = retypeField.getText();
        if (currentPass.isEmpty() || newPass.isEmpty() || retype.isEmpty()) {
            dialogue("Missing Fields", "All fields must be filled.");
            return;
        }
        if (!newPass.equals(retype)) {
            dialogue("Mismatch", "New passwords do not match.");
            return;
        }
        try {
            userService.updatePassword(sessionUser, currentPass, newPass);
            dialogue("Success", "Password updated.");
            currentPassField.clear();
            newPassField.clear();
            retypeField.clear();
        } catch (IllegalArgumentException e) {
            switch (e.getMessage()) {
                case "PASSWORD_EMPTY":
                    dialogue("Error", "Password cannot be empty.");
                    break;
                case "WRONG_CURRENT_PASSWORD":
                    dialogue("Incorrect Password", "Current password incorrect.");
                    break;
                default:
                    dialogue("Error", "Unexpected error.");
            }
        } catch (Exception e) {
            dialogue("Offline Mode", "Cannot update password offline.");
        }
    }

    @FXML
    private void deleteAccount() {
        if ("admin".equals(sessionUser.getUsername())) {
            dialogue("Not Allowed", "The default admin account cannot be deleted.");
            return;
        }
        if (!dialogue("Delete Account", "This action cannot be undone. Continue?")) {
            return;
        }
        try {
            userService.deleteUser(sessionUser, sessionUser);
            userService.logout();
            dialogue("Deleted", "Your account has been deleted.");
            Parent root = FXMLLoader.load(getClass().getResource("/com/mycompany/javafxapplication1/primary.fxml"));
            Scene scene = new Scene(root, 1000, 700);
            scene.getStylesheets().add(getClass().getResource("/com/mycompany/javafxapplication1/app.css").toExternalForm());
            Stage stage = (Stage) deleteAccountBtn.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Login");
        } catch (Exception e) {
            e.printStackTrace();
            dialogue("Offline Mode", "Cannot delete account offline.");
        }
    }

    @FXML
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/javafxapplication1/secondary.fxml"));
            Parent root = loader.load();
            SecondaryController controller = loader.getController();
            controller.initialise(sessionUser);
            Scene scene = new Scene(root, 1000, 700);
            scene.getStylesheets().add(getClass().getResource("/com/mycompany/javafxapplication1/app.css").toExternalForm());
            Stage stage = (Stage) backBtn.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Welcome, " + sessionUser.getUsername());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    private void logout() {  
        if (!dialogue("Confirm Logout", "Are you sure you want to log out?")) {
            return;
        }
        try {
            new SQLiteDB().clearSession();
            Parent root = FXMLLoader.load(getClass().getResource("/com/mycompany/javafxapplication1/primary.fxml"));
            Scene scene = new Scene(root, 1000, 700);
            scene.getStylesheets().add(getClass().getResource("/com/mycompany/javafxapplication1/app.css").toExternalForm());
            Stage stage = (Stage) logoutBtn.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean dialogue(String headerMsg, String contentMsg) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText(headerMsg);
        alert.setContentText(contentMsg);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/com/mycompany/javafxapplication1/app.css").toExternalForm());
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
    
    public void initialise(User user) {
        this.sessionUser = user;
        usernameField.setText(user.getUsername());
        usernameField.setEditable(false);
    }
    
}