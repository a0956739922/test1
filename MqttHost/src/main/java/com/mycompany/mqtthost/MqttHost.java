/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.mqtthost;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import org.eclipse.paho.client.mqttv3.*;
/**
 *
 * @author ntu-user
 */
public class MqttHost {
    
    private static final String BROKER_URL   = "tcp://localhost:1883";
    private static final String REQUEST_TOPIC = "/lb/host";
    private static final String SCALE_TOPIC   = "/lb/scale";
    private static final String AGG_TOPIC     = "/host/requests";
    private static final String CLIENT_ID     = "HostManagerClient";
    private static int nextContainerIndex = 5;

    public static void main(String[] args) {
        try {
            MqttClient client = new MqttClient(BROKER_URL, CLIENT_ID);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            client.connect(options);
            System.out.println("[Host] Connected to MQTT broker");
             client.subscribe(REQUEST_TOPIC, (topic, message) -> {
                System.out.println("[Host] Received file request");
                ensureBaselineContainers();
                MqttMessage forward = new MqttMessage(message.getPayload());
                forward.setQos(1);
                client.publish(AGG_TOPIC, forward);
                System.out.println("[Host] Forwarded request to Aggregator");
            });
            client.subscribe(SCALE_TOPIC, (topic, message) -> {
                JsonObject msg =Json.createReader(new StringReader(new String(message.getPayload()))).readObject();
                int groupSize = msg.getInt("group_size");
                System.out.println("[Host] SCALE UP requested, groupSize=" + groupSize);
                for (int i = 0; i < groupSize; i++) {
                    String name = "soft40051-files-container" + nextContainerIndex;
                    System.out.println("[Host] Starting container: " + name);
                    executeDockerCommand("docker", "start", name);
                    nextContainerIndex++;
                }
            });
            System.out.println("[Host] Listening on:");
            System.out.println(" - " + REQUEST_TOPIC);
            System.out.println(" - " + SCALE_TOPIC);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static Set<String> getRunningContainers() {
        Set<String> result = new HashSet<>();
        try {
            ProcessBuilder pb =new ProcessBuilder("docker", "ps", "--format", "{{.Names}}");
            Process p = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                result.add(line.trim());
            }
            p.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
    
    private static void ensureBaselineContainers() {
        Set<String> running = getRunningContainers();
        for (int i = 1; i <= 4; i++) {
            String name = "soft40051-files-container" + i;
            if (!running.contains(name)) {
                System.out.println("[Host] Baseline missing, starting: " + name);
                executeDockerCommand("docker", "start", name);
            } else {
                System.out.println("[Host] Baseline OK: " + name);
            }
        }
    }

    private static String executeDockerCommand(String... command) {
        StringBuilder output = new StringBuilder();
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            process.waitFor();
        } catch (Exception e) {
            output.append("Error executing docker command: ").append(e.getMessage());
        }
        return output.toString();
    }
}