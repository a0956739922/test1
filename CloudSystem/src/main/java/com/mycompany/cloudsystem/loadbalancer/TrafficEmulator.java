/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.cloudsystem.loadbalancer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
/**
 *
 * @author ntu-user
 */
public class TrafficEmulator {

    private final Queue<Task> waitingQueue = new LinkedList<>();
    private final Queue<Task> processingQueue = new LinkedList<>();
    private final Queue<Task> readyQueue = new LinkedList<>();

    private final List<Long> resources = new ArrayList<>();
    private int groups = 0;
    private static final int REQ_PER_GROUP = 5;
    private static final int MAX_GROUPS = 3;

    private long lastTaskTime = System.currentTimeMillis();

    public void addTask(String name, String action, long delayMs, String payload) {
        Task t = new Task(name, action, delayMs, payload);
        waitingQueue.offer(t);
        lastTaskTime = System.currentTimeMillis();
        System.out.println("[ADD] " + t);
    }

    public void processTasks() {
        if (groups == 0) return;

        // waiting -> processing (network slot)
        for (int i = 0; i < resources.size() && !waitingQueue.isEmpty(); i++) {
            if (resources.get(i) == 0L) {
                Task t = waitingQueue.poll();
                processingQueue.offer(t);
                resources.set(i, 1L); // mark busy
                System.out.println("[NETWORK] R" + (i + 1) + " -> " + t);
            }
        }

        // processing -> ready
        Iterator<Task> it = processingQueue.iterator();
        for (int i = 0; i < resources.size() && it.hasNext(); i++) {
            Task t = it.next();
            if (t.isNetworkDone()) {
                it.remove();
                readyQueue.offer(t);
                resources.set(i, 0L);
                System.out.println("[READY] " + t.getName());
            }
        }

        printQueues();
    }
    
    public boolean scaleUp() {
        if (groups == 0) return false;
        int capacity = groups * REQ_PER_GROUP;
        double ratio = (double) waitingQueue.size() / capacity;
        return ratio >= 0.8 && groups < MAX_GROUPS;
    }

    public boolean scaleDown() {
        return groups > 0 && waitingQueue.isEmpty() && processingQueue.isEmpty() && readyQueue.isEmpty() && System.currentTimeMillis() - lastTaskTime > 30_000;
    }

    public void updateGroups(int newGroups) {
        if (groups == newGroups) return;
        groups = newGroups;

        while (resources.size() < groups) resources.add(0L);
        while (resources.size() > groups) resources.remove(resources.size() - 1);

        System.out.println("[LB] Network slots=" + groups);
    }

    public Queue<Task> getReadyQueue() {
        return readyQueue;
    }

    private void printQueues() {
        System.out.println("----- QUEUE STATUS -----");
        System.out.println("WAITING   : " + waitingQueue);
        System.out.println("PROCESSING: " + processingQueue);
        System.out.println("READY     : " + readyQueue);
        System.out.println("------------------------\n");
    }
}
