/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1.controller;

import com.mycompany.javafxapplication1.MySQLDB;
import com.mycompany.javafxapplication1.User;
import com.mycompany.javafxapplication1.UserService;
import java.util.Optional;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;
/**
 *
 * @author ntu-user
 */
public class AdminChangePasswordController {

    @FXML
    private Label infoLabel;

    @FXML
    private PasswordField newPassField;

    @FXML
    private PasswordField retypeField;

    @FXML
    private Button confirmBtn;

    @FXML
    private Button cancelBtn;

    private User adminUser;
    private User targetUser;
    private UserService userService = new UserService();

    @FXML
    private void savePassword() {
        String newPass = newPassField.getText();
        String retype = retypeField.getText();
        if (newPass.isEmpty() || retype.isEmpty()) {
            dialogue("Missing Fields", "All fields must be filled.");
            return;
        }
        if (!newPass.equals(retype)) {
            dialogue("Mismatch", "New passwords do not match.");
            return;
        }
        if (!dialogue("Confirm Change",
                "Are you sure you want to update the password for " + targetUser.getUsername() + "?")) {
            return;
        }
        try {
            userService.adminUpdatePassword(adminUser, targetUser, newPass);
            dialogue("Success", "Password updated.");
            Stage stage = (Stage) confirmBtn.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            dialogue("Offline Mode", "Cannot update password offline.");
        }
    }

    @FXML
    private void cancel() {
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }

    private boolean dialogue(String headerMsg, String contentMsg) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText(headerMsg);
        alert.setContentText(contentMsg);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
    
    public void initialise(User admin, User target) {
        adminUser = admin;
        targetUser = target;
        infoLabel.setText("Change password for: " + target.getUsername());
    }
    
}