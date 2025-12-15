/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

import org.eclipse.paho.client.mqttv3.*;
import javax.json.JsonObject;
/**
 *
 * @author ntu-user
 */
public class MqttUI {

    private static final String BROKER = "tcp://mqtt-broker:1883";
    private static final String UI_REQ = "/request";
    private static final String AGG_RES = "/agg/response";
    private static final String CLIENT_ID = "UIClient";

    public void send(JsonObject json) throws Exception {
        MqttClient client = new MqttClient(BROKER, CLIENT_ID);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        System.out.println("[UI] Connecting to MQTT broker...");
        client.connect(options);
        MqttMessage message = new MqttMessage(json.toString().getBytes());
        message.setQos(1);
        client.publish(UI_REQ, message);
        System.out.println("[UI] Published action: " + json.getString("action"));
        client.disconnect();
        client.close();
        System.out.println("[UI] Disconnected.");
    }
    
    public void requestClient(JsonObject json) throws Exception {
        MqttClient client = new MqttClient(BROKER, CLIENT_ID);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        client.connect(options);
        client.subscribe(AGG_RES, (topic, msg) -> {
            String result = new String(msg.getPayload());
            System.out.println("Received from " + topic);
            System.out.println(result);
            try {
                client.disconnect();
                System.out.println("Disconnected.");
            } catch (MqttException e) {
                e.printStackTrace();
            }
        });
        MqttMessage message = new MqttMessage(json.toString().getBytes());
        message.setQos(1);
        client.publish(UI_REQ, message);
        System.out.println("Request published.");
    }
}