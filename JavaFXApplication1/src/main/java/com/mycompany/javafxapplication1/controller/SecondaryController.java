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
import javafx.stage.Stage;



public class SecondaryController {
    
    @FXML 
    private Button userManageBtn;
    
    @FXML 
    private Button fileManageBtn;
    
    @FXML 
    private Button advancedPanelBtn;
    
    @FXML 
    private Button logoutBtn;
    
    private User sessionUser;
    
    @FXML
    private void openUserManagement() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/javafxapplication1/userManagement.fxml"));
            Parent root = loader.load();
            UserManagementController controller = loader.getController();
            controller.initialise(sessionUser);
            Scene scene = new Scene(root, 1000, 700);
            scene.getStylesheets().add(getClass().getResource("/com/mycompany/javafxapplication1/app.css").toExternalForm());
            Stage stage = (Stage) userManageBtn.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("User Management");
        } catch (IOException e) {
            dialogue("User Management Unavailable", "Cannot open User Management in offline mode.");
        }
    }
    
    @FXML
    private void openFileManagement() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/javafxapplication1/fileManagement.fxml"));
            Parent root = loader.load();
            FileManagementController controller = loader.getController();
            controller.initialise(sessionUser);
            Scene scene = new Scene(root, 1000, 700);
            scene.getStylesheets().add(getClass().getResource("/com/mycompany/javafxapplication1/app.css").toExternalForm());
            Stage stage = (Stage) fileManageBtn.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("File Management");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    private void openAdvancedPanel() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/javafxapplication1/advanced.fxml"));
            Parent root = loader.load();
            AdvancedController controller = loader.getController();
            controller.initialise(sessionUser);
            Scene scene = new Scene(root, 1000, 700);
            scene.getStylesheets().add(getClass().getResource("/com/mycompany/javafxapplication1/app.css").toExternalForm());
            Stage stage = (Stage) advancedPanelBtn.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Advanced Panel");
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
            new UserService().logout();
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
        if (!"admin".equals(user.getRole())) {
            advancedPanelBtn.setVisible(false);
        }
    }
    
}
