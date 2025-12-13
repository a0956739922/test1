/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1.controller;

import com.mycompany.javafxapplication1.FileModel;
import com.mycompany.javafxapplication1.FileService;
import com.mycompany.javafxapplication1.MySQLDB;
import com.mycompany.javafxapplication1.SQLiteDB;
import com.mycompany.javafxapplication1.User;
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

    public void initialise(User user) {
        sessionUser = user;
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
            System.out.println("Cannot load files.");
        }
    }

    @FXML
    private void createFile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/javafxapplication1/createFile.fxml"));
            Parent root = loader.load();
            CreateFileController controller = loader.getController();
            controller.initialise(sessionUser, new FileService());
            Stage stage = new Stage();
            stage.setTitle("Create File");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void updateFile() {
//        String selected = fileListView.getSelectionModel().getSelectedItem();
//        if (selected != null) openModal("/com/mycompany/javafxapplication1/update_file.fxml");
    }

    @FXML
    private void deleteFile() {
//        String selected = fileListView.getSelectionModel().getSelectedItem();
//        if (selected == null) return;
        if (!dialogue("Delete", "Proceed to delete?")) return;
    }

    @FXML
    private void uploadFile() {
    }

    @FXML
    private void downloadFile() {
//        String selected = fileListView.getSelectionModel().getSelectedItem();
//        if (selected == null) return;
        if (!dialogue("Download", "Proceed to download?")) return;
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
