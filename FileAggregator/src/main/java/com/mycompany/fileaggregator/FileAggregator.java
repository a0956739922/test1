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

    public Object acceptRaw(String rawJson, String serverName) throws Exception {
        JsonObject json = Json.createReader(new StringReader(rawJson)).readObject();
        String action = json.getString("action");
        switch (action) {
            case "upload":
            case "create": {
                long ownerId = json.getJsonNumber("ownerId").longValue();
                String fileName = json.getString("fileName");
                String logicalPath = json.getString("logicalPath");
                String content = json.getString("content");
                return fileService.create(ownerId, fileName, logicalPath, content, serverName);
            }
            case "update": {
                long fileId = json.getJsonNumber("fileId").longValue();
                fileService.update(
                        fileId,
                        json.getString("newLocalFilePath"),
                        json.getString("newLogicalPath"),
                        serverName);
                return fileId;
            }
            case "download": {
                long fileId = json.getJsonNumber("fileId").longValue();
                return fileService.download(fileId, json.getString("outputDir"));
            }
            case "delete": {
                long fileId = json.getJsonNumber("fileId").longValue();
                fileService.delete(fileId);
                return fileId;
            }
            case "share": {
                fileService.share(
                        json.getJsonNumber("fileId").longValue(),
                        json.getJsonNumber("ownerId").longValue(),
                        json.getJsonNumber("targetId").longValue(),
                        json.getString("permission"));
                return json.getJsonNumber("fileId").longValue();
            }
            case "renameMove": {
                fileService.renameMove(
                        json.getJsonNumber("fileId").longValue(),
                        json.getString("newName"),
                        json.getString("newLogicalPath"));
                return json.getJsonNumber("fileId").longValue();
            }
            default:
                throw new IllegalArgumentException("Unknown action: " + action);
        }
    }
}