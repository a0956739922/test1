/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.fileaggregator;

import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonArray;
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
    private static final String CLIENT_ID  = "AggregatorClient";
    private static final FileAggregator aggregator = new FileAggregator();

    public static void main(String[] args) {
        try {
            MqttClient client = new MqttClient(BROKER, CLIENT_ID);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            System.out.println("[AGG] Connecting to MQTT broker...");
            client.connect(options);
            System.out.println("[AGG] Connected.");
            System.out.println("[AGG] Listening on:");
            System.out.println("   " + HOST_REQ);
            System.out.println("   " + META_REQ);
            client.subscribe(HOST_REQ, (topic, msg) -> handleMessage(msg));
            client.subscribe(META_REQ, (topic, msg) -> handleMessage(msg));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleMessage(MqttMessage msg) {
        try {
            String raw = new String(msg.getPayload());
            JsonObject root = Json.createReader(new StringReader(raw)).readObject();
            JsonObject request = root.getJsonObject("request");
            aggregator.acceptRaw(request.toString());
            System.out.println("[AGG] Request processed successfully");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[AGG] Error handling request: " + e.getMessage());
        }
    }
}