/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.cloudsystem;

import com.mycompany.cloudsystem.loadbalancer.MqttLoadBalancer;
import com.mycompany.cloudsystem.fileaggregator.MqttAggregator;
import com.mycompany.cloudsystem.host.MqttHost;
/**
 *
 * @author ntu-user
 */
public class Main {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("No arguments provided. Usage: 'loadbalancer', 'aggregator', or 'host'");
            return;
        }

        String mode = args[0].toLowerCase();

        switch (mode) {
            case "loadbalancer":
                System.out.println("Starting Load Balancer...");
                MqttLoadBalancer.main(args);
                break;
            case "fileaggregator":
                System.out.println("Starting File Aggregator...");
                MqttAggregator.main(args);
                break;
            case "host":
                System.out.println("Starting Host Manager...");
                MqttHost.main(args);
                break;
            default:
                System.out.println("Invalid argument. Usage: 'loadbalancer', 'aggregator', or 'host'");
                break;
        }
    }
}