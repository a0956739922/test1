/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.mqttproject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import org.eclipse.paho.client.mqttv3.*;
/**
 *
 * @author ntu-user
 */
public class MqttHost {

    private static final String BROKER = "tcp://localhost:1883";
    private static final String LB_HOST_REQ = "/lb/host";
    private static final String HOST_STATUS = "/host/status";
    private static final String AGG_REQ = "/host/requests";
    private static final String CLIENT_ID = "HostManagerClient";
    private static final String[] SERVERS = {
        "soft40051-files-container1",
        "soft40051-files-container2",
        "soft40051-files-container3",
        "soft40051-files-container4"
    };

    public static void main(String[] args) {
        try {
            MqttClient client = new MqttClient(BROKER, CLIENT_ID);
            client.connect();
            client.subscribe(LB_HOST_REQ, (topic, message) -> {
                try {
                    String payload = new String(message.getPayload());
                    JsonObject json = Json.createReader(new StringReader(payload)).readObject();
                    String action = json.getString("action");
                    switch (action) {
                        case "startServer": {
                            String server = json.getString("server");
                            String result = executeDocker("docker", "start", server);
                            publishStatus(client, "startServer", server, result);
                            break;
                        }
                        case "stopServer": {
                            String server = json.getString("server");
                            String result = executeDocker("docker", "stop", server);
                            publishStatus(client, "stopServer", server, result);
                            break;
                        }
                        case "listServers": {
                            String result = executeDocker("docker", "ps");
                            publishStatus(client, "listServers", "all", result);
                            break;
                        }
                        case "getActiveServers": {
                            String ps = executeDocker("docker", "ps");
                            JsonArrayBuilder activeList = Json.createArrayBuilder();
                            for (String s : SERVERS) {
                                if (ps.contains(s)) activeList.add(s);
                            }
                            JsonObject jsonMsg = Json.createObjectBuilder()
                                    .add("status", "ok")
                                    .add("type", "activeServers")
                                    .add("servers", activeList.build())
                                    .build();
                            client.publish(HOST_STATUS, new MqttMessage(jsonMsg.toString().getBytes()));
                            break;
                        }
                        case "fileRequest": {
                            String server = json.getString("server");
                            JsonObject req = json.getJsonObject("payload");
                            executeDocker("docker", "start", server);
                            JsonObject wrapped = Json.createObjectBuilder()
                                    .add("server", server)
                                    .add("request", req)
                                    .build();
                            client.publish(AGG_REQ, new MqttMessage(wrapped.toString().getBytes()));
                            publishStatus(client, "fileRequest", server, "forwarded");
                            break;
                        }
                        default:
                            publishError(client, "Unknown action: " + action);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    publishError(client, e.getMessage());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String executeDocker(String... command) {
        StringBuilder output = new StringBuilder();
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            Process proc = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) output.append(line).append("\n");
            proc.waitFor();
        } catch (Exception e) {
            output.append("ERROR: ").append(e.getMessage());
        }
        return output.toString();
    }

    private static void publishStatus(MqttClient client, String type, String target, String result) {
        try {
            JsonObject json = Json.createObjectBuilder()
                    .add("status", "ok")
                    .add("type", type)
                    .add("target", target)
                    .add("result", result)
                    .build();
            client.publish(HOST_STATUS, new MqttMessage(json.toString().getBytes()));
        } catch (Exception e) { }
    }

    private static void publishError(MqttClient client, String message) {
        try {
            JsonObject json = Json.createObjectBuilder()
                    .add("status", "error")
                    .add("message", message)
                    .build();
            client.publish(HOST_STATUS, new MqttMessage(json.toString().getBytes()));
        } catch (Exception e) { }
    }
}