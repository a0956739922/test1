/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1.controller;

import com.mycompany.javafxapplication1.FileService;
import com.mycompany.javafxapplication1.SQLiteDB;
import com.mycompany.javafxapplication1.User;
import java.util.Optional;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
/**
 *
 * @author ntu-user
 */
public class CreateFileController {

    @FXML
    private TextField nameField;

    @FXML
    private TextArea contentArea;

    private User sessionUser;
    private FileService fileService;

    public void initialise(User user, FileService service) {
        this.sessionUser = user;
        this.fileService = service;
    }

    @FXML
    private void createFile() {
        try {
            String fileName = nameField.getText();
            String content = contentArea.getText();
            if (fileName.isEmpty()) {
                dialogue("Missing Fields", "Name cannot be empty.");
                return;
            }
            SQLiteDB sqlite = new SQLiteDB();
            if (sqlite.isFileExists(sessionUser.getUserId(), fileName)) {
                dialogue("Duplicate Name", "A file with this name already exists.");
                return;
            }
            String reqId = java.util.UUID.randomUUID().toString();
            sqlite.insertLocalFile(reqId, null, sessionUser.getUserId(), sessionUser.getUsername(), fileName, "owner", null, content, "PENDING_CREATE");
            closeWindow();
        } catch (Exception e) {
            e.printStackTrace();
            dialogue("Error", "Failed to create file.");
        }
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) nameField.getScene().getWindow();
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