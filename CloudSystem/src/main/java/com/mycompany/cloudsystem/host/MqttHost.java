/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.cloudsystem.host;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonObject;
import org.eclipse.paho.client.mqttv3.*;
/**
 *
 * @author ntu-user
 */
public class MqttHost {

    private static final String BROKER = "tcp://localhost:1883";
    private static final String CLIENT_ID = "HostManagerClient";
    private static final String LB_SCALE   = "/lb/scale";
    private static final String HOST_STATUS = "/host/status";
    
    private static final Map<Integer, List<String>> groupContainers = new HashMap<>();
    private static final int GROUP_SIZE = 4;
    private static final int MAX_CONTAINERS = 12;
    private static int activeGroups = 0;
    private static int nextContainerId = 1;

    private static final List<String> VOLUMES = List.of("fs-vol-1", "fs-vol-2", "fs-vol-3", "fs-vol-4");

    public static void main(String[] args) {
        try {
            MqttClient client = new MqttClient(BROKER, CLIENT_ID);
            client.connect();
            client.subscribe(LB_SCALE);
            System.out.println("[HOST] Listening on " + LB_SCALE);
            client.setCallback(new MqttCallback() {

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    JsonObject cmd = Json.createReader(new StringReader(new String(message.getPayload()))).readObject();
                    String action = cmd.getString("action", "scale_up");
                    int targetGroups = cmd.getInt("groups", activeGroups);
                    if ("scale_up".equals(action)) {
                        scaleUpTo(targetGroups);
                    } else if ("scale_down".equals(action)) {
                        scaleDownTo(targetGroups);
                    }
                    publishStatus(client);
                }

                @Override public void connectionLost(Throwable cause) {
                    System.out.println("[LB] Connection lost: " + cause.getMessage());
                }
                
                @Override public void deliveryComplete(IMqttDeliveryToken token) {
                }
                
            });
            while (true) {
                try {
                    publishStatus(client);
                    Thread.sleep(3000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void scaleUpTo(int targetGroups) {
        while (activeGroups < targetGroups && nextContainerId <= MAX_CONTAINERS) {
            int groupId = activeGroups + 1;
            System.out.println("[HOST] SCALE UP group " + groupId);
            List<String> containers = new java.util.ArrayList<>();
            for (int i = 0; i < GROUP_SIZE; i++) {
                String name = "soft40051-files-container" + nextContainerId;
                int volumeIndex = (nextContainerId - 1) % VOLUMES.size();
                String volume = VOLUMES.get(volumeIndex);
                dockerRun(name, volume);
                containers.add(name);
                nextContainerId++;
            }
            groupContainers.put(groupId, containers);
            activeGroups++;
        }
    }

    private static void scaleDownTo(int targetGroups) {
        while (activeGroups > targetGroups) {
            System.out.println("[HOST] SCALE DOWN group " + activeGroups);
            for (int i = 0; i < GROUP_SIZE; i++) {
                int id = --nextContainerId;
                dockerStop("soft40051-files-container" + id);
            }
            activeGroups--;
        }
    }

    private static void publishStatus(MqttClient client) throws Exception {
        client.publish(HOST_STATUS, new MqttMessage(Json.createObjectBuilder().add("active_groups", activeGroups).build().toString().getBytes()));
        for (int groupId = 1; groupId <= activeGroups; groupId++) {
            boolean groupHealthy = true;
            List<String> containers = groupContainers.get(groupId);
            if (containers == null) {
                groupHealthy = false;
            } else {
                for (String c : containers) {
                    boolean running = isContainerRunning(c);
                    if (!running) {
                        groupHealthy = false;
                        break;
                    }
                }
            }
            JsonObject health = Json.createObjectBuilder().add("group", groupId).add("status", groupHealthy ? "UP" : "DOWN").build();
            client.publish(HOST_STATUS, new MqttMessage(health.toString().getBytes()));
            System.out.println("[HealthCheck] group-" + groupId + " -> " + (groupHealthy ? "UP" : "DOWN"));
        }
    }
    
    private static boolean isContainerRunning(String name) {
        try {
            Process p = new ProcessBuilder("docker", "inspect", "-f", "{{.State.Running}}", name).start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = r.readLine();
            p.waitFor();
            return "true".equalsIgnoreCase(line);
        } catch (Exception e) {
            return false;
        }
    }

    private static void dockerRun(String name, String volume) {
        exec(new ProcessBuilder(
            "docker", "run", "-d", "--rm",
            "--name", name,
            "--network", "soft40051_network",
            "-v", volume + ":/home/ntu-user/data",
            "pedrombmachado/simple-ssh-container:base"
        ));
    }

    private static void dockerStop(String name) {
        exec(new ProcessBuilder("docker", "stop", name));
    }

    private static void exec(ProcessBuilder pb) {
        try {
            Process p = pb.start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while (r.readLine() != null) {}
            p.waitFor();
        } catch (Exception ignore) {}
    }
}