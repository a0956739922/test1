/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1.controller;

import com.mycompany.javafxapplication1.LocalFile;
import com.mycompany.javafxapplication1.FileService;
import com.mycompany.javafxapplication1.LocalFile;
import com.mycompany.javafxapplication1.MqttSubUI;
import com.mycompany.javafxapplication1.SQLiteDB;
import com.mycompany.javafxapplication1.User;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
/**
 *
 * @author ntu-user
 */
public class FileManagementController {
    
    @FXML
    private TableView<LocalFile> fileTable;

    @FXML
    private TableColumn<LocalFile, String> colOwner;

    @FXML
    private TableColumn<LocalFile, String> colFilename;

    @FXML
    private TableColumn<LocalFile, String> colPath;

    @FXML
    private TableColumn<LocalFile, String> colPermission;
    
    @FXML
    private TableColumn<LocalFile, String> colShareTo;

    @FXML
    private Button createBtn;

    @FXML
    private Button updateBtn;

    @FXML
    private Button deleteBtn;

    @FXML
    private Button uploadBtn;

    @FXML
    private Button downloadBtn;
    
    @FXML
    private Button shareBtn;

    @FXML
    private Button backBtn;

    @FXML
    private Button logoutBtn;

    private User sessionUser;
    private FileService fileService;

