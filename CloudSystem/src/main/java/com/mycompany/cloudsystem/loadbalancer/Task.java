/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.cloudsystem.loadbalancer;

/**
 *
 * @author ntu-user
 */
public class Task {

    private final String name;
    private final String action;
    private final String payload;

    private final long networkDelayMs;
    private final long startTime;

    public Task(String name, String action, long networkDelayMs, String payload) {
        this.name = name;
        this.action = action;
        this.payload = payload;
        this.networkDelayMs = networkDelayMs;
        this.startTime = System.currentTimeMillis();
    }

    public String getName() { return name; }
    public String getAction() { return action; }
    public String getPayload() { return payload; }

    public boolean isNetworkDone() {
        return System.currentTimeMillis() >= startTime + networkDelayMs;
    }

    @Override
    public String toString() {
        return name + "(" + action + ", delay=" + networkDelayMs + "ms)";
    }
}
