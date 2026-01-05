/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import javax.json.Json;
import javax.json.JsonObject;
import org.eclipse.paho.client.mqttv3.*;
/**
 *
 * @author ntu-user
 */
public class MqttSubUI {

    private static final String BROKER = "tcp://mqtt-broker:1883";
    private static final String AGG_RES = "/agg/response";
    private static final String CLIENT_ID = "UIClientSub";

    private static final List<Runnable> refreshListeners = new CopyOnWriteArrayList<>();
    private static final Map<String, Consumer<String>> requestCallbacks = new ConcurrentHashMap<>();
    
    private MqttClient client;

    public static void addRefreshListener(Runnable listener) {
        refreshListeners.add(listener);
    }

    public static void registerRequestCallback(String reqId, Consumer<String> callback) {
        requestCallbacks.put(reqId, callback);
    }

    private void notifyRefresh() {
        javafx.application.Platform.runLater(() -> {
            for (Runnable listener : refreshListeners) {
                listener.run();
            }
        });
    }

    public void start() {
        try {
            client = new MqttClient(BROKER, CLIENT_ID);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            client.connect(options);
            client.subscribe(AGG_RES, 1);
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {}

                @Override
                public void messageArrived(String topic, MqttMessage msg) {
                    try {
                        String payload = new String(msg.getPayload());
                        JsonObject res = Json.createReader(new StringReader(payload)).readObject();
                        String reqId = res.getString("req_id", "");
                        String action = res.getString("action", "");
                        String status = res.getString("status", "");
                        if ("ok".equals(status)) {
                            if ("delete".equals(action)) {
                                new SQLiteDB().finalizeDelete(res.getInt("fileId"));
                                notifyRefresh();
                            } else if ("create".equals(action)) {
                                new SQLiteDB().finalizeCreate(reqId, res.getInt("fileId"));
                                notifyRefresh();
                            }
                        }
                        Consumer<String> callback = requestCallbacks.remove(reqId);
                        if (callback != null) {
                            callback.accept(payload);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {}
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                client.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}