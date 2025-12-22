/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1.controller;

import com.mycompany.javafxapplication1.FileModel;
import com.mycompany.javafxapplication1.FileService;
import com.mycompany.javafxapplication1.MqttSubUI;
import com.mycompany.javafxapplication1.MySQLDB;
import com.mycompany.javafxapplication1.SQLiteDB;
import com.mycompany.javafxapplication1.User;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
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
    private TableView<FileModel> fileTable;

    @FXML
    private TableColumn<FileModel, String> colOwner;

    @FXML
    private TableColumn<FileModel, String> colFilename;

    @FXML
    private TableColumn<FileModel, String> colPath;

    @FXML
    private TableColumn<FileModel, String> colPermission;

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
    private Button backBtn;

    @FXML
    private Button logoutBtn;

    private User sessionUser;
    private FileService fileService;

    public void initialise(User user) {
        this.sessionUser = user;
        this.fileService = new FileService();
        loadFiles();
    }
    
    private void loadFiles() {
        try {
            MySQLDB mysql = new MySQLDB();
            List<FileModel> files = mysql.getAllFilesByUser(sessionUser.getUserId());
            colFilename.setCellValueFactory(new PropertyValueFactory<>("name"));
            colPath.setCellValueFactory(new PropertyValueFactory<>("logicalPath"));
            colOwner.setCellValueFactory(cellData -> new SimpleStringProperty(sessionUser.getUsername()));
            colPermission.setCellValueFactory(cellData -> new SimpleStringProperty("owner"));
            fileTable.getItems().setAll(files);
            fileTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            colPath.setMaxWidth(Integer.MAX_VALUE);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Cannot load files.");
        }
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
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadFiles();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void updateFile() {
        FileModel selected = fileTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            dialogue("No File Selected", "Please select a file to update.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/javafxapplication1/updateFile.fxml"));
            Parent root = loader.load();
            UpdateFileController controller = loader.getController();
            controller.initialise(sessionUser, fileService, selected);
            Stage stage = new Stage();
            stage.setTitle("Update File");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadFiles();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void deleteFile() {
        FileModel selected = fileTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            dialogue("No File Selected", "Please select a file to delete.");
            return;
        }
        if (!dialogue("Delete", "Proceed to delete " + selected.getName() + "?")) {
            return;
        }
        try {
            fileService.delete(selected.getId());
            dialogue("Deleted", "File deleted successfully.");
            loadFiles();
        } catch (Exception e) {
            e.printStackTrace();
            dialogue("Error", "Failed to delete file.");
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
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadFiles();
        } catch (Exception e) {
            e.printStackTrace();
            dialogue("Error", "Failed to open upload window.");
        }
    }

    @FXML
    private void downloadFile() {
        FileModel selected = fileTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            dialogue("No File Selected", "Please select a file to download.");
            return;
        }
        if (!dialogue("Download", "Proceed to download?")) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save File As");
        chooser.setInitialFileName(selected.getName());
        Stage stage = (Stage) downloadBtn.getScene().getWindow();
        File file = chooser.showSaveDialog(stage);
        if (file == null) return;
        try {
            String reqId = fileService.download(selected.getId());
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
                            fileService.downloadSftp(finalResult, file.getName(), file.getParent());
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
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/javafxapplication1/secondary.fxml"));
            Parent root = loader.load();
            SecondaryController controller = loader.getController();
            controller.initialise(sessionUser);
            Stage stage = (Stage) backBtn.getScene().getWindow();
            stage.setScene(new Scene(root, 640, 480));
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
            new SQLiteDB().clearSession();
            Stage stage = (Stage) logoutBtn.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/com/mycompany/javafxapplication1/primary.fxml"));
            stage.setScene(new Scene(root, 640, 480));
            stage.setTitle("Login");
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
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}
