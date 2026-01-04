/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
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
    private MySQLDB remote = new MySQLDB();
    
    public String create(String reqId, Integer userId, String username, String fileName, String content) throws Exception {
        remote.log(userId, username, "FILE_CREATE_REQ", "fileName=" + fileName);
        JsonObject json = Json.createObjectBuilder()
                .add("req_id", reqId)
                .add("action", "create")
                .add("ownerId", userId)
                .add("fileName", fileName)
                .add("content", content)
                .build();
        new MqttPubUI().send(json);
        return reqId;
    }
    
    public String upload(Integer userId, String username, String fileName, String content) throws Exception {
        remote.log(userId, username, "FILE_CREATE_REQ", "fileName=" + fileName);
        String reqId = java.util.UUID.randomUUID().toString();
        JsonObject json = Json.createObjectBuilder()
                .add("req_id", reqId)
                .add("action", "create")
                .add("ownerId", userId)
                .add("fileName", fileName)
                .add("content", content)
                .build();
        new MqttPubUI().send(json);
        return reqId;
    }
    
    public String loadContent(int fileId) throws Exception {
        String reqId = java.util.UUID.randomUUID().toString();
        JsonObject json = Json.createObjectBuilder()
                .add("req_id", reqId)
                .add("action", "loadContent")
                .add("fileId", fileId)
                .build();
        new MqttPubUI().send(json);
        return reqId;
    }

    public String update(Integer userId, String username, int fileId, String newName, String content) throws Exception {
        remote.log(userId, username, "FILE_UPDATE_REQ", "fileId=" + fileId + ", newName=" + newName + ", hasContent=" + (content != null));
        String reqId = java.util.UUID.randomUUID().toString();
        JsonObjectBuilder json = Json.createObjectBuilder()
                .add("req_id", reqId)
                .add("action", "update")
                .add("ownerId", userId)
                .add("fileId", fileId)
                .add("newName", newName);
        if (content != null) {
            json.add("content", content);
        }
        new MqttPubUI().send(json.build());
        return reqId;
    }

    public String delete(Integer userId, String username, int fileId) throws Exception {
        remote.log(userId, username, "FILE_DELETE_REQ", "fileId=" + fileId);
        String reqId = java.util.UUID.randomUUID().toString();
        JsonObject json = Json.createObjectBuilder()
                .add("req_id", reqId)
                .add("action", "delete")
                .add("ownerId", userId)
                .add("fileId", fileId)
                .build();
        new MqttPubUI().send(json);
        return reqId;
    }

    public String download(Integer userId, String username, int fileId) throws Exception {
        remote.log(userId, username, "FILE_DOWNLOAD_REQ", "fileId=" + fileId);
        String reqId = java.util.UUID.randomUUID().toString();
        JsonObject json = Json.createObjectBuilder()
                .add("req_id", reqId)
                .add("action", "download")
                .add("ownerId", userId)
                .add("fileId", fileId)
                .build();
        new MqttPubUI().send(json);
        return reqId;
    }

    public void downloadSftp(Integer userId, String username, String resultJson, String filename, String downloadPath) throws Exception {
        JsonObject json = Json.createReader(new StringReader(resultJson)).readObject();
        String status = json.getString("status", "error");
        if (!"ok".equals(status)) {
            throw new Exception("Download failed: " + json);
        }
        String remotePath = json.getString("remoteFilePath");
        remote.log(userId, username, "FILE_DOWNLOAD_RESULT", "remotePath=" + remotePath + ", localFile=" + downloadPath + "/" + filename);
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

    public void share(Integer userId, String username, int fileId, int targetId, String permission) throws Exception {
        remote.log(userId, username, "FILE_SHARE_REQ", "fileId=" + fileId + ", targetId=" + targetId + ", permission=" + permission);
        String reqId = java.util.UUID.randomUUID().toString();
        JsonObject json = Json.createObjectBuilder()
                .add("req_id", reqId)
                .add("action", "share")
                .add("ownerId", userId)
                .add("fileId", fileId)
                .add("ownerId", userId)
                .add("targetId", targetId)
                .add("permission", permission)
                .build();
        new MqttPubUI().send(json);
    }

}
