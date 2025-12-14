/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.loadbalancer;

import org.eclipse.paho.client.mqttv3.*;
import java.io.StringReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
/**
 *
 * @author ntu-user
 */
public class MqttLoadBalancer {

    private static final String BROKER   = "tcp://mqtt-broker:1883";
    private static final String UI_REQ   = "/request";
    private static final String LB_HOST  = "/lb/host";
    private static final String LB_META  = "/lb/meta";
    private static final String LB_SCALE = "/lb/scale";
    private static final String CLIENT_ID = "LoadBalancerClient";
    private static final Map<String, String> RAW = new ConcurrentHashMap<>();
    private static LoadBalancer lb;

    public static void main(String[] args) {
        try {
            MqttClient client = new MqttClient(BROKER, CLIENT_ID);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            client.connect(options);
            lb = new LoadBalancer() {
                @Override
                protected void onScaleTriggered(int waiting, int capacity) {
                    try {
                        JsonObject msg = Json.createObjectBuilder()
                                .add("action", "scale_up")
                                .add("group_size", 4)
                                .build();
                        client.publish(LB_SCALE,new MqttMessage(msg.toString().getBytes()));
                        System.out.println("[LB SCALE] " + msg);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            System.out.println("[LB] Listening on " + UI_REQ);
            client.subscribe(UI_REQ, (topic, msg) -> onRequest(msg));
            while (true) {
                lb.getEmulator().step();
                forwardReady(client);
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void onRequest(MqttMessage msg) {
        try {
            String raw = new String(msg.getPayload());
            Request req = lb.acceptRaw(raw);
            RAW.put(req.getId(), raw);
            System.out.println("[LB] Queued " + req);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void forwardReady(MqttClient client) {
        try {
            var readyQueue = lb.getEmulator().getReadyQueue();
            while (!readyQueue.isEmpty()) {
                Request r = readyQueue.poll();
                String raw = RAW.remove(r.getId());
                if (r.getType() == Request.Type.META) {
                    client.publish(LB_META,new MqttMessage(raw.getBytes()));
                    System.out.println("[LB META] " + raw);
                } else {
                    JsonObject out = Json.createObjectBuilder()
                            .add("request",Json.createReader(new StringReader(raw)).readObject())
                            .build();
                    client.publish(LB_HOST,new MqttMessage(out.toString().getBytes()));
                    System.out.println("[LB HOST] " + out);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}