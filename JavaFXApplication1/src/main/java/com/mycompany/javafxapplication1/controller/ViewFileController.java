/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1.controller;

import com.mycompany.javafxapplication1.FileService;
import com.mycompany.javafxapplication1.LocalFile;
import com.mycompany.javafxapplication1.MqttSubUI;
import com.mycompany.javafxapplication1.SQLiteDB;
import java.io.StringReader;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import javax.json.Json;
/**
 *
 * @author ntu-user
 */
public class ViewFileController {

    @FXML
    private TextArea contentArea;

    private FileService fileService;
    private LocalFile file;

    public void initialise(FileService service, LocalFile file) {
        this.fileService = service;
        this.file = file;
        contentArea.setEditable(false);
        if (file.getRemoteFileId() == null) {
            loadLocalContent();
            return;
        }
        contentArea.setText("Loading...");
        loadContent();
    }

    private void loadContent() {
        try {
            String reqId = fileService.loadContent(file.getRemoteFileId());
            MqttSubUI.registerRequestCallback(reqId, resultJson -> {
                try {
                    String content = Json.createReader(new StringReader(resultJson)).readObject().getString("content", "");
                    javafx.application.Platform.runLater(() -> contentArea.setText(content));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void loadLocalContent() {
        SQLiteDB sqlite = new SQLiteDB();
        String content = sqlite.getLocalFileContent(file.getLocalId());
        contentArea.setText(content);
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) contentArea.getScene().getWindow();
        stage.close();
    }
}