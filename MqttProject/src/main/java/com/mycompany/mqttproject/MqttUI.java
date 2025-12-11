/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.mqttproject;

import org.eclipse.paho.client.mqttv3.*;
import java.io.StringReader;
import java.util.UUID;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
/**
 *
 * @author ntu-user
 */
public class MqttUI {

    private static final String BROKER   = "tcp://mqtt-broker:1883";
    private static final String UI_REQ   = "/request";
    private static final String UI_RES   = "/ui/results";

    private final MqttClient client;
    private volatile ResultListener listener;

    public MqttUI() throws MqttException {
        String clientId = "UIClient-" + UUID.randomUUID().toString();
        this.client = new MqttClient(BROKER, clientId);
        this.client.connect();
        this.client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                handleResultMessage(message);
            }

            @Override
            public void deliveryComplete(org.eclipse.paho.client.mqttv3.IMqttDeliveryToken token) {
            }
        });
        this.client.subscribe(UI_RES);
    }

    public interface ResultListener {
        void onResult(JsonObject result);
    }

    public void setResultListener(ResultListener listener) {
        this.listener = listener;
    }

    private void handleResultMessage(MqttMessage msg) {
        try {
            String payload = new String(msg.getPayload());
            JsonReader reader = Json.createReader(new StringReader(payload));
            JsonObject json = reader.readObject();
            reader.close();
            ResultListener l = this.listener;
            if (l != null) {
                l.onResult(json);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void send(JsonObject json) throws MqttException {
        MqttMessage msg = new MqttMessage(json.toString().getBytes());
        client.publish(UI_REQ, msg);
    }

    public void sendCreate(long ownerId, String localFilePath, String logicalPath, long sizeBytes) throws MqttException {
        JsonObject json = Json.createObjectBuilder()
                .add("action", "create")
                .add("ownerId", ownerId)
                .add("localFilePath", localFilePath)
                .add("logicalPath", logicalPath)
                .add("sizeBytes", sizeBytes)
                .build();
        send(json);
    }

    public void sendUpload(long ownerId, String localFilePath, String logicalPath, long sizeBytes) throws MqttException {
        JsonObject json = Json.createObjectBuilder()
                .add("action", "upload")
                .add("ownerId", ownerId)
                .add("localFilePath", localFilePath)
                .add("logicalPath", logicalPath)
                .add("sizeBytes", sizeBytes)
                .build();
        send(json);
    }

    public void sendUpdate(long fileId, String newLocalFilePath, String newLogicalPath, long sizeBytes) throws MqttException {
        JsonObject json = Json.createObjectBuilder()
                .add("action", "update")
                .add("fileId", fileId)
                .add("newLocalFilePath", newLocalFilePath)
                .add("newLogicalPath", newLogicalPath)
                .add("sizeBytes", sizeBytes)
                .build();
        send(json);
    }

    public void sendDelete(long fileId, long sizeBytes) throws MqttException {
        JsonObject json = Json.createObjectBuilder()
                .add("action", "delete")
                .add("fileId", fileId)
                .add("sizeBytes", sizeBytes)
                .build();
        send(json);
    }

    public void sendDownload(long fileId, String outputDir, long sizeBytes) throws MqttException {
        JsonObject json = Json.createObjectBuilder()
                .add("action", "download")
                .add("fileId", fileId)
                .add("outputDir", outputDir)
                .add("sizeBytes", sizeBytes)
                .build();
        send(json);
    }

    public void sendShare(long fileId, long ownerId, long targetId, String permission, long sizeBytes) throws MqttException {
        JsonObject json = Json.createObjectBuilder()
                .add("action", "share")
                .add("fileId", fileId)
                .add("ownerId", ownerId)
                .add("targetId", targetId)
                .add("permission", permission)
                .add("sizeBytes", sizeBytes)
                .build();
        send(json);
    }

    public void sendRenameMove(long fileId, String newName, String newLogicalPath, long sizeBytes) throws MqttException {
        JsonObject json = Json.createObjectBuilder()
                .add("action", "renameMove")
                .add("fileId", fileId)
                .add("newName", newName)
                .add("newLogicalPath", newLogicalPath)
                .add("sizeBytes", sizeBytes)
                .build();
        send(json);
    }

    public void close() {
        try {
            if (client.isConnected()) {
                client.disconnect();
            }
            client.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
