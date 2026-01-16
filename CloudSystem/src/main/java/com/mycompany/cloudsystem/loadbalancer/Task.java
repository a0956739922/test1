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
    private final long delay;
    private final String payload;
    private long startTime = -1;

    public Task(String name, String action, long delay, String payload) {
        this.name = name;
        this.action = action;
        this.delay = delay;
        this.payload = payload;
    }
    
    public void markProcessingStart() {
        this.startTime = System.currentTimeMillis();
    }

    public boolean hasStarted() {
        return startTime > 0;
    }

    public boolean isReady(long now) {
        return hasStarted() && now >= startTime + delay;
    }

    public String getName() {
        return name;
    }

    public String getAction() {
        return action;
    }

    public long getDelay() {
        return delay;
    }

    public long getStartTime() {
        return startTime;
    }
    
    public String getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return name + "(" + action + "," + delay / 1000 + "s)";
    }
}
