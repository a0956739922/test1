/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.fileaggregator;

import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonObject;
/**
 *
 * @author ntu-user
 */
public class FileAggregator {

    private final FileService fileService = new FileService();

    public JsonObject acceptRaw(String rawJson) throws Exception {
        JsonObject json = Json.createReader(new StringReader(rawJson)).readObject();
        String action = json.getString("action");
        switch (action) {
            case "upload":
            case "create": {
                long ownerId = json.getJsonNumber("ownerId").longValue();
                String fileName = json.getString("fileName");
                String logicalPath = json.getString("logicalPath");
                String content = json.getString("content");
                long fileId = fileService.create(ownerId, fileName, logicalPath, content);
                return Json.createObjectBuilder().add("action", action).add("fileId", fileId).build();
            }
            case "loadContent": {
                long fileId = json.getJsonNumber("fileId").longValue();
                String content = fileService.loadContent(fileId);
                return Json.createObjectBuilder()
                        .add("action", "loadContent")
                        .add("fileId", fileId)
                        .add("content", content)
                        .build();
            }
            case "update": {
                long fileId = json.getJsonNumber("fileId").longValue();
                String newName = json.containsKey("newName") ? json.getString("newName") : null;
                String newLogicalPath = json.containsKey("newLogicalPath") ? json.getString("newLogicalPath") : null;
                String content = json.containsKey("content") ? json.getString("content") : null;
                fileService.update(fileId, newName, newLogicalPath, content);
                return Json.createObjectBuilder()
                        .add("action", "update")
                        .add("fileId", fileId)
                        .add("status", "ok")
                        .build();
            }
            case "download": {
                long fileId = json.getJsonNumber("fileId").longValue();
                String remotePath = fileService.download(fileId);
                return Json.createObjectBuilder()
                        .add("action", action)
                        .add("ready", true)
                        .add("remoteFilePath", remotePath)
                        .build();
            }
            case "delete": {
                long fileId = json.getJsonNumber("fileId").longValue();
                fileService.delete(fileId);
                return Json.createObjectBuilder().add("action", action).add("fileId", fileId).build();
            }
            case "share": {
                long fileId = json.getJsonNumber("fileId").longValue();
                fileService.share(
                        json.getJsonNumber("fileId").longValue(),
                        json.getJsonNumber("ownerId").longValue(),
                        json.getJsonNumber("targetId").longValue(),
                        json.getString("permission")
                );
                return Json.createObjectBuilder()
                        .add("action", action)
                        .add("fileId", fileId)
                        .add("shared", true)
                        .build();
            }
            default:
                throw new IllegalArgumentException("Unknown action: " + action);
        }
    }
}