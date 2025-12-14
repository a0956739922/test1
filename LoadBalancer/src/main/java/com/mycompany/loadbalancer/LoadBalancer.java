/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.loadbalancer;

import java.io.StringReader;
import java.util.UUID;
import javax.json.Json;
import javax.json.JsonObject;

/**
 *
 * @author ntu-user
 */
public class LoadBalancer {

    private static final double WAITING_THRESHOLD = 0.8;
    private static final int GROUP_SIZE = 4;
    private static final int MAX_CONTAINERS = 12;
    private final TrafficEmulator emulator;
    private final Scheduler scheduler = new Scheduler();
    private int currentContainers = GROUP_SIZE;
    private boolean scaling = false;

    public LoadBalancer() {
        emulator = new TrafficEmulator(12, scheduler);
    }

    public TrafficEmulator getEmulator() {
        return emulator;
    }

    public Request acceptRaw(String rawJson) {
        JsonObject json = Json.createReader(new StringReader(rawJson)).readObject();
        String action = json.getString("action");
        long size = json.getJsonNumber("sizeBytes").longValue();
        Request.Type type = mapAction(action);
        Request req = new Request(UUID.randomUUID().toString(), type, size);
        emulator.add(req);
        System.out.println("[LB] New Request " + req);
        checkScale();
        return req;
    }

    private void checkScale() {
        int waiting = emulator.getWaitingSize();
        int capacity = emulator.getServerCount();
        if (capacity == 0) return;
        double ratio = (double) waiting / capacity;
        if (ratio >= WAITING_THRESHOLD && !scaling) {
            if (currentContainers + GROUP_SIZE > MAX_CONTAINERS) {
                System.out.println(
                    "[LB] MAX capacity reached (" + currentContainers +
                    "), queueing only. waiting=" + waiting
                );
                return;
            }
            scaling = true;
            onScaleTriggered(waiting, capacity);
        }
    }

    protected void onScaleTriggered(int waiting, int capacity) {
        System.out.println(
            "[LB] SCALE UP (group) waiting=" + waiting +
            " capacity=" + capacity +
            " currentContainers=" + currentContainers
        );
    }

    public void onScaleCompleted() {
        currentContainers += GROUP_SIZE;
        scaling = false;
        System.out.println(
            "[LB] SCALE completed. currentContainers=" + currentContainers
        );
    }
    
    private Request.Type mapAction(String action) {
        switch (action) {
            case "upload":
            case "create":
            case "update":
                return Request.Type.UPLOAD;
            case "download":
                return Request.Type.DOWNLOAD;
            case "delete":
                return Request.Type.DELETE;
            case "share":
            case "renameMove":
                return Request.Type.META;
            default:
                throw new IllegalArgumentException("Unknown action: " + action);
        }
    }
}