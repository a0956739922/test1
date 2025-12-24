package com.mycompany.javafxapplication1.controller;

import com.mycompany.javafxapplication1.FileModel;
import com.mycompany.javafxapplication1.FileService;
import com.mycompany.javafxapplication1.MqttSubUI;
import com.mycompany.javafxapplication1.User;
import java.io.StringReader;
import java.util.Optional;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javax.json.Json;
import javax.json.JsonObject;

public class UpdateFileController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField pathField;

    @FXML
    private TextArea contentArea;

    private User sessionUser;
    private FileService fileService;
    private FileModel originalFile;
    private String originalContent = "";

    public void initialise(User user, FileService service, FileModel file) {
        this.sessionUser = user;
        this.fileService = service;
        this.originalFile = file;
        nameField.setText(file.getName());
        pathField.setText(file.getLogicalPath());
        nameField.setDisable(true);
        pathField.setDisable(true);
        contentArea.setDisable(true);
        loadContentAsync(file.getId());
    }
    
    @FXML
    public void initialize() {
        javafx.application.Platform.runLater(() -> {
            Stage stage = (Stage) nameField.getScene().getWindow();
            stage.setOnCloseRequest(event -> {
                closeWindow();
                event.consume();
            });
        });
    }
    
    @FXML
    private void updateFile() {
        try {
            String newName = nameField.getText().trim();
            String newPath = pathField.getText().trim();
            String content = contentArea.getText();
            boolean nameChanged = !newName.equals(originalFile.getName());
            boolean pathChanged = !newPath.equals(originalFile.getLogicalPath());
            boolean contentChanged = !content.equals(originalContent);
            if (!nameChanged && !pathChanged && !contentChanged) {
                closeWindow();
                return;
            }
            if (!dialogue("Save Changes", "Do you want to save the changes?")) return;
            fileService.update(originalFile.getId(), newName, newPath, contentChanged ? content : null);
            originalContent = content;
            originalFile.setLogicalPath(newPath);
            closeWindow();
        } catch (Exception e) {
            e.printStackTrace();
            dialogue("Error", "Failed to update file.");
        }
    }
    
    private void loadContentAsync(long fileId) {
        new Thread(() -> {
            try {
                String reqId = fileService.loadContent(fileId);
                String resultJson = null;
                while (resultJson == null) {
                    resultJson = MqttSubUI.RESULTS.remove(reqId);
                    Thread.sleep(100);
                }
                String payload = resultJson;
                javafx.application.Platform.runLater(() -> {
                    try {
                        JsonObject res = Json.createReader(new StringReader(payload)).readObject();
                        String content = res.getString("content", "");
                        originalContent = content;
                        contentArea.setText(content);
                        nameField.setDisable(false);
                        pathField.setDisable(false);
                        contentArea.setDisable(false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void closeWindow() {
        if (isDirty()) {
            boolean confirm = dialogue("Unsaved Changes", "You have unsaved changes. Leave without saving?");
            if (!confirm) return;
        }
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }

    public boolean isDirty() {
        return !nameField.getText().trim().equals(originalFile.getName())
                || !pathField.getText().trim().equals(originalFile.getLogicalPath())
                || !contentArea.getText().equals(originalContent);
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
