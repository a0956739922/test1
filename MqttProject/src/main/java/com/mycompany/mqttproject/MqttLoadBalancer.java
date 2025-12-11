/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.mqttproject;

import com.mycompany.loadbalancer.LoadBalancer;
import com.mycompany.loadbalancer.Request;
import org.eclipse.paho.client.mqttv3.*;
import java.io.StringReader;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.json.Json;
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
    private static final String CLIENT_ID = "LoadBalancerClient";

    private static final Map<String, String> RAW = new ConcurrentHashMap<>();
    private static LoadBalancer lb;

    public static void main(String[] args) {
        try {
            lb = new LoadBalancer();
            MqttClient client = new MqttClient(BROKER, CLIENT_ID);
            client.connect();
            System.out.println("[LB] Listening on:");
            System.out.println("   " + UI_REQ);
            client.subscribe(UI_REQ, (topic, msg) -> handleMessage(msg));
            while (true) {
                lb.getEmulator().step();
                forwardReady(client);
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleMessage(MqttMessage msg) {
        try {
            String raw = new String(msg.getPayload());
            JsonObject json = Json.createReader(new StringReader(raw)).readObject();
            String action = json.getString("action");
            long size = json.getJsonNumber("sizeBytes").longValue();
            String reqId = UUID.randomUUID().toString();
            Request req;
            switch (action) {
                case "upload":
                case "create":
                case "update":
                    req = new Request(reqId, Request.Type.UPLOAD, size);
                    break;
                case "download":
                    req = new Request(reqId, Request.Type.DOWNLOAD, size);
                    break;
                case "delete":
                    req = new Request(reqId, Request.Type.DELETE, size);
                    break;
                case "share":
                case "renameMove":
                    req = new Request(reqId, Request.Type.META, size);
                    break;
                default:
                    System.out.println("[LB] Unknown action: " + action);
                    return;
            }

            RAW.put(reqId, raw);
            lb.submitRequest(req);
            System.out.println("[LB] Queued → " + req);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void forwardReady(MqttClient client) {
        try {
            var ready = lb.getEmulator().getReadyQueue();
            while (!ready.isEmpty()) {
                Request r = ready.poll();
                String raw = RAW.remove(r.getId());
                switch (r.getType()) {
                    case META:
                        client.publish(LB_META, new MqttMessage(raw.getBytes()));
                        System.out.println("[LB → AGG META] " + raw);
                        break;
                    default:
                        int index = r.getAssignedServer();
                        String serverName = "soft40051-files-container" + (index + 1);
                        JsonObject out = Json.createObjectBuilder()
                                .add("server", serverName)
                                .add("request", Json.createReader(new StringReader(raw)).readObject())
                                .build();
                        client.publish(LB_HOST, new MqttMessage(out.toString().getBytes()));
                        System.out.println("[LB → HOST] " + out);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}