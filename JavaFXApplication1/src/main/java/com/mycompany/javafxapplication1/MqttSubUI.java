/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
/**
 *
 * @author ntu-user
 */
public class MqttSubUI {

    private static final String BROKER = "tcp://mqtt-broker:1883";
    private static final String AGG_RES = "/agg/response";
    private static final String CLIENT_ID = "UIClientSub";
    public static volatile String lastResultJson;
    private MqttClient client;

    public void start() throws Exception {
        client = new MqttClient(BROKER, CLIENT_ID);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        client.connect(options);
        client.subscribe(AGG_RES, (topic, msg) -> {
            String result = new String(msg.getPayload());
            System.out.println("Received from " + topic);
            lastResultJson = result;
            System.out.println(result);
        });
        System.out.println("[UI] Subscribed (long-lived) to " + AGG_RES);
    }
}