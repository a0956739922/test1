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

    public static void main(String[] args) throws Exception {
        MqttClient client = new MqttClient(BROKER, CLIENT_ID);
        client.connect();
        client.subscribe(LB_REQ, 1);
        client.setCallback(new MqttCallback() {
            public void connectionLost(Throwable cause) {
            }
            public void deliveryComplete(IMqttDeliveryToken token) {
            }

            public void messageArrived(String topic, MqttMessage msg) {
                try {
                    JsonObject req = Json.createReader(new StringReader(new String(msg.getPayload()))).readObject();
                    String reqId = req.getString("req_id");
                    String action = req.getString("action");
                    JsonObjectBuilder result = Json.createObjectBuilder();
                    switch (action) {
                        case "create" -> result.add("fileId",
                                aggregator.create(
                                        req.getJsonNumber("ownerId").longValue(),
                                        req.getString("fileName"),
                                        req.getString("logicalPath"),
                                        req.getString("content")
                                )
                        );
                        case "download" -> result.add("remoteFilePath",
                                aggregator.download(req.getJsonNumber("fileId").longValue())
                        );
                        case "loadContent" -> result.add("content",
                                aggregator.loadContent(req.getJsonNumber("fileId").longValue())
                        );
                        case "update" -> aggregator.update(
                                req.getJsonNumber("fileId").longValue(),
                                req.containsKey("newName") ? req.getString("newName") : null,
                                req.containsKey("newLogicalPath") ? req.getString("newLogicalPath") : null,
                                req.containsKey("content") ? req.getString("content") : null
                        );

                        case "delete" -> aggregator.delete(
                                req.getJsonNumber("fileId").longValue()
                        );

                        case "share" -> aggregator.share(
                                req.getJsonNumber("fileId").longValue(),
                                req.getJsonNumber("ownerId").longValue(),
                                req.getJsonNumber("targetId").longValue(),
                                req.getString("permission")
                        );
                    }
                    JsonObject res = Json.createObjectBuilder()
                        .add("req_id", reqId)
                        .add("status", "ok")
                        .addAll(result)
                        .build();
                    client.publish(AGG_RES, new MqttMessage(res.toString().getBytes()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        while (true) Thread.sleep(1000);
    }
}