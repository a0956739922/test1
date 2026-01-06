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
import java.util.Optional;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
    private final ObservableList<User> userItems = FXCollections.observableArrayList();
    
    public void initialise(User user) {
        this.sessionUser = user;
        colUserId.setCellValueFactory(new PropertyValueFactory<>("userId"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        userTable.setItems(userItems);
        userTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, selected) -> {
                if (selected != null)
                    selectionLabel.setText("Selected user: " + selected.getUsername());
                else
                    selectionLabel.setText("Select a user to manage.");
            }
        );
        loadUsers();
    }

    private void loadUsers() {
        try {
            MySQLDB mysql = new MySQLDB();
            ObservableList<User> users = mysql.getAllUsers();
            users.removeIf(u -> u.getUsername().equals("admin"));
            users.removeIf(u -> u.getUserId() == sessionUser.getUserId());
            userItems.setAll(users);
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
            Scene scene = new Scene(root, 600, 400);
            scene.getStylesheets().add(getClass().getResource("/com/mycompany/javafxapplication1/app.css").toExternalForm());
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            dialogue("Error", "Cannot open password window.");
        }
    }

    @FXML
    private void promoteDemote() {
        User target = userTable.getSelectionModel().getSelectedItem();
        if (target == null) {
            dialogue("No Selection", "Please select a user from the table.");
            return;
        }
        try {
            UserService userService = new UserService();
            if (target.getRole().equals("admin")) {
                userService.demote(sessionUser, target);
                target.setRole("standard");
                dialogue("User Updated", target.getUsername() + " is now a standard user.");
            } else {
                userService.promote(sessionUser, target);
                target.setRole("admin");
                dialogue("User Updated", target.getUsername() + " is now an admin.");
            }
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
        try {
            UserService userService = new UserService();
            userService.deleteUser(sessionUser, target);
            userItems.remove(target);
        } catch (Exception e) {
            e.printStackTrace();
            dialogue("Error", "Delete failed.");
        }
    }

    @FXML
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/javafxapplication1/advanced.fxml"));
            Parent root = loader.load();
            AdvancedController controller = loader.getController();
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
}