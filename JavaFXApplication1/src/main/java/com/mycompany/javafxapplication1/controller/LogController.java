/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1.controller;

import com.mycompany.javafxapplication1.Log;
import com.mycompany.javafxapplication1.MySQLDB;
import com.mycompany.javafxapplication1.User;
import com.mycompany.javafxapplication1.UserService;
import java.io.IOException;
import java.util.Optional;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
/**
 *
 * @author ntu-user
 */
public class LogController {

    @FXML
    private TableView<Log> logTable;

    @FXML
    private TableColumn<Log, String> colTime;

    @FXML
    private TableColumn<Log, String> colUser;

    @FXML
    private TableColumn<Log, String> colAction;

    @FXML
    private TableColumn<Log, String> colDetail;

    @FXML
    private TextField searchField;

    @FXML
    private CheckBox chkUser;

    @FXML
    private CheckBox chkFile;

    @FXML
    private Button backBtn;

    @FXML
    private Button logoutBtn;

    private User sessionUser;
    private ObservableList<Log> allLogs;
    private FilteredList<Log> filteredLogs;
    private MySQLDB remote = new MySQLDB();

    public void initialise(User user) {
        this.sessionUser = user;

        colUser.setCellValueFactory(new PropertyValueFactory<>("username"));
        colUser.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : (item == null ? "SYSTEM" : item));
            }
        });

        colAction.setCellValueFactory(new PropertyValueFactory<>("action"));

        colDetail.setCellValueFactory(new PropertyValueFactory<>("detail"));

        colTime.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        try {
            allLogs = remote.getAllLogs();
            filteredLogs = new FilteredList<>(allLogs, p -> true);
            logTable.setItems(filteredLogs);
        } catch (Exception e) {
            e.printStackTrace();
        }

        searchField.textProperty().addListener((obs, o, n) -> applyFilter());
        chkUser.selectedProperty().addListener((obs, o, n) -> applyFilter());
        chkFile.selectedProperty().addListener((obs, o, n) -> applyFilter());
    }

    private void applyFilter() {
        String keyword = searchField.getText();
        if (keyword == null) keyword = "";
        String search = keyword.toLowerCase();

        boolean userSelected = chkUser.isSelected();
        boolean fileSelected = chkFile.isSelected();

        filteredLogs.setPredicate(log -> {
            String action = log.getAction();
            String username = log.getUsername();
            if (username == null) username = "system";
            String detail = log.getDetail();
            if (detail == null) detail = "";
            boolean matchSearch = action.toLowerCase().contains(search)
                    || username.toLowerCase().contains(search)
                    || detail.toLowerCase().contains(search);
            if (!matchSearch) return false;
            if (!userSelected && !fileSelected) {
                return true;
            }
            if (fileSelected && action.startsWith("FILE_")) {
                return true;
            }
            if (userSelected && !action.startsWith("FILE_")) {
                return true;
            }
            return false;
        });
    }

    @FXML
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/javafxapplication1/advanced.fxml"));
            Parent root = loader.load();
            AdvancedController controller = loader.getController();
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
            new UserService().logout();
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
