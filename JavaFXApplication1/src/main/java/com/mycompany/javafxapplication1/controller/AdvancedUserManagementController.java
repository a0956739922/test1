/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1.controller;

import com.mycompany.javafxapplication1.MySQLDB;
import com.mycompany.javafxapplication1.SQLiteDB;
import com.mycompany.javafxapplication1.User;
import com.mycompany.javafxapplication1.UserService;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
/**
 *
 * @author ntu-user
 */
public class AdvancedUserManagementController {

    @FXML
    private TableView<User> userTable;

    @FXML
    private TableColumn<User, Integer> colUserId;

    @FXML
    private TableColumn<User, String> colUsername;

    @FXML
    private TableColumn<User, String> colRole;

    @FXML
    private Label selectionLabel;

    @FXML
    private Button changePasswordBtn;

    @FXML
    private Button promoteDemoteBtn;

    @FXML
    private Button deleteUserBtn;

    @FXML
    private Button backBtn;

    @FXML
    private Button logoutBtn;

    private User sessionUser;

    public void initialise(User user) {
        sessionUser = user;
        loadUsers();
    }

    private void loadUsers() {
        try {
            MySQLDB mysql = new MySQLDB();
            List<User> users = mysql.getAllUsers();
            users.removeIf(u -> u.getUsername().equals("admin"));
            users.removeIf(u -> u.getUserId() == sessionUser.getUserId());
            colUserId.setCellValueFactory(new PropertyValueFactory<>("userId"));
            colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
            colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
            userTable.getItems().setAll(users);
            userTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
                if (selected != null)
                    selectionLabel.setText("Selected user: " + selected.getUsername());
                else
                    selectionLabel.setText("Select a user to manage.");
            });
        } catch (Exception e) {
            selectionLabel.setText("Cannot load users.");
        }
    }

    @FXML
    private void changePassword() {
        User target = userTable.getSelectionModel().getSelectedItem();
        if (target == null) {
            dialogue("No Selection", "Please select a user from the table.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/javafxapplication1/adminChangePassword.fxml"));
            Parent root = loader.load();
            AdminChangePasswordController controller = loader.getController();
            controller.initialise(sessionUser, target);
            Stage stage = new Stage();
            stage.setTitle("Change Password");
            stage.setScene(new Scene(root, 350, 220));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            dialogue("Error", "Cannot open password window.");
        }
    }

    @FXML
    private void promoteDemote() {
        User target = userTable.getSelectionModel().getSelectedItem();
        UserService userService = new UserService();
        if (target == null) {
            dialogue("No Selection", "Please select a user from the table.");
            return;
        }
        try {
            if (target.getRole().equals("admin")) {
                userService.demote(sessionUser, target);
                dialogue("User Updated", target.getUsername() + " is now a standard user.");
            } else {
                userService.promote(sessionUser, target);
                dialogue("User Updated", target.getUsername() + " is now an admin.");
            }
            loadUsers();
        } catch (Exception e) {
            e.printStackTrace();
            dialogue("Error", "Operation failed.");
        }
    }

    @FXML
    private void deleteUser() {
        User target = userTable.getSelectionModel().getSelectedItem();
        if (target == null) {
            dialogue("No Selection", "Please select a user from the table.");
            return;
        }
        if (!dialogue("Delete User", "Are you sure you want to delete " + target.getUsername() + "?")) {
            return;
        }
        System.out.println("Deleting user: " + target.getUsername());
    }

    @FXML
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/javafxapplication1/advanced.fxml"));
            Parent root = loader.load();
            AdvancedController controller = loader.getController();
            controller.initialise(sessionUser);
            Stage stage = (Stage) backBtn.getScene().getWindow();
            stage.setScene(new Scene(root, 640, 480));
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
            Stage stage = (Stage) logoutBtn.getScene().getWindow();
            stage.setScene(new Scene(root, 640, 480));
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
}