/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

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
            MySQLDB mysql = new MySQLDB();
            User fresh = mysql.getUserByName(sessionUser.getUsername());
            if (fresh == null) {
                dialogue("Error", "Your account no longer exists.");
                return;
            }
            if (!mysql.verifyPassword(currentPass, fresh.getPasswordHash())) {
                dialogue("Incorrect Password", "Current password incorrect.");
                return;
            }
            userService.updatePassword(fresh.getUserId(), newPass);
            dialogue("Success", "Password updated.");
            currentPassField.clear();
            newPassField.clear();
            retypeField.clear();
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
            userService.deleteUser(sessionUser.getUserId());
            userService.logout();
            dialogue("Deleted", "Your account has been deleted.");
            Parent root = FXMLLoader.load(getClass().getResource("primary.fxml"));
            Stage stage = (Stage) deleteAccountBtn.getScene().getWindow();
            stage.setScene(new Scene(root, 640, 480));
            stage.setTitle("Login");
        } catch (Exception e) {
            e.printStackTrace();
            dialogue("Offline Mode", "Cannot delete account offline.");
        }
    }

    @FXML
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("secondary.fxml"));
            Parent root = loader.load();
            SecondaryController controller = loader.getController();
            controller.initialise(sessionUser);
            Stage stage = (Stage) backBtn.getScene().getWindow();
            stage.setScene(new Scene(root, 640, 480));
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
            Stage stage = (Stage) logoutBtn.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("primary.fxml"));
            stage.setScene(new Scene(root, 640, 480));
            stage.setTitle("Login");
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
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
    
    public void initialise(User user) {
        sessionUser = user;
        usernameField.setText(user.getUsername());
        usernameField.setEditable(false);
    }
    
}