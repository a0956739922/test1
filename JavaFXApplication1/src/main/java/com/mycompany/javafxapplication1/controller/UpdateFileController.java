package com.mycompany.javafxapplication1.controller;

import com.mycompany.javafxapplication1.FileService;
import com.mycompany.javafxapplication1.LocalFile;
import com.mycompany.javafxapplication1.MqttSubUI;
import com.mycompany.javafxapplication1.MySQLDB;
import com.mycompany.javafxapplication1.SQLiteDB;
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
    private TextArea contentArea;

    private User sessionUser;
    private FileService fileService;
    private LocalFile originalFile;

    private boolean contentLoaded = false;
    private boolean userEdited = false;

    public void initialise(User user, FileService service, LocalFile file) {
        this.sessionUser = user;
        this.fileService = service;
        this.originalFile = file;
        nameField.setText(file.getFileName());
        nameField.setDisable(true);
        contentArea.setDisable(true);
        if (file.getRemoteFileId() == null) {
            loadLocalContent();
            return;
        }
        contentArea.setText("Loading...");
        loadContent(file.getRemoteFileId());
    }

    @FXML
    public void initialize() {
        javafx.application.Platform.runLater(() -> {
            Stage stage = (Stage) nameField.getScene().getWindow();
            stage.setOnCloseRequest(event -> {
                if (userEdited) {
                    boolean confirm = dialogue("Unsaved Changes", "You have unsaved changes. Leave without saving?");
                    if (!confirm) {
                        event.consume();
                    }
                }
            });
        });
    }
    
    private void loadLocalContent() {
        SQLiteDB sqlite = new SQLiteDB();
        String content = sqlite.getLocalFileContent(originalFile.getLocalId());
        contentArea.setText(content);
        contentLoaded = true;
        nameField.setDisable(false);
        contentArea.setDisable(false);
        nameField.textProperty().addListener((o, a, b) -> userEdited = true);
        contentArea.textProperty().addListener((o, a, b) -> userEdited = true);
    }

    @FXML
    private void updateFile() {
        try {
            if (!contentLoaded) {
                return;
            }
            if (!userEdited) {
                closeWindow();
                return;
            }
            if (!dialogue("Save Changes", "Do you want to save the changes?")) {
                return;
            }
            String name = nameField.getText();
            String content = contentArea.getText();
            if (originalFile.getRemoteFileId() == null) {
                SQLiteDB sqlite = new SQLiteDB();
                sqlite.updateLocalFile(originalFile.getLocalId(), name, content);
                userEdited = false;
                closeWindow();
                return;
            }
            fileService.update(
                    sessionUser.getUserId(),
                    sessionUser.getUsername(),
                    originalFile.getRemoteFileId(),
                    name,
                    content
            );
            userEdited = false;
            closeWindow();
        } catch (Exception e) {
            dialogue("DB connect Failed", "Cannot update remote file while offline.");
        }
    }

    private void loadContent(int remoteFileId) {
        try {
            String reqId = fileService.loadContent(remoteFileId);
            MqttSubUI.registerRequestCallback(reqId, payload -> {
                javafx.application.Platform.runLater(() -> {
                    try {
                        JsonObject res = Json.createReader(new StringReader(payload)).readObject();
                        String content = res.getString("content", "");
                        contentArea.setText(content);
                        contentLoaded = true;
                        nameField.setDisable(false);
                        contentArea.setDisable(false);
                        nameField.textProperty().addListener((o, a, b) -> userEdited = true);
                        contentArea.textProperty().addListener((o, a, b) -> userEdited = true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void closeWindow() {
        if (userEdited) {
            boolean confirm = dialogue("Unsaved Changes", "You have unsaved changes. Leave without saving?");
            if (!confirm) {
                return;
            }
        }
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
