package com.mycompany.javafxapplication1.controller;

import com.mycompany.javafxapplication1.FileModel;
import com.mycompany.javafxapplication1.FileService;
import com.mycompany.javafxapplication1.User;
import java.util.Optional;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

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

    public void initialise(User user, FileService service, FileModel file) {
        this.sessionUser = user;
        this.fileService = service;
        this.originalFile = file;

        // preload original values
        nameField.setText(file.getName());
        pathField.setText(file.getLogicalPath());
        //Not Finished, needs a new function to show contents.
        //String content = fileService.getFileContent(file.getId());
        //contentArea.setText(content);
    }

    @FXML
    private void updateFile() {
        try {
            String newName = nameField.getText().trim();
            String newPath = pathField.getText().trim();
            String content = contentArea.getText();
            boolean nameChanged = !newName.equals(originalFile.getName());
            boolean pathChanged = !newPath.equals(originalFile.getLogicalPath());
            boolean contentChanged = !content.isEmpty();
            if (!nameChanged && !pathChanged && !contentChanged) {
                closeWindow();
                return;
            }
            long sizeBytes = content.getBytes().length;
            if (!contentChanged) {
                fileService.renameMove(originalFile.getId(), newName, newPath);
            } else {
                fileService.update(originalFile.getId(), newPath);
            }
            closeWindow();
        } catch (Exception e) {
            e.printStackTrace();
            dialogue("Error", "Failed to update file.");
        }
    }

    @FXML
    private void closeWindow() {
        if (isDirty()) {
            boolean confirm = dialogue(
                    "Unsaved Changes",
                    "You have unsaved changes. Leave without saving?"
            );
            if (!confirm) return;
        }
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }

    private boolean isDirty() {
        return !nameField.getText().trim().equals(originalFile.getName())
                || !pathField.getText().trim().equals(originalFile.getLogicalPath())
                || !contentArea.getText().isEmpty();
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
