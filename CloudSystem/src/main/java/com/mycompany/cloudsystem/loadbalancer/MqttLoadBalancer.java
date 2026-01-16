/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.cloudsystem.loadbalancer;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
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
    private static final String LB_SCALE = "/lb/scale";
    private static final String HOST_STATUS = "/host/status";

    private static final Map<Integer, String> groupStatus = new ConcurrentHashMap<>();
    private static int activeGroups = 0;
    private static int targetGroups = 0;
    private static long lastScaleUpTime = 0;
    private static int rrCursor = 0;
    private static int groupCursor = 0;
    private static Random random = new Random();
    private static final long SCALE_DOWN_COOLDOWN_MS = 60_000;

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
                        if (st.containsKey("active_groups")) {
                            activeGroups = st.getInt("active_groups", activeGroups);
                            lb.updateGroups(activeGroups);
                        }
                        if (st.containsKey("group") && st.containsKey("status")) {
                            int gid = st.getInt("group");
                            String status = st.getString("status");
                            groupStatus.put(gid, status);
                            System.out.println("[HealthCheck] group-" + gid + " -> " + status);
                        }
                        return;
                    }
                    if (!UI_REQ.equals(topic)) return;
                    JsonObject in = Json.createReader(new StringReader(payload)).readObject();
                    String reqId  = in.getString("req_id", "");
                    String action = in.getString("action", "");
                    if (activeGroups == 0 && targetGroups == 0) {
                        requestScale(client, 1);
                        System.out.println("[LB] Cold start triggered by action=" + action);
                    }
                    else if (activeGroups > 0) {
                        boolean hasUp = false;
                        boolean hasStarting = false;
                        for (String s : groupStatus.values()) {
                            if ("UP".equals(s)) {
                                hasUp = true;
                                break;
                            }
                            if ("STARTING".equals(s)) {
                                hasStarting = true;
                            }
                        }
                        if (!hasUp && !hasStarting) {
                            requestScale(client, activeGroups + 1);
                            System.out.println("[LB] No UP & no STARTING group, scale up triggered");
                        }
                    }
                    lb.addTask(reqId, action, 1 + random.nextInt(1000, 5000), payload);
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
                if (activeGroups == targetGroups) {
                    if (lb.scaleUp()) {
                        requestScale(client, activeGroups + 1);
                    }
                    else if (lb.scaleDown()) {
                        long now = System.currentTimeMillis();
                        if (now - lastScaleUpTime >= SCALE_DOWN_COOLDOWN_MS) {
                            requestScale(client, activeGroups - 1);
                        }
                    }
                }
                lb.processTasks();
                dispatchReady(client, lb);
                Thread.sleep(1500);
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
        List<Integer> upGroups = new ArrayList<>();
        for (Map.Entry<Integer, String> e : groupStatus.entrySet()) {
            if ("UP".equals(e.getValue())) {
                upGroups.add(e.getKey());
            }
        }
        if (upGroups.isEmpty()) return;
        Queue<Task> ready = lb.getReadyQueue();
        while (!ready.isEmpty()) {
            Task t = ready.poll();
            int groupId = upGroups.get(rrCursor % upGroups.size());
            rrCursor++;
            JsonObject original = Json.createReader(new StringReader(t.getPayload())).readObject();
            JsonObject routed = Json.createObjectBuilder(original).add("target_group", groupId).build();
            client.publish(LB_REQ, new MqttMessage(routed.toString().getBytes()));
            System.out.println("[LB] Dispatch req=" + t.getName() + " to group-" + groupId);
        }
    }

}