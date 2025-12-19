/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.mqtthost;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
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

    private static final String BROKER = "tcp://localhost:1883";
    private static final String LB_SCALE = "/lb/scale";
    private static final String HOST_STATUS = "/host/status";
    private static final String CLIENT_ID = "HostManagerClient";

    private static final int GROUP_SIZE = 4;
    private static final int MAX_CONTAINERS = 12;

    private static final List<String> VOLUMES =
            List.of("fs-vol-1", "fs-vol-2", "fs-vol-3", "fs-vol-4");

    private static final Map<String, String> volumeOwner = new ConcurrentHashMap<>();

    private static int nextContainerIndex = 1;
    private static int activeGroups = 0;

    public static void main(String[] args) {
        try {
            MqttClient client = new MqttClient(BROKER, CLIENT_ID);
            client.connect();
            client.subscribe(LB_SCALE, 1);

            client.setCallback(new MqttCallback() {

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    if (!LB_SCALE.equals(topic)) return;

                    JsonObject msg = Json.createReader(
                            new StringReader(new String(message.getPayload()))
                    ).readObject();

                    int targetGroups = msg.getInt("groups");

                    if (activeGroups >= targetGroups) return;

                    int groupsToAdd = targetGroups - activeGroups;
                    List<String> started = new ArrayList<>();

                    for (int g = 0; g < groupsToAdd; g++) {
                        for (int i = 0; i < GROUP_SIZE; i++) {
                            if (nextContainerIndex > MAX_CONTAINERS) break;

                            String volume = null;
                            for (String v : VOLUMES) {
                                if (!volumeOwner.containsKey(v)) {
                                    volume = v;
                                    break;
                                }
                            }

                            String name = "soft40051-files-container" + nextContainerIndex;
                            dockerRun(name, volume);

                            if (volume != null) {
                                volumeOwner.put(volume, name);
                            }

                            started.add(name);
                            nextContainerIndex++;
                        }
                        activeGroups++;
                    }

                    waitUntilRunning(started);

                    JsonObject status = Json.createObjectBuilder()
                            .add("status", "scale_up_done")
                            .add("active_groups", activeGroups)
                            .build();

                    client.publish(
                            HOST_STATUS,
                            new MqttMessage(status.toString().getBytes())
                    );
                }

                @Override
                public void connectionLost(Throwable cause) {}

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {}
            });

            while (true) Thread.sleep(1000);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void dockerRun(String name, String volume) {
        List<String> cmd = new ArrayList<>(List.of(
                "docker", "run", "-d", "--rm",
                "--name", name,
                "--network", "soft40051_network"
        ));

        if (volume != null) {
            cmd.add("-v");
            cmd.add(volume + ":/home/ntu-user/data");
        }

        cmd.add("pedrombmachado/simple-ssh-container:base");
        exec(cmd);
    }

    private static void waitUntilRunning(List<String> containers) throws InterruptedException {
        while (true) {
            boolean ready = true;
            for (String c : containers) {
                if (!dockerPsContains(c)) {
                    ready = false;
                    break;
                }
            }
            if (ready) return;
            Thread.sleep(500);
        }
    }

    private static boolean dockerPsContains(String name) {
        String out = exec(List.of(
                "docker", "ps",
                "--filter", "name=" + name,
                "--format", "{{.Names}}"
        ));
        return out.contains(name);
    }

    private static String exec(List<String> command) {
        StringBuilder sb = new StringBuilder();
        try {
            Process p = new ProcessBuilder(command).start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            p.waitFor();
        } catch (Exception e) {
            sb.append(e.getMessage());
        }
        return sb.toString();
    }
}