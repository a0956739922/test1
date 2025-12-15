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
            case "update": {
                long fileId = json.getJsonNumber("fileId").longValue();
                fileService.update(fileId,json.getString("newLocalFilePath"),json.getString("newLogicalPath"));
                return Json.createObjectBuilder().add("action", action).add("fileId", fileId).build();
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
            case "renameMove": {
                long fileId = json.getJsonNumber("fileId").longValue();
                fileService.renameMove(
                        json.getJsonNumber("fileId").longValue(),
                        json.getString("newName"),
                        json.getString("newLogicalPath")
                );
                return Json.createObjectBuilder().add("action", action).add("fileId", fileId).build();
            }
            default:
                throw new IllegalArgumentException("Unknown action: " + action);
        }
    }
}