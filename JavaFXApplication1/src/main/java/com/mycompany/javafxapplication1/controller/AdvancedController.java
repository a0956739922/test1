/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1.controller;

import com.mycompany.javafxapplication1.SQLiteDB;
import com.mycompany.javafxapplication1.User;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.Set;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
/**
 *
 * @author ntu-user
 */
public class AdvancedController {
    
    @FXML
    private TextArea terminalOutput;

    @FXML
    private TextField terminalInput;

    @FXML
    private Button backBtn;
    
    @FXML
    private Button userManageBtn;
    
    @FXML
    private Button logBtn;
    
    @FXML
    private Button logoutBtn;
    
    private User sessionUser;
    private static final Set<String> ALLOWED_COMMANDS = Set.of("ls", "whoami", "ps", "tree","mkdir", "cp", "mv", "nano");

    @FXML
    private void openUserManagement() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/javafxapplication1/advancedUserManagement.fxml"));
            Parent root = loader.load();
            AdvancedUserManagementController controller = loader.getController();
            controller.initialise(sessionUser);
            Scene scene = new Scene(root, 1000, 700);
            scene.getStylesheets().add(getClass().getResource("/com/mycompany/javafxapplication1/app.css").toExternalForm());
            Stage stage = (Stage) userManageBtn.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Admin User Management");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void openLogView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/javafxapplication1/log.fxml"));
            Parent root = loader.load();
            LogController controller = loader.getController();
            controller.initialise(sessionUser);
            Scene scene = new Scene(root, 1000, 700);
            scene.getStylesheets().add(getClass().getResource("/com/mycompany/javafxapplication1/app.css").toExternalForm());
            Stage stage = (Stage) logBtn.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Log View");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleCommand() {  
        String input = terminalInput.getText();
        terminalInput.clear();
        if (input.isEmpty()) return;
        terminalOutput.appendText("terminal@ntu-user $ " + input + "\n");
        String[] parts = input.split("\\s+");
        String command = parts[0];
        if (!ALLOWED_COMMANDS.contains(command)) {
            terminalOutput.appendText("Command not allowed.\n");
            return;
        }
        if (command.equals("nano")) {
            try {
                ProcessBuilder pb;
                if (parts.length >= 2) {
                    pb = new ProcessBuilder("terminator", "-e", "nano " + parts[1]);
                } else {
                    pb = new ProcessBuilder("terminator", "-e", "nano");
                }
                pb.directory(new File("/home/ntu-user/NetBeansProjects/soft40051_cwk"));
                pb.start();
            } catch (Exception e) {
                terminalOutput.appendText("Failed to launch nano: " + e.getMessage() + "\n");
            }
            return;
        }
        runCommand(parts);
    }
    
    private void runCommand(String[] command) {
        new Thread(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.directory(new File("/home/ntu-user/NetBeansProjects/soft40051_cwk"));
                pb.redirectErrorStream(true);
                Process process = pb.start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String out = line;
                        javafx.application.Platform.runLater(() -> terminalOutput.appendText(out + "\n"));
                    }
                }
                process.waitFor();
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> terminalOutput.appendText("Error: " + e.getMessage() + "\n"));
            }
        }).start();
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
            new SQLiteDB().clearSession();
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
    
    public void initialise(User user) {
        this.sessionUser = user;
    }
    
}