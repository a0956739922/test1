/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.loadbalancer;

/**
 *
 * @author ntu-user
 */
public class LoadBalancer {

    private final TrafficEmulator emulator;
    private final Scheduler scheduler = new Scheduler();

    public LoadBalancer() {
        emulator = new TrafficEmulator(4, scheduler);
    }

    public void submitRequest(Request req) {
        emulator.add(req);
        System.out.println("New Request: " + req);
    }

    public TrafficEmulator getEmulator() {
        return emulator;
    }
}