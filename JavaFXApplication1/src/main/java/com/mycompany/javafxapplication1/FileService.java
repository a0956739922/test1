/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

import javax.json.Json;
import javax.json.JsonObject;
/**
 *
 * @author ntu-user
 */

public class FileService {

    public void create(long ownerId, String fileName, String logicalPath, String content, long sizeBytes) throws Exception {
        JsonObject json = Json.createObjectBuilder()
                .add("action", "create")
                .add("ownerId", ownerId)
                .add("fileName", fileName)
                .add("logicalPath", logicalPath)
                .add("content", content)
                .add("sizeBytes", sizeBytes)
                .build();
        new MqttUI().send(json);
    }

    public void upload(long ownerId, String localPath, String logicalPath, long sizeBytes) throws Exception {
        JsonObject json = Json.createObjectBuilder()
                .add("action", "upload")
                .add("ownerId", ownerId)
                .add("localFilePath", localPath)
                .add("logicalPath", logicalPath)
                .add("sizeBytes", sizeBytes)
                .build();
        new MqttUI().send(json);
    }

    public void update(long fileId, String newLocal, String newLogical, long sizeBytes) throws Exception {
        JsonObject json = Json.createObjectBuilder()
                .add("action", "update")
                .add("fileId", fileId)
                .add("newLocalFilePath", newLocal)
                .add("newLogicalPath", newLogical)
                .add("sizeBytes", sizeBytes)
                .build();
        new MqttUI().send(json);
    }

    public void delete(long fileId, long sizeBytes) throws Exception {
        JsonObject json = Json.createObjectBuilder()
                .add("action", "delete")
                .add("fileId", fileId)
                .add("sizeBytes", sizeBytes)
                .build();
        new MqttUI().send(json);
    }

    public void download(long fileId, String outputDir, long sizeBytes) throws Exception {
        JsonObject json = Json.createObjectBuilder()
                .add("action", "download")
                .add("fileId", fileId)
                .add("outputDir", outputDir)
                .add("sizeBytes", sizeBytes)
                .build();
        new MqttUI().send(json);
    }

    public void share(long fileId, long ownerId, long targetId, String permission, long sizeBytes) throws Exception {
        JsonObject json = Json.createObjectBuilder()
                .add("action", "share")
                .add("fileId", fileId)
                .add("ownerId", ownerId)
                .add("targetId", targetId)
                .add("permission", permission)
                .add("sizeBytes", sizeBytes)
                .build();
        new MqttUI().send(json);
    }

    public void renameMove(long fileId, String newName, String newLogical, long sizeBytes) throws Exception {
        JsonObject json = Json.createObjectBuilder()
                .add("action", "renameMove")
                .add("fileId", fileId)
                .add("newName", newName)
                .add("newLogicalPath", newLogical)
                .add("sizeBytes", sizeBytes)
                .build();
        new MqttUI().send(json);
    }
}
