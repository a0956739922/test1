/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.loadbalancer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.json.Json;
import javax.json.JsonObject;
import org.eclipse.paho.client.mqttv3.*;
/**
 *
 * @author ntu-user
 */
public class MqttLoadBalancer {

    private static final String BROKER = "tcp://mqtt-broker:1883";
    private static final String CLIENT_ID = "LoadBalancerClient";

    private static final String UI_REQ = "/request";
    private static final String LB_REQ = "/lb/request";
    private static final String HOST_STATUS = "/host/status";
    private static final String LB_SCALE = "/lb/scale";

    private static final LoadBalancer lb = new LoadBalancer();
    private static final Map<String, String> RAW = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        try {
            MqttClient client = new MqttClient(BROKER, CLIENT_ID);
            client.connect();
            lb.setDispatchHandler(r -> {
                try {
                    String raw = RAW.remove(r.id);
                    if (raw != null) {
                        client.publish(LB_REQ, new MqttMessage(raw.getBytes()));
                        System.out.println("[LB] Dispatched req_id=" + r.id);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            lb.setScaleUpHandler(groups -> {
                try {
                    JsonObject out = Json.createObjectBuilder().add("groups", groups).build();
                    client.publish(LB_SCALE, new MqttMessage(out.toString().getBytes()));
                    System.out.println("[LB] Published scale up, groups=" + groups);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            client.subscribe(UI_REQ);
            client.subscribe(HOST_STATUS);
            client.setCallback(new MqttCallback() {
                @Override
                public void messageArrived(String topic, MqttMessage msg) throws Exception {
                    String payload = new String(msg.getPayload());
                    if (UI_REQ.equals(topic)) {
                        JsonObject in = Json.createReader(
                                new java.io.StringReader(payload)).readObject();
                        String reqId = in.getString("req_id");
                        RAW.put(reqId, payload);
                        lb.receiveRequest(reqId);
                        lb.tick();
                    }

                    if (HOST_STATUS.equals(topic)) {
                        System.out.println("[LB] Host status: " + payload);
                        lb.tick();
                    }
                }

                @Override public void connectionLost(Throwable cause) {}
                @Override public void deliveryComplete(IMqttDeliveryToken token) {}
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}