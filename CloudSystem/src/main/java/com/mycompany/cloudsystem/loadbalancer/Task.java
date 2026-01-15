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
    private final long totalTimeMs;
    private long remainingTimeMs;
    private long startTime;

    public Task(String name, String action, int delaySec, String payload) {
        this.name = name;
        this.action = action;
        this.payload = payload;
        this.totalTimeMs = delaySec;
        this.remainingTimeMs = this.totalTimeMs;
        this.startTime = System.currentTimeMillis();
    }

    public String getName() { return name; }
    public String getAction() { return action; }
    public String getPayload() { return payload; }
    public long getRemainingTimeMs() { return remainingTimeMs; }

    public long consume(long sliceMs) {
        long used = Math.min(sliceMs, remainingTimeMs);
        remainingTimeMs -= used;
        return used;
    }

    public boolean isCompleted() {
        return remainingTimeMs <= 0;
    }

    @Override
    public String toString() {
        return name + "(" + action + ", remaining=" + remainingTimeMs + "ms)";
    }
}