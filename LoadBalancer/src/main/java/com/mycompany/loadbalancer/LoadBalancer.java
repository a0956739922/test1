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
    public Request acceptRaw(String rawJson) {
        JsonObject json = Json.createReader(new StringReader(rawJson)).readObject();
        String action = json.getString("action");
        long size = json.getJsonNumber("sizeBytes").longValue();
        String id = UUID.randomUUID().toString();
        Request.Type type;
        switch (action) {
            case "upload":
            case "create":
            case "update":
                type = Request.Type.UPLOAD;
                break;
            case "download":
                type = Request.Type.DOWNLOAD;
                break;
            case "delete":
                type = Request.Type.DELETE;
                break;
            case "share":
            case "renameMove":
                type = Request.Type.META;
                break;
            default:
                throw new IllegalArgumentException("Unknown action: " + action);
        }
        Request req = new Request(id, type, size);
        emulator.add(req);
        System.out.println("[LB] New Request " + req);
        return req;
    }
}