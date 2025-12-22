/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import java.io.File;
import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
/**
 *
 * @author ntu-user
 */

public class FileService {
    
    private final String USERNAME = "ntu-user";
    private final String PASSWORD = "ntu-user";
    private final int REMOTE_PORT = 22;
    
    public void create(long ownerId, String fileName, String logicalPath, String content) throws Exception {
        String reqId = java.util.UUID.randomUUID().toString();
        JsonObject json = Json.createObjectBuilder()
                .add("req_id", reqId)
                .add("action", "create")
                .add("ownerId", ownerId)
                .add("fileName", fileName)
                .add("logicalPath", logicalPath)
                .add("content", content)
                .build();
        new MqttPubUI().send(json);
    }

    public void uploadSftp(String localFilePath, String reqId) throws Exception {
        String tmpDir = "/home/ntu-user/tmp";
        String uploadDir = "/home/ntu-user/tmp/upload";
        String serverPath = uploadDir + "/" + reqId;
        JSch jsch = new JSch();
        jsch.setKnownHosts("/home/ntu-user/.ssh/known_hosts");
        Session session = jsch.getSession(USERNAME, "file-aggregator", REMOTE_PORT);
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.setPassword(PASSWORD);
        session.connect(10000);
        ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
        sftp.connect(5000);
        try {
            sftp.mkdir(tmpDir);
        } catch (Exception ignored) {}
        try {
            sftp.mkdir(uploadDir);
        } catch (Exception ignored) {}
        System.out.println("[SFTP] put " + localFilePath + " -> " + serverPath);
        sftp.put(localFilePath, serverPath);
        sftp.disconnect();
        session.disconnect();
    }
    
    public String upload(long ownerId, String reqId, String fileName, String logicalPath) throws Exception {
        JsonObject json = Json.createObjectBuilder()
                .add("req_id", reqId)
                .add("action", "upload")
                .add("ownerId", ownerId)
                .add("fileName", fileName)
                .add("logicalPath", logicalPath)
                .build();
        new MqttPubUI().send(json);
        return reqId;
    }
    
    public String loadContent(long fileId) throws Exception {
        String reqId = java.util.UUID.randomUUID().toString();
        JsonObject json = Json.createObjectBuilder()
                .add("req_id", reqId)
                .add("action", "loadContent")
                .add("fileId", fileId)
                .build();
        new MqttPubUI().send(json);
        return reqId;
    }

    public void update(long fileId, String newName, String newLogical, String content) throws Exception {
        String reqId = java.util.UUID.randomUUID().toString();
        JsonObjectBuilder json = Json.createObjectBuilder()
                .add("req_id", reqId)
                .add("action", "update")
                .add("fileId", fileId)
                .add("newName", newName)
                .add("newLogicalPath", newLogical);
        if (content != null) {
            json.add("content", content);
        }
        new MqttPubUI().send(json.build());
    }

    public void delete(long fileId) throws Exception {
        String reqId = java.util.UUID.randomUUID().toString();
        JsonObject json = Json.createObjectBuilder()
                .add("req_id", reqId)
                .add("action", "delete")
                .add("fileId", fileId)
                .build();
        new MqttPubUI().send(json);
    }

    public String download(long fileId) throws Exception {
        String reqId = java.util.UUID.randomUUID().toString();
        JsonObject json = Json.createObjectBuilder()
                .add("req_id", reqId)
                .add("action", "download")
                .add("fileId", fileId)
                .build();
        new MqttPubUI().send(json);
        return reqId;
    }

    public void downloadSftp(String resultJson,String filename,String downloadPath) throws Exception {
        JsonObject json = Json.createReader(new StringReader(resultJson)).readObject();
        String status = json.getString("status", "error");
        if (!"ok".equals(status)) {
            throw new Exception("Download failed: " + json);
        }
        String remotePath = json.getString("remoteFilePath");
        JSch jsch = new JSch();
        jsch.setKnownHosts("/home/ntu-user/.ssh/known_hosts");
        Session session = jsch.getSession(USERNAME, "file-aggregator", REMOTE_PORT);
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.setPassword(PASSWORD);
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

    public void share(long fileId, long targetId, String permission) throws Exception {
        String reqId = java.util.UUID.randomUUID().toString();
        JsonObject json = Json.createObjectBuilder()
                .add("req_id", reqId)
                .add("action", "share")
                .add("fileId", fileId)
                .add("targetId", targetId)
                .add("permission", permission)
                .build();
        new MqttPubUI().send(json);
    }

}
