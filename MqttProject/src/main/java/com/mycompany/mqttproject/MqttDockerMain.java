/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.mqttproject;

/**
 *
 * @author ntu-user
 */
public class MqttDockerMain {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: loadbalancer | host | aggregator");
            return;
        }

        switch (args[0].toLowerCase()) {
            case "loadbalancer":
                MqttLoadBalancer.main(args);
                break;
            case "host":
                MqttHost.main(args);
                break;
            case "aggregator":
                MqttAggregator.main(args);
                break;
            default:
                System.out.println("Usage: loadbalancer | host | aggregator");
        }
    }
}
