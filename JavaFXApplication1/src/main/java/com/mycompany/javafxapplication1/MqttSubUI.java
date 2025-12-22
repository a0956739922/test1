/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

import java.io.StringReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.json.Json;
import javax.json.JsonObject;
import org.eclipse.paho.client.mqttv3.*;
/**
 *
 * @author ntu-user
 */
public class MqttSubUI {

    private static final String BROKER = "tcp://mqtt-broker:1883";
    private static final String AGG_RES = "/agg/response";
    private static final String CLIENT_ID = "UIClientSub";

    public static final Map<String, String> RESULTS = new ConcurrentHashMap<>();

    private MqttClient client;

    public void start() {
        try {
            client = new MqttClient(BROKER, CLIENT_ID);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            client.connect(options);
            System.out.println("[UI] Connected to broker");
            client.subscribe(AGG_RES, 1);
            System.out.println("[UI] Subscribed to " + AGG_RES);
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("[UI] Connection lost: " + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage msg) {
                    System.out.println("[UI] messageArrived topic=" + topic);
                    try {
                        String payload = new String(msg.getPayload());
                        System.out.println("[UI] payload=" + payload);
                        JsonObject res = Json.createReader(new StringReader(payload)).readObject();
                        String reqId = res.getString("req_id", "");
                        RESULTS.put(reqId, payload);
                        System.out.println("[UI] result stored for req_id=" + reqId);
                    } catch (Exception e) {
                        System.out.println("[UI] EXCEPTION inside messageArrived");
                        e.printStackTrace();
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                client.close();
                System.out.println("[UI] MQTT disconnected");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}