/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1.controller;

import com.mycompany.javafxapplication1.FileService;
import com.mycompany.javafxapplication1.MqttSubUI;
import com.mycompany.javafxapplication1.User;
import java.io.File;
import java.nio.file.Files;
import java.util.Optional;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
/**
 *
 * @author ntu-user
 */
public class UploadFileController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField localPathField;

    @FXML
    private TextField logicalPathField;

    private User sessionUser;
    private FileService fileService;
    private File selectedFile;

    public void initialise(User user, FileService service) {
        this.sessionUser = user;
        this.fileService = service;
    }

    @FXML
    private void browseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select File to Upload");
        Stage stage = (Stage) nameField.getScene().getWindow();
        selectedFile = chooser.showOpenDialog(stage);
        if (selectedFile == null) return;
        localPathField.setText(selectedFile.getAbsolutePath());
        if (nameField.getText().isEmpty()) {
            nameField.setText(selectedFile.getName());
        }
    }

    @FXML
    private void uploadFile() {
        try {
            if (selectedFile == null) {
                dialogue("Missing File", "Please select a file.");
                return;
            }
            String fileName = nameField.getText();
            String logicalPath = logicalPathField.getText();
            if (fileName.isEmpty() || logicalPath.isEmpty()) {
                dialogue("Missing Fields", "All fields are required.");
                return;
            }
            String content = Files.readString(selectedFile.toPath());
            fileService.create(sessionUser.getUserId(), fileName, logicalPath, content);
            closeWindow();
        } catch (Exception e) {
            e.printStackTrace();
            dialogue("Error", "Failed to upload file.");
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
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}