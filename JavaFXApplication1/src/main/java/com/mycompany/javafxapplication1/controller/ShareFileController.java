/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1.controller;

import com.mycompany.javafxapplication1.FileService;
import com.mycompany.javafxapplication1.MySQLDB;
import com.mycompany.javafxapplication1.User;
import java.util.Optional;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
/**
 *
 * @author ntu-user
 */
public class ShareFileController {

    @FXML
    private TextField usernameField;

    private User sessionUser;
    private FileService fileService;
    private int fileId;

    public void initialise(User user, FileService service, int fileId) {
        this.sessionUser = user;
        this.fileService = service;
        this.fileId = fileId;
    }

    @FXML
    private void shareRead() {
        share("read");
    }

    @FXML
    private void shareWrite() {
        share("write");
    }

    private void share(String permission) {
        try {
            String targetUsername = usernameField.getText().trim();
            if (targetUsername.isEmpty()) {
                dialogue("Missing Field", "Username cannot be empty.");
                return;
            }
            if (targetUsername.equals(sessionUser.getUsername())) {
                dialogue("Invalid User", "You cannot share with yourself.");
                return;
            }
            MySQLDB db = new MySQLDB();
            User targetUser = db.getUserByName(targetUsername);
            if (targetUser == null) {dialogue("User Not Found", "The specified user does not exist.");return;}
            fileService.share(sessionUser.getUserId(), sessionUser.getUsername(), fileId, targetUser.getUserId(), permission);
            closeWindow();
        } catch (Exception e) {
            e.printStackTrace();
            dialogue("Error", "Failed to share file.");
        }
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.close();
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