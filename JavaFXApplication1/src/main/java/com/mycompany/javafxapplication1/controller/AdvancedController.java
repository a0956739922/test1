/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1.controller;

import com.mycompany.javafxapplication1.SQLiteDB;
import com.mycompany.javafxapplication1.User;
import java.io.IOException;
import java.util.Optional;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
/**
 *
 * @author ntu-user
 */
public class AdvancedController {

    @FXML
    private Button backBtn;
    
    @FXML
    private Button userManageBtn;
    
    @FXML
    private Button logoutBtn;
    
    private User sessionUser;

    @FXML
    private void openUserManagement() {
            try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/javafxapplication1/advancedUserManagement.fxml"));
            Parent root = loader.load();
            AdvancedUserManagementController controller = loader.getController();
            controller.initialise(sessionUser);
            Stage stage = (Stage) userManageBtn.getScene().getWindow();
            stage.setScene(new Scene(root, 640, 480));
            stage.setTitle("Admin User Management");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void openFileManagement() {
        System.out.println("File Management clicked.");
        // TODO
    }

    @FXML
    private void openLogView() {
        System.out.println("Logs clicked.");
        // TODO
    }

    @FXML
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/javafxapplication1/secondary.fxml"));
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
            Parent root = FXMLLoader.load(getClass().getResource("/com/mycompany/javafxapplication1/primary.fxml"));
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
    }
    
}