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
import javafx.scene.control.Label;
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

    @FXML
    private void openUserManagement() {
        System.out.println("User Management clicked.");
        // TODO: Add your user management UI here
    }
    
    @FXML
    private void openFileManagement() {
        System.out.println("File Management clicked.");
        // TODO: Add your file management UI here
    }
    
    @FXML
    private void openAdvancedPanel() {
        System.out.println("Advanced Panel clicked (admin).");
        // TODO: Add your advanced panel UI here
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
        if (!"admin".equals(user.getRole())) {
            advancedPanelBtn.setVisible(false);
        }
    }
    
}
