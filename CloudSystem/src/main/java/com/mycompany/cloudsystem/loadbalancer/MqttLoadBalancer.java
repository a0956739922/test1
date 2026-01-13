/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.cloudsystem.loadbalancer;

import java.io.StringReader;
import java.util.Queue;
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
    private static final String LB_SCALE = "/lb/scale";
    private static final String HOST_STATUS = "/host/status";

    private static int activeGroups = 0;
    private static int targetGroups = 0;
    private static long lastScaleUpTime = 0;
    private static final long SCALE_DOWN_COOLDOWN_MS = 60_000;
    private static String pendingLoadContent = null;

    public static void main(String[] args) {

        TrafficEmulator lb = new TrafficEmulator();

        try {
            MqttClient client = new MqttClient(BROKER, CLIENT_ID);
            client.connect();
            client.subscribe(UI_REQ);
            client.subscribe(HOST_STATUS);
            System.out.println("[LB] Connected & subscribed");

            client.setCallback(new MqttCallback() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String payload = new String(message.getPayload());
                    if (HOST_STATUS.equals(topic)) {
                        JsonObject st = Json.createReader(new StringReader(payload)).readObject();
                        activeGroups = st.getInt("active_groups", activeGroups);
                        lb.updateGroups(activeGroups);
                        lb.processTasks();
                        if (activeGroups > 0 && pendingLoadContent != null) {
                            client.publish(LB_REQ, new MqttMessage(pendingLoadContent.getBytes()));
                            pendingLoadContent = null;
                            System.out.println("[LB] Dispatch pending loadContent after cold start");
                        }
                        return;
                    }
                    if (!UI_REQ.equals(topic)) return;
                    JsonObject in = Json.createReader(new StringReader(payload)).readObject();
                    String reqId  = in.getString("req_id", "");
                    String action = in.getString("action", "");
                    if ("loadContent".equals(action)) {
                        if (activeGroups == 0 && targetGroups == 0) {
                            requestScale(client, 1);
                            pendingLoadContent = payload;
                            System.out.println("[LB] Cold start for loadContent");
                            return;
                        }
                        client.publish(LB_REQ, new MqttMessage(payload.getBytes()));
                        return;
                    }
                    if (activeGroups == 0 && targetGroups == 0) {
                        requestScale(client, 1);
                        System.out.println("[LB] Cold start triggered by action=" + action);
                    }
                    lb.addTask(reqId, action, 1 + (int)(Math.random() * 5), payload);
                }

                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("[LB] Connection lost: " + cause.getMessage());
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });

            while (true) {
                boolean scaling = (activeGroups != targetGroups);
                if (!scaling) {
                    if (lb.scaleUp()) {
                        requestScale(client, activeGroups + 1);
                        continue;
                    }
                    if (lb.scaleDown()) {
                        long now = System.currentTimeMillis();
                        if (now - lastScaleUpTime < SCALE_DOWN_COOLDOWN_MS) {
                            continue;
                        }
                        requestScale(client, activeGroups - 1);
                        continue;
                    }
                }
                lb.processTasks();
                dispatchReady(client, lb);
                Thread.sleep(2000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void requestScale(MqttClient client, int target) throws Exception {
        if (target > activeGroups) {
            lastScaleUpTime = System.currentTimeMillis();
        }
        targetGroups = target;
        JsonObject scale = Json.createObjectBuilder()
                .add("action", target > activeGroups ? "scale_up" : "scale_down")
                .add("groups", target)
                .build();
        client.publish(LB_SCALE, new MqttMessage(scale.toString().getBytes()));
        System.out.println("[LB] Request scale groups=" + target);
    }

    private static void dispatchReady(MqttClient client, TrafficEmulator lb) throws Exception {
        Queue<Task> ready = lb.getReadyQueue();
        while (!ready.isEmpty()) {
            Task t = ready.poll();
            int groupId = lb.getGroupOfTask(t.getName());
            JsonObject original = Json.createReader(new StringReader(t.getPayload())).readObject();
            JsonObject routed = Json.createObjectBuilder(original).add("target_group", groupId).build();
            client.publish(LB_REQ, new MqttMessage(routed.toString().getBytes()));
            System.out.println("[LB] Dispatch req=" + t.getName() + " to group-" + groupId);
        }
    }
}