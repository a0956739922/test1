/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.cloudsystem.loadbalancer;

import java.util.ArrayList;
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
    private final List<Task> processingSlots = new ArrayList<>();
    
    private final Scheduler scheduler = new Scheduler();

    private static final int REQ_PER_GROUP = 4;
    private static final int MAX_GROUPS = 3;
    private static final int DELAY_SLOTS = 3;

    private int groups = 0;
    private long lastTaskTime = System.currentTimeMillis();

    public void addTask(String name, String action, int delaySec, String payload) {
        Task t = new Task(name, action, delaySec, payload);
        waitingQueue.offer(t);
        lastTaskTime = System.currentTimeMillis();
        System.out.println("[ADD] " + t);
    }

    public void processTasks() {
        if (groups == 0) return;
        scheduler.chooseAlgo(waitingQueue.size());
        for (int i = 0; i < resources.size() && !waitingQueue.isEmpty(); i++) {
            if (resources.get(i) == 0L) {
                Task task = scheduler.select(waitingQueue);
                task.markProcessingStart();
                processingQueue.offer(task);
                processingSlots.set(i, task);
                resources.set(i, task.getStartTime() + task.getDelay());
                System.out.printf("[PROCESS] %-8s | slot-%d -> %s%n", scheduler.getCurrentAlgo(), i + 1, task);
            }
        }
        long now = System.currentTimeMillis();
        for (int i = 0; i < resources.size(); i++) {
            if (resources.get(i) > 0 && resources.get(i) <= now) {
                Task t = processingSlots.get(i);
                if (t != null) {
                    processingQueue.remove(t);
                    readyQueue.offer(t);
                    processingSlots.set(i, null);
                    resources.set(i, 0L);
                    System.out.println("[READY] " + t.getName());
                }
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
        int targetSlots = groups * DELAY_SLOTS;
        while (resources.size() < targetSlots) {
            resources.add(0L);
            processingSlots.add(null);
        }
        while (resources.size() > targetSlots) {
            int last = resources.size() - 1;
            resources.remove(last);
            processingSlots.remove(last);
        }
        System.out.println("[LB] Emulator updated groups=" + groups +", Slots=" + resources.size());
    }
    
    private void printQueues() {
        System.out.println("----- QUEUE STATUS -----");
        System.out.println("WAITING   : " + waitingQueue);
        System.out.println("PROCESSING: " + processingQueue);
        System.out.println("READY     : " + readyQueue);
        System.out.println("------------------------\n");
    }
    
    public Queue<Task> getReadyQueue() {
        return readyQueue;
    }

}
