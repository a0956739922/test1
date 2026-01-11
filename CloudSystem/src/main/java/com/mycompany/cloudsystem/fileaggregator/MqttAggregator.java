/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.cloudsystem.fileaggregator;

import java.io.StringReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.eclipse.paho.client.mqttv3.*;
/**
 *
 * @author ntu-user
 */
public class MqttAggregator {

    private static final String BROKER = "tcp://mqtt-broker:1883";
    private static final String LB_REQ = "/lb/request";
    private static final String AGG_RES = "/agg/response";
    private static final String CLIENT_ID = "AggregatorClient";
    private static final FileAggregator aggregator = new FileAggregator();
    private static final ExecutorService workers = Executors.newFixedThreadPool(4);
    public static void main(String[] args) {
        try {
            MqttClient client = new MqttClient(BROKER, CLIENT_ID);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            System.out.println("Connecting to broker: " + BROKER);
            client.connect(options);
            System.out.println("Connected");
            client.subscribe(LB_REQ, 1);
            System.out.println("Subscribed to " + LB_REQ);
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("Connection lost: " + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage msg) {
                    workers.submit(() -> {
                        try {
                            String payload = new String(msg.getPayload());
                            String responseContent = processRequest(payload);
                            MqttMessage responseMsg = new MqttMessage(responseContent.getBytes());
                            responseMsg.setQos(1);
                            client.publish(AGG_RES, responseMsg);
                            System.out.println("Sent: " + responseContent);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
                
                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
                
            });

            while (true) {
                Thread.sleep(1000);
            }

        } catch (MqttException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static String processRequest(String payload) {
        JsonObjectBuilder resBuilder = Json.createObjectBuilder();
        try {
            JsonObject req = Json.createReader(new StringReader(payload)).readObject();
            String reqId = req.getString("req_id", "unknown");
            String action = req.getString("action", "unknown");
            int group = req.getInt("target_group", 1);
            resBuilder.add("req_id", reqId).add("action", action).add("target_group", group);
            switch (action) {
                case "create" -> {
                    int fileId = aggregator.create(
                            group,
                            req.getJsonNumber("ownerId").intValue(),
                            req.getString("fileName"),
                            req.getString("content")
                    );
                    resBuilder.add("fileId", fileId);
                }
                case "download" -> {
                    String path = aggregator.download(
                            group,
                            req.getJsonNumber("ownerId").intValue(),
                            req.getJsonNumber("fileId").intValue(),
                            req.getString("fileName")
                    );
                    resBuilder.add("fileId", req.getJsonNumber("fileId").intValue()).add("remoteFilePath", path);
                }
                case "loadContent" -> {
                    String content = aggregator.loadContent(group, req.getJsonNumber("fileId").intValue());
                    resBuilder.add("fileId", req.getJsonNumber("fileId").intValue()).add("content", content);
                }
                case "update" -> {
                    aggregator.update(
                            group,
                            req.getJsonNumber("ownerId").intValue(),
                            req.getJsonNumber("fileId").intValue(),
                            req.containsKey("newName") ? req.getString("newName") : null,
                            req.containsKey("content") ? req.getString("content") : null
                    );
                    resBuilder.add("fileId", req.getJsonNumber("fileId").intValue());
                }
                case "delete" -> {
                    aggregator.delete(
                            group,
                            req.getJsonNumber("ownerId").intValue(),
                            req.getJsonNumber("fileId").intValue(),
                            req.getString("fileName")
                    );
                    resBuilder.add("fileId", req.getJsonNumber("fileId").intValue());
                }
                case "share" -> {
                    aggregator.share(
                            req.getJsonNumber("ownerId").intValue(),
                            req.getJsonNumber("fileId").intValue(),
                            req.getString("fileName"),
                            req.getJsonNumber("targetId").intValue(),
                            req.getString("targetUsername"),
                            req.getString("permission")
                    );
                    resBuilder
                            .add("fileId", req.getJsonNumber("fileId").intValue())
                            .add("targetUsername", req.getString("targetUsername"))
                            .add("permission", req.getString("permission"));
                }
                default -> throw new IllegalArgumentException("Unknown action: " + action);
            }
            resBuilder.add("status", "ok");
        } catch (Exception e) {
            resBuilder.add("status", "error").add("error", e.getMessage());
        }
        return resBuilder.build().toString();
    }
}