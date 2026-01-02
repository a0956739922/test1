package com.mycompany.javafxapplication1.controller;

import com.mycompany.javafxapplication1.FileModel;
import com.mycompany.javafxapplication1.FileService;
import com.mycompany.javafxapplication1.MqttSubUI;
import com.mycompany.javafxapplication1.User;
import java.io.StringReader;
import java.util.Optional;
import javafx.application.Platform;
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

    private boolean contentLoaded = false;
    private boolean userEdited = false;

    public void initialise(User user, FileService service, FileModel file) {
        this.sessionUser = user;
        this.fileService = service;
        this.originalFile = file;

        nameField.setText(file.getName());
        pathField.setText(file.getLogicalPath());

        nameField.setDisable(true);
        pathField.setDisable(true);
        contentArea.setDisable(true);

        loadContentAsync(file.getRemoteId());
    }

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            Stage stage = (Stage) nameField.getScene().getWindow();
            stage.setOnCloseRequest(event -> {
                if (userEdited) {
                    boolean confirm = dialogue(
                            "Unsaved Changes",
                            "You have unsaved changes. Leave without saving?"
                    );
                    if (!confirm) {
                        event.consume();
                    }
                }
            });
        });
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

            String newName = nameField.getText().trim();
            String newPath = pathField.getText().trim();
            String content = contentArea.getText();

            fileService.update(
                    sessionUser.getUserId(),
                    sessionUser.getUsername(),
                    originalFile.getRemoteId(),
                    newName,
                    newPath,
                    content
            );

            closeWindow();

        } catch (Exception e) {
            e.printStackTrace();
            dialogue("Error", "Failed to update file.");
        }
    }

    private void loadContentAsync(int fileId) {
        new Thread(() -> {
            try {
                String reqId = fileService.loadContent(fileId);
                String resultJson = null;

                while (resultJson == null) {
                    resultJson = MqttSubUI.RESULTS.remove(reqId);
                    Thread.sleep(100);
                }

                String payload = resultJson;

                Platform.runLater(() -> {
                    try {
                        JsonObject res = Json.createReader(new StringReader(payload)).readObject();
                        String content = res.getString("content", "");

                        contentArea.setText(content);
                        contentLoaded = true;

                        nameField.setDisable(false);
                        pathField.setDisable(false);
                        contentArea.setDisable(false);

                        nameField.textProperty().addListener((o, a, b) -> userEdited = true);
                        pathField.textProperty().addListener((o, a, b) -> userEdited = true);
                        contentArea.textProperty().addListener((o, a, b) -> userEdited = true);

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
        if (userEdited) {
            boolean confirm = dialogue(
                    "Unsaved Changes",
                    "You have unsaved changes. Leave without saving?"
            );
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
        alert.getDialogPane().getStylesheets()
                .add(getClass().getResource("/com/mycompany/javafxapplication1/app.css").toExternalForm());
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}
