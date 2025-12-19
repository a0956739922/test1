/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

import javax.json.Json;
import javax.json.JsonObject;
import org.eclipse.paho.client.mqttv3.*;
/**
 *
 * @author ntu-user
 */
public class MqttPubUI {

    private static final String BROKER    = "tcp://mqtt-broker:1883";
    private static final String CLIENT_ID = "UIClientPub";
    private static final String UI_REQ    = "/request";

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
        System.out.println("[UI] Disconnected.");
    }
}