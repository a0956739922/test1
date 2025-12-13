/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.loadbalancer;

/**
 *
 * @author ntu-user
 */
public class Request {

    public enum Type { UPLOAD, DOWNLOAD, DELETE, META }

    private final String id;
    private final Type type;
    private final long size;

    private long arrivalTime;
    private long startTime;
    private int assignedServer = -1;

    public Request(String id, Type type, long size) {
        this.id = id;
        this.type = type;
        this.size = size;
    }

    public String getId() { return id; }
    public Type getType() { return type; }
    public long getSize() { return size; }

    public long getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(long t) { this.arrivalTime = t; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long t) { this.startTime = t; }

    public int getAssignedServer() { return assignedServer; }
    public void setAssignedServer(int s) { this.assignedServer = s; }

    @Override
    public String toString() {
        return "[Req " + id + " | " + type + " | size=" + size + "]";
    }
}