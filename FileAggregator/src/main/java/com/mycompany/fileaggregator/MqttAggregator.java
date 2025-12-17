/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.fileaggregator;

import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonObject;
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
                public void messageArrived(String topic, MqttMessage msg) throws Exception {
                    if (LB_REQ.equals(topic)) {
                        String raw = new String(msg.getPayload());
                        JsonObject request = Json.createReader(new StringReader(raw)).readObject();
                        JsonObject result = aggregator.acceptRaw(request.toString());
                        MqttMessage resMsg = new MqttMessage(result.toString().getBytes());
                        resMsg.setQos(1);
                        client.publish(AGG_RES, resMsg);
                        System.out.println("[AGG] Result published to " + AGG_RES);
                        System.out.println("[AGG] Request processed successfully");
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