/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.mqttproject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
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
    private static final String AGG_TOPIC     = "/host/requests";
    private static final String CLIENT_ID     = "HostManagerClient";

    public static void main(String[] args) {
        try {
            MqttClient client = new MqttClient(BROKER_URL, CLIENT_ID);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            System.out.println("[Host] Connecting to MQTT broker...");
            client.connect(options);
            System.out.println("[Host] Connected!");
            client.subscribe(REQUEST_TOPIC, (topic, message) -> {
                String raw = new String(message.getPayload());
                System.out.println("[Host] Received request:");
                System.out.println(raw);
                JsonObject root = Json.createReader(new StringReader(raw)).readObject();
                JsonArray servers = root.getJsonArray("server");
                String running = executeDockerCommand("docker", "ps");
                for (JsonValue v : servers) {
                    String serverName = v.toString().replace("\"", "");
                    if (!running.contains(serverName)) {
                        System.out.println("[Host] Starting container: " + serverName);
                        executeDockerCommand("docker", "start", serverName);
                    } else {
                        System.out.println("[Host] Already running: " + serverName);
                    }
                }
                MqttMessage forward = new MqttMessage(raw.getBytes());
                forward.setQos(1);
                client.publish(AGG_TOPIC, forward);
                System.out.println("[Host] Forwarded request to Aggregator");
            });
            System.out.println("[Host] Listening on topic: " + REQUEST_TOPIC);
        } catch (Exception e) {
            e.printStackTrace();
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