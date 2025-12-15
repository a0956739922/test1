/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import java.io.StringReader;
import java.util.Properties;
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
        new MqttPubUI().send(json);
    }

    public void upload(long ownerId, String localPath, String logicalPath, long sizeBytes) throws Exception {
        JsonObject json = Json.createObjectBuilder()
                .add("action", "upload")
                .add("ownerId", ownerId)
                .add("localFilePath", localPath)
                .add("logicalPath", logicalPath)
                .add("sizeBytes", sizeBytes)
                .build();
        new MqttPubUI().send(json);
    }

    public void update(long fileId, String newLocal, String newLogical, long sizeBytes) throws Exception {
        JsonObject json = Json.createObjectBuilder()
                .add("action", "update")
                .add("fileId", fileId)
                .add("newLocalFilePath", newLocal)
                .add("newLogicalPath", newLogical)
                .add("sizeBytes", sizeBytes)
                .build();
        new MqttPubUI().send(json);
    }

    public void delete(long fileId, long sizeBytes) throws Exception {
        JsonObject json = Json.createObjectBuilder()
                .add("action", "delete")
                .add("fileId", fileId)
                .add("sizeBytes", sizeBytes)
                .build();
        new MqttPubUI().send(json);
    }

    public void download(long fileId) throws Exception {
        JsonObject json = Json.createObjectBuilder()
                .add("action", "download")
                .add("fileId", fileId)
                .build();
        new MqttPubUI().send(json);
    }

    public void downloadSftp(String resultJson,String filename,String downloadPath) throws Exception {
        JsonObject json = Json.createReader(new StringReader(resultJson)).readObject();
        String action = json.getString("action", "");
        boolean ready = json.getBoolean("ready", false);
        if (!"download".equals(action) || !ready) {
            return;
        }
        String remotePath = json.getString("remoteFilePath");
        JSch jsch = new JSch();
        Session session = jsch.getSession("ntu-user", "file-aggregator", 22);
        session.setPassword("ntu-user");
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        config.put("PreferredAuthentications", "password");
        session.setConfig(config);
        session.connect(10000);
        ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
        sftp.connect(5000);
        String localFile = downloadPath + "/" + filename;
        System.out.println("[SFTP] get " + remotePath + " -> " + localFile);
        sftp.get(remotePath, localFile);
        sftp.disconnect();
        session.disconnect();
        System.out.println("[SFTP] Download completed");
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
        new MqttPubUI().send(json);
    }

    public void renameMove(long fileId, String newName, String newLogical, long sizeBytes) throws Exception {
        JsonObject json = Json.createObjectBuilder()
                .add("action", "renameMove")
                .add("fileId", fileId)
                .add("newName", newName)
                .add("newLogicalPath", newLogical)
                .add("sizeBytes", sizeBytes)
                .build();
        new MqttPubUI().send(json);
    }
}