    public void initialise(User user) {
        this.sessionUser = user;
        this.fileService = new FileService();
        colFilename.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        colPath.setCellValueFactory(new PropertyValueFactory<>("logicalPath"));
        colOwner.setCellValueFactory(new PropertyValueFactory<>("username"));
        colPermission.setCellValueFactory(new PropertyValueFactory<>("permission"));
        colShareTo.setCellValueFactory(new PropertyValueFactory<>("sharedTo"));
        loadFiles();
        fileTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> updateButtonState(newSel));
        fileTable.setRowFactory(tv -> {
            TableRow<LocalFile> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openViewer(row.getItem());
                }
            });
            return row;
        });
    }
    
    private void loadFiles() {
        SQLiteDB sqlite = new SQLiteDB();
        List<LocalFile> files = sqlite.getAllOwnedFiles(sessionUser.getUserId());
        fileTable.getItems().setAll(files);
    }

    @FXML
    private void createFile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/javafxapplication1/createFile.fxml"));
            Parent root = loader.load();
            CreateFileController controller = loader.getController();
            controller.initialise(sessionUser, fileService);
            Stage stage = new Stage();
            stage.setTitle("Create File");
            Scene scene = new Scene(root, 600, 450);
            scene.getStylesheets().add(getClass().getResource("/com/mycompany/javafxapplication1/app.css").toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();
            loadFiles();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    private void uploadFile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/javafxapplication1/uploadFile.fxml"));
            Parent root = loader.load();
            UploadFileController controller = loader.getController();
            controller.initialise(sessionUser, fileService);
            Stage stage = new Stage();
            stage.setTitle("Upload File");
            Scene scene = new Scene(root, 600, 450);
            scene.getStylesheets().add(getClass().getResource("/com/mycompany/javafxapplication1/app.css").toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();
            loadFiles();
        } catch (Exception e) {
            e.printStackTrace();
            dialogue("Error", "Failed to open upload window.");
        }
    }

    @FXML
    private void updateFile() {
//        LocalFile selected = fileTable.getSelectionModel().getSelectedItem();
//        if (selected == null) {
//            dialogue("No File Selected", "Please select a file to update.");
//            return;
//        }
//        try {
//            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/javafxapplication1/updateFile.fxml"));
//            Parent root = loader.load();
//            UpdateFileController controller = loader.getController();
//            controller.initialise(sessionUser, fileService, selected);
//            Stage stage = new Stage();
//            stage.setTitle("Update File");
//            Scene scene = new Scene(root, 600, 450);
//            scene.getStylesheets().add(getClass().getResource("/com/mycompany/javafxapplication1/app.css").toExternalForm());
//            stage.setScene(scene);
//            stage.showAndWait();
//            loadFiles();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    @FXML
    private void deleteFile() {
        LocalFile selected = fileTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        if (!dialogue("Delete", "Proceed to delete " + selected.getFileName() + "?")) {
            return;
        }
        SQLiteDB sqlite = new SQLiteDB();
        if (selected.getRemoteFileId() == null) {
            sqlite.deletePendingCreate(selected.getLocalId());
        } else {
            sqlite.markPendingDelete(sessionUser.getUserId(), selected.getRemoteFileId());
        }
        loadFiles();
    }

    @FXML
    private void downloadFile() {
        LocalFile selected = fileTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            dialogue("No File Selected", "Please select a file to download.");
            return;
        }
        if (!dialogue("Download", "Proceed to download?")) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save File As");
        chooser.setInitialFileName(selected.getFileName());
        Stage stage = (Stage) downloadBtn.getScene().getWindow();
        File file = chooser.showSaveDialog(stage);
        if (file == null) return;
        try {
            String reqId = fileService.download(sessionUser.getUserId(), sessionUser.getUsername(), selected.getRemoteFileId());
            new Thread(() -> {
                try {
                    String resultJson = null;
                    while (resultJson == null) {
                        resultJson = MqttSubUI.RESULTS.remove(reqId);
                        Thread.sleep(100);
                    }
                    String finalResult = resultJson;
                    javafx.application.Platform.runLater(() -> {
                        try {
                            fileService.downloadSftp(sessionUser.getUserId(), sessionUser.getUsername(), finalResult, file.getName(), file.getParent());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    private void shareFile() {
        LocalFile selected = fileTable.getSelectionModel().getSelectedItem();
        if (selected == null) { dialogue("No File Selected", "Please select a file to share."); return; }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/javafxapplication1/shareFile.fxml"));
            Parent root = loader.load();
            ShareFileController controller = loader.getController();
            controller.initialise(sessionUser, fileService, selected.getRemoteFileId());
            Stage stage = new Stage();
            stage.setTitle("Share File");
            Scene scene = new Scene(root, 480, 300);
            scene.getStylesheets().add(getClass().getResource("/com/mycompany/javafxapplication1/app.css").toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void updateButtonState(LocalFile selected) {
        if (selected == null) {
            updateBtn.setDisable(true);
            deleteBtn.setDisable(true);
            shareBtn.setDisable(true);
            downloadBtn.setDisable(true);
            return;
        }
        String permission = selected.getPermission();
        downloadBtn.setDisable(false);
        switch (permission) {
            case "owner" -> {
                updateBtn.setDisable(false);
                deleteBtn.setDisable(false);
                shareBtn.setDisable(false);
            }
            case "write" -> {
                updateBtn.setDisable(false);
                deleteBtn.setDisable(true);
                shareBtn.setDisable(true);
            }
            case "read" -> {
                updateBtn.setDisable(true);
                deleteBtn.setDisable(true);
                shareBtn.setDisable(true);
            }
            default -> {
                updateBtn.setDisable(true);
                deleteBtn.setDisable(true);
                shareBtn.setDisable(true);
            }
        }
    }
    
    private void openViewer(LocalFile selected) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/javafxapplication1/viewFile.fxml"));
            Parent root = loader.load();
            ViewFileController controller = loader.getController();
            controller.initialise(fileService, selected.getRemoteFileId());
            Stage stage = new Stage();
            stage.setTitle("View File: " + selected.getFileName());
            Scene scene = new Scene(root, 800, 600);
            scene.getStylesheets().add(getClass().getResource("/com/mycompany/javafxapplication1/app.css").toExternalForm());
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            dialogue("Error", "Failed to load file content.");
        }
    }

    @FXML
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/javafxapplication1/secondary.fxml"));
            Parent root = loader.load();
            SecondaryController controller = loader.getController();
            controller.initialise(sessionUser);
            Scene scene = new Scene(root, 1000, 700);
            scene.getStylesheets().add(getClass().getResource("/com/mycompany/javafxapplication1/app.css").toExternalForm());
            Stage stage = (Stage) backBtn.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Welcome, " + sessionUser.getUsername());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    private void logout() {  
        if (!dialogue("Confirm Logout", "Are you sure you want to log out?")) {
            return;
        }
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/mycompany/javafxapplication1/primary.fxml"));
            Scene scene = new Scene(root, 1000, 700);
            scene.getStylesheets().add(getClass().getResource("/com/mycompany/javafxapplication1/app.css").toExternalForm());
            Stage stage = (Stage) logoutBtn.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
