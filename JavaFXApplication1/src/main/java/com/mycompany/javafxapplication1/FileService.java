/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import java.io.File;
import java.io.FileWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
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
    private MySQLDB remote = new MySQLDB();
    private SQLiteDB local = new SQLiteDB();
    private MqttPubUI mqtt = new MqttPubUI();
    
    public String create(String reqId, Integer userId, String username, String fileName, String content) throws Exception {
        remote.log(userId, username, "FILE_CREATE_REQ", "file=" + fileName);
        JsonObject json = Json.createObjectBuilder()
                .add("req_id", reqId)
                .add("action", "create")
                .add("ownerId", userId)
                .add("fileName", fileName)
                .add("content", content)
                .build();
        mqtt.send(json);
        return reqId;
    }
    
    public String loadContent(int fileId) throws Exception {
        String reqId = java.util.UUID.randomUUID().toString();
        JsonObject json = Json.createObjectBuilder()
                .add("req_id", reqId)
                .add("action", "loadContent")
                .add("fileId", fileId)
                .build();
        mqtt.send(json);
        return reqId;
    }

    public String update(Integer userId, String username, int fileId, String oldName, String newName, String content) throws Exception {
        String reqId = java.util.UUID.randomUUID().toString();
        String contentStatus = (content != null) ? "yes" : "no";
        String logDetail;
        if (!oldName.equals(newName)) {
            logDetail = "renamed '" + oldName + "' to '" + newName + "', content_modified=" + contentStatus;
        } else {
            logDetail = "file=" + newName + ", content_modified=" + contentStatus;
        }
        remote.log(userId, username, "FILE_UPDATE_REQ", logDetail);
        JsonObjectBuilder json = Json.createObjectBuilder()
                .add("req_id", reqId)
                .add("action", "update")
                .add("ownerId", userId)
                .add("fileId", fileId)
                .add("newName", newName);
        if (content != null) {
            json.add("content", content);
        }
        mqtt.send(json.build());
        return reqId;
    }

    public String delete(Integer userId, String username, int fileId, String fileName) throws Exception {
        String reqId = java.util.UUID.randomUUID().toString();
        remote.log(userId, username, "FILE_DELETE_REQ", "file=" + fileName);
        JsonObject json = Json.createObjectBuilder()
                .add("req_id", reqId)
                .add("action", "delete")
                .add("ownerId", userId)
                .add("fileId", fileId)
                .add("fileName", fileName)
                .build();
        mqtt.send(json);
        return reqId;
    }

    public String download(Integer userId, String username, int fileId, String fileName) throws Exception {
        String reqId = java.util.UUID.randomUUID().toString();
        remote.log(userId, username, "FILE_DOWNLOAD_REQ", "file=" + fileName);
        JsonObject json = Json.createObjectBuilder()
                .add("req_id", reqId)
                .add("action", "download")
                .add("ownerId", userId)
                .add("fileId", fileId)
                .add("fileName", fileName)
                .build();
        mqtt.send(json);
        return reqId;
    }

    public void share(Integer userId, String username, int fileId, String fileName, int targetId, String targetUsername, String permission) throws Exception {
        String reqId = java.util.UUID.randomUUID().toString();
        remote.log(userId, username, "FILE_SHARE_REQ", "file=" + fileName + ", target_user=" + targetUsername + ", permission=" + permission);
        JsonObject json = Json.createObjectBuilder()
                .add("req_id", reqId)
                .add("action", "share")
                .add("ownerId", userId)
                .add("fileId", fileId)
                .add("fileName", fileName)
                .add("targetId", targetId)
                .add("targetUsername", targetUsername)
                .add("permission", permission)
                .build();
        mqtt.send(json);
    }
    
    public void downloadSftp(Integer userId, String username, String resultJson, String fileName, String downloadPath) throws Exception {
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
        String localFile = downloadPath + "/" + fileName;
        System.out.println("[SFTP] get " + remotePath + " -> " + localFile);
        sftp.get(remotePath, localFile);
        sftp.disconnect();
        session.disconnect();
        System.out.println("[SFTP] Download completed");
    }
    
    public void downloadLocal(int localId, File targetFile) throws Exception {
        String content = local.getLocalFileContent(localId);
        if (content == null) {
            content = "";
        }
        try (FileWriter writer = new FileWriter(targetFile)) {
            writer.write(content);
        }
    }
    
    public String getLocalContent(int localId) {
        return local.getLocalFileContent(localId);
    }
    
    public void finalizeLocalCreate(String reqId, int remoteFileId) {
        local.finalizeCreate(reqId, remoteFileId);
    }
    
    public void finalizeLocalDelete(int remoteFileId) {
        local.finalizeDelete(remoteFileId);
    }
    
    public void finalizeLocalShare(int remoteFileId, String targetUsername, String permission) {
        String currentStr = local.getShareTo(remoteFileId);
        List<String> shares = new ArrayList<>();
        if (!currentStr.isEmpty()) {
            String[] parts = currentStr.split(",");
            for (String p : parts) {
                String clean = p.trim();
                if (!clean.isEmpty() && !clean.startsWith(targetUsername + ":")) {
                    shares.add(clean);
                }
            }
        }
        shares.add(targetUsername + ":" + permission);
        local.updateShareTo(remoteFileId, String.join(",", shares));
    }

}