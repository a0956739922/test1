/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.mqtthost;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.json.Json;
import javax.json.JsonObject;
import org.eclipse.paho.client.mqttv3.*;
/**
 *
 * @author ntu-user
 */
public class MqttHost {

    private static final String BROKER    = "tcp://localhost:1883";
    private static final String LB_SCALE  = "/lb/scale";
    private static final String HOST_STATUS = "/host/status";
    private static final String CLIENT_ID = "HostManagerClient";

    private static final int MAX_CONTAINERS = 12;
    private static final List<String> VOLUMES =
            List.of("fs-vol-1", "fs-vol-2", "fs-vol-3", "fs-vol-4");

    private static final Map<String, String> volumeOwner = new ConcurrentHashMap<>();
    private static int nextContainerIndex = 1;

    public static void main(String[] args) {
        try {
            MqttClient client = new MqttClient(BROKER, CLIENT_ID);
            client.connect();
            System.out.println("[Host] Connected to MQTT broker");
            new Thread(() -> {
                System.out.println("[Host] Volume monitor started");
                while (true) {
                    try {
                        for (String volume : VOLUMES) {
                            String owner = volumeOwner.get(volume);
                            if (owner == null) continue;
                            if (!isContainerRunning(owner)) {
                                System.out.println("[Host] Volume owner DOWN: " + volume + " -> " + owner);
                                volumeOwner.remove(volume);
                                synchronized (MqttHost.class) {
                                    String name = "soft40051-files-container" + nextContainerIndex;
                                    System.out.println("[Host] Replacing volume owner: " + volume + " -> " + name);
                                    runContainerWithVolume(name, volume);
                                    volumeOwner.put(volume, name);
                                    nextContainerIndex++;
                                }
                            }
                        }
                        Thread.sleep(5000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            client.subscribe(LB_SCALE, 1);
            System.out.println("[Host] Subscribed to " + LB_SCALE);
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("[Host] Connection lost: " + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    if (LB_SCALE.equals(topic)) {JsonObject msg = Json.createReader(
                                new StringReader(new String(message.getPayload()))).readObject();
                        int groupSize = msg.getInt("group_size");
                        System.out.println("[Host] SCALE UP requested, groupSize=" + groupSize);
                        for (int i = 0; i < groupSize; i++) {
                            synchronized (MqttHost.class) {
                                if (nextContainerIndex > MAX_CONTAINERS) {
                                    System.out.println("[Host] Max container limit reached");
                                    break;
                                }
                                String volume = null;
                                for (String v : VOLUMES) {
                                    if (!volumeOwner.containsKey(v)) {
                                        volume = v;
                                        break;
                                    }
                                }
                                if (volume != null) {
                                    String name = "soft40051-files-container" + nextContainerIndex;
                                    runContainerWithVolume(name, volume);
                                    volumeOwner.put(volume, name);
                                    nextContainerIndex++;
                                } else {
                                    runContainerWithoutVolume();
                                }
                            }
                        }
                        JsonObject status = Json.createObjectBuilder().add("status", "scale_up_done").build();
                        MqttMessage statusMsg = new MqttMessage(status.toString().getBytes());
                        statusMsg.setQos(1);
                        client.publish(HOST_STATUS, statusMsg);
                        System.out.println("[Host] Scale-up completed, status sent");
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

    private static void runContainerWithoutVolume() {
        String name = "soft40051-files-container" + nextContainerIndex;
        System.out.println("[Host] Starting container (no volume): " + name);
        executeDockerCommand(
                "docker", "run", "-d", "--rm",
                "--name", name,
                "--network", "soft40051_network",
                "pedrombmachado/simple-ssh-container:base"
        );
        nextContainerIndex++;
    }

    private static void runContainerWithVolume(String name, String volume) {
        executeDockerCommand(
                "docker", "run", "-d", "--rm",
                "--name", name,
                "--network", "soft40051_network",
                "-v", volume + ":/home/ntu-user/data",
                "pedrombmachado/simple-ssh-container:base"
        );
    }

    private static boolean isContainerRunning(String name) {
        String output = executeDockerCommand(
                "docker", "ps",
                "--filter", "name=" + name,
                "--format", "{{.Names}}"
        );
        return output.contains(name);
    }

    private static String executeDockerCommand(String... command) {
        StringBuilder output = new StringBuilder();
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            Process p = pb.start();
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            p.waitFor();
        } catch (Exception e) {
            output.append("Error: ").append(e.getMessage());
        }
        return output.toString();
    }
}