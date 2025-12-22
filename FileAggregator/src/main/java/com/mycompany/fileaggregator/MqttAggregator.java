/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.fileaggregator;

import java.io.StringReader;
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

    public static void main(String[] args) {
        try {
            MqttClient client = new MqttClient(BROKER, CLIENT_ID);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            client.connect(options);
            System.out.println("[AGG] Connected to broker");
            client.subscribe(LB_REQ, 1);
            System.out.println("[AGG] Subscribed to " + LB_REQ);

            client.setCallback(new MqttCallback() {

                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("[AGG] Connection lost: " + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage msg) {
                    try {
                        String payload = new String(msg.getPayload());
                        JsonObject req = Json.createReader(new StringReader(payload)).readObject();
                        String reqId = req.getString("req_id");
                        String action = req.getString("action");
                        JsonObjectBuilder result = Json.createObjectBuilder();
                        switch (action) {
                            case "create": {
                                long fileId = aggregator.create(
                                        req.getJsonNumber("ownerId").longValue(),
                                        req.getString("fileName"),
                                        req.getString("logicalPath"),
                                        req.getString("content")
                                );
                                result.add("fileId", fileId);
                                break;
                            }
                            case "upload": {
                                long fileId = aggregator.upload(
                                        req.getJsonNumber("ownerId").longValue(),
                                        req.getString("localFilePath"),
                                        req.getString("fileName"),
                                        req.getString("logicalPath")
                                );
                                result.add("fileId", fileId);
                                break;
                            }
                            case "download": {
                                String path = aggregator.download(req.getJsonNumber("fileId").longValue());
                                result.add("remoteFilePath", path);
                                break;
                            }
                            case "loadContent": {
                                String content = aggregator.loadContent(req.getJsonNumber("fileId").longValue()
                                );
                                result.add("content", content);
                                break;
                            }
                            case "update": {
                                aggregator.update(
                                        req.getJsonNumber("fileId").longValue(),
                                        req.containsKey("newName") ? req.getString("newName") : null,
                                        req.containsKey("newLogicalPath") ? req.getString("newLogicalPath") : null,
                                        req.containsKey("content") ? req.getString("content") : null
                                );
                                result.add("ok", true);
                                break;
                            }
                            case "delete": {
                                aggregator.delete(req.getJsonNumber("fileId").longValue());
                                result.add("ok", true);
                                break;
                            }
                            case "share": {
                                aggregator.share(
                                        req.getJsonNumber("fileId").longValue(),
                                        req.getJsonNumber("ownerId").longValue(),
                                        req.getJsonNumber("targetId").longValue(),
                                        req.getString("permission")
                                );
                                result.add("ok", true);
                                break;
                            }
                            default:
                                throw new IllegalArgumentException("Unknown action: " + action);
                        }
                        JsonObject response = Json.createObjectBuilder()
                                .add("req_id", reqId)
                                .add("status", "ok")
                                .addAll(result)
                                .build();
                        MqttMessage resMsg = new MqttMessage(response.toString().getBytes());
                        resMsg.setQos(1);
                        client.publish(AGG_RES, resMsg);
                        System.out.println("[AGG] published response for req_id=" + reqId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {

                }
            });

            while (true) {
                Thread.sleep(1000);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}