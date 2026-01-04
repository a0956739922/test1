/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1.controller;

import com.mycompany.javafxapplication1.FileService;
import com.mycompany.javafxapplication1.MqttSubUI;
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
    private int fileId;

    public void initialise(FileService service, int fileId) {
        this.fileService = service;
        this.fileId = fileId;
        loadContent();
    }

    private void loadContent() {
        try {
            String reqId = fileService.loadContent(fileId);
            MqttSubUI.registerRequestCallback(reqId, (resultJson) -> {
                try {
                    String content = Json.createReader(new StringReader(resultJson)).readObject().getString("content", "");
                    javafx.application.Platform.runLater(() -> {
                        contentArea.setText(content);
                        contentArea.setEditable(false);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) contentArea.getScene().getWindow();
        stage.close();
    }
}