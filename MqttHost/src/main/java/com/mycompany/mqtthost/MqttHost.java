/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.mqtthost;

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
                String raw = new String(message.getPayload());
                System.out.println("[Host] Received file request");
                System.out.println(raw);
                MqttMessage forward = new MqttMessage(raw.getBytes());
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