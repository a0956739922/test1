/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.mqttproject;

import com.mycompany.fileaggregator.FileService;
import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonObject;
import org.eclipse.paho.client.mqttv3.*;
/**
 *
 * @author ntu-user
 */
public class MqttAggregator {

    private static final String BROKER     = "tcp://mqtt-broker:1883";
    private static final String HOST_REQ   = "/host/requests";
    private static final String META_REQ   = "/lb/meta";
    private static final String UI_RES     = "/ui/results";
    private static final String CLIENT_ID  = "AggregatorClient";
    private static final FileService fileService = new FileService();

    public static void main(String[] args) {
        try {
            MqttClient client = new MqttClient(BROKER, CLIENT_ID);
            client.connect();
            System.out.println("[AGG] Listening on:");
            System.out.println("   " + HOST_REQ);
            System.out.println("   " + META_REQ);
            client.subscribe(HOST_REQ, (topic, msg) -> handleMessage(client, msg, true));
            client.subscribe(META_REQ, (topic, msg) -> handleMessage(client, msg, false));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleMessage(MqttClient client, MqttMessage msg, boolean fromHost) {
        try {
            String payload = new String(msg.getPayload());
            System.out.println("[AGG] Received: " + payload);
            JsonObject root = Json.createReader(new StringReader(payload)).readObject();
            JsonObject json;
            String serverName = null;
            if (fromHost) {
                serverName = root.getString("server");
                json = root.getJsonObject("request");
            } else {
                json = root;
            }
            String action = json.getString("action");
            switch (action) {
                case "upload":
                case "create": {
                    long ownerId = json.getJsonNumber("ownerId").longValue();
                    String localFile = json.getString("localFilePath");
                    String logicalPath = json.getString("logicalPath");
                    long fileId = fileService.upload(ownerId, localFile, logicalPath, serverName);
                    sendResult(client, action + "_success", fileId);
                    break;
                }
                case "update": {
                    long fileId = json.getJsonNumber("fileId").longValue();
                    String newLocal = json.getString("newLocalFilePath");
                    String newLogical = json.getString("newLogicalPath");
                    fileService.update(fileId, newLocal, newLogical, serverName);
                    sendResult(client, "update_success", fileId);
                    break;
                }
                case "download": {
                    long fileId = json.getJsonNumber("fileId").longValue();
                    String out = json.getString("outputDir");
                    String resultPath = fileService.download(fileId, out);
                    sendResult(client, "download_success", resultPath);
                    break;
                }
                case "delete": {
                    long fileId = json.getJsonNumber("fileId").longValue();
                    fileService.delete(fileId);
                    sendResult(client, "delete_success", fileId);
                    break;
                }
                case "share": {
                    long fileId = json.getJsonNumber("fileId").longValue();
                    long ownerId = json.getJsonNumber("ownerId").longValue();
                    long targetId = json.getJsonNumber("targetId").longValue();
                    String perm = json.getString("permission");
                    fileService.share(fileId, ownerId, targetId, perm);
                    sendResult(client, "share_success", fileId);
                    break;
                }
                case "renameMove": {
                    long fileId = json.getJsonNumber("fileId").longValue();
                    String newName = json.getString("newName");
                    String newLogical = json.getString("newLogicalPath");
                    fileService.renameMove(fileId, newName, newLogical);
                    sendResult(client, "rename_success", fileId);
                    break;
                }
                default:
                    sendError(client, "Unknown action: " + action);
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendError(client, e.getMessage());
        }
    }

    private static void sendResult(MqttClient client, String type, Object data) {
        try {
            JsonObject result = Json.createObjectBuilder()
                    .add("status", "ok")
                    .add("type", type)
                    .add("data", data.toString())
                    .build();
            client.publish(UI_RES, new MqttMessage(result.toString().getBytes()));
            System.out.println("[AGG] Sent result → UI");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static void sendError(MqttClient client, String msg) {
        try {
            JsonObject result = Json.createObjectBuilder()
                    .add("status", "error")
                    .add("message", msg)
                    .build();
            client.publish(UI_RES, new MqttMessage(result.toString().getBytes()));
            System.out.println("[AGG] Sent ERROR → UI");
        } catch (Exception e) { e.printStackTrace(); }
    }
}