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
/**
 *
 * @author ntu-user
 */

public class FileService {
    
    private final String USERNAME = "ntu-user";
    private final String PASSWORD = "ntu-user";
    private final int REMOTE_PORT = 22;
    private final int SESSION_TIMEOUT = 10000;
    private final int CHANNEL_TIMEOUT = 5000;

    public void create(long ownerId, String fileName, String logicalPath, String content) throws Exception {
        JsonObject json = Json.createObjectBuilder()
                .add("action", "create")
                .add("ownerId", ownerId)
                .add("fileName", fileName)
                .add("logicalPath", logicalPath)
                .add("content", content)
                .build();
        new MqttPubUI().send(json);
    }

    public void upload(long ownerId, String localPath, String logicalPath) throws Exception {
        JsonObject json = Json.createObjectBuilder()
                .add("action", "upload")
                .add("ownerId", ownerId)
                .add("localFilePath", localPath)
                .add("logicalPath", logicalPath)
                .build();
        new MqttPubUI().send(json);
    }
    
    public void preUpdate(long fileId) throws Exception {
        JsonObject json = Json.createObjectBuilder()
                .add("action", "preUpdate")
                .add("fileId", fileId)
                .build();
        new MqttPubUI().send(json);
    }

    public void update(long fileId, String newLogical) throws Exception {
        JsonObject json = Json.createObjectBuilder()
                .add("action", "update")
                .add("fileId", fileId)
                .add("newLogicalPath", newLogical)
                .build();
        new MqttPubUI().send(json);
    }
    
    public void renameMove(long fileId, String newName, String newLogical) throws Exception {
        JsonObject json = Json.createObjectBuilder()
                .add("action", "renameMove")
                .add("fileId", fileId)
                .add("newName", newName)
                .add("newLogicalPath", newLogical)
                .build();
        new MqttPubUI().send(json);
    }

    public void delete(long fileId) throws Exception {
        JsonObject json = Json.createObjectBuilder()
                .add("action", "delete")
                .add("fileId", fileId)
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
        JsonObject json = Json.createObjectBuilder()
                .add("action", "share")
                .add("fileId", fileId)
                .add("targetId", targetId)
                .add("permission", permission)
                .build();
        new MqttPubUI().send(json);
    }

}
