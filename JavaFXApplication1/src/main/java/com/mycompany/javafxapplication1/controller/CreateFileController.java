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
    private TextField pathField;

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
            String logicalPath = pathField.getText();
            String content = contentArea.getText();
            if (fileName.isEmpty() || logicalPath.isEmpty()) {
                dialogue("Missing Fields", "Name and Path cannot be empty.");
                return;
            }
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