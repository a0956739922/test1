/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1.controller;

import com.mycompany.javafxapplication1.FileService;
import com.mycompany.javafxapplication1.User;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
/**
 *
 * @author ntu-user
 */
public class CreateFileController {

    @FXML private TextField txtName;
    @FXML private TextField txtPath;
    @FXML private TextArea txtContent;

    private User sessionUser;
    private FileService fileService;

    public void initialise(User user, FileService service) {
        this.sessionUser = user;
        this.fileService = service;
    }

    @FXML
    private void createFile() {
        try {
            String name = txtName.getText().trim();
            String path = txtPath.getText().trim();
            String content = txtContent.getText();
            long sizeBytes = content.getBytes().length;
            if (name.isEmpty() || path.isEmpty()) {
                showAlert("Name and Path cannot be empty.");
                return;
            }
            fileService.create(sessionUser.getUserId(), name, path, content, sizeBytes);
            closeWindow();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Failed to create file.");
        }
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) txtName.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Error");
        alert.setContentText(msg);
        alert.show();
    }
}