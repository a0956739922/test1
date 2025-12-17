/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.loadbalancer;

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
    private static final String HOST_STATUS = "/host/status";
    private static final String LB_SCALE = "/lb/scale";

    private static final int GROUP_SIZE = 4;

    private static final LoadBalancer lb = new LoadBalancer();

    public static void main(String[] args) {
        try {
            MqttClient client = new MqttClient(BROKER, CLIENT_ID);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            client.connect(options);
            System.out.println("[LB] Connected to broker");
            client.subscribe(UI_REQ, 1);
            client.subscribe(HOST_STATUS, 1);
            System.out.println("[LB] Subscribed to:");
            System.out.println("   " + UI_REQ);
            System.out.println("   " + HOST_STATUS);
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("[LB] Connection lost: " + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage msg) throws Exception {
                    if (UI_REQ.equals(topic)) {
                        lb.acceptRequest(msg.getPayload().length);
                        if (lb.evaluateScale() == LoadBalancer.ScaleDecision.SCALE_UP) {
                            JsonObject out = Json.createObjectBuilder().add("group_size", GROUP_SIZE).build();
                            MqttMessage scaleMsg = new MqttMessage(out.toString().getBytes());
                            scaleMsg.setQos(1);
                            client.publish(LB_SCALE, scaleMsg);
                            System.out.println(
                                "[LB] SCALE UP requested (group_size=" + GROUP_SIZE + ")"
                            );
                        }
                    }
                    else if (HOST_STATUS.equals(topic)) {
                        JsonObject status = Json.createReader(new java.io.StringReader(new String(msg.getPayload()))).readObject();
                        if ("scale_up_done".equals(status.getString("status", ""))) {
                            lb.onScaleUpCompleted();
                            System.out.println("[LB] Scale-up completed");
                        }
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
}
