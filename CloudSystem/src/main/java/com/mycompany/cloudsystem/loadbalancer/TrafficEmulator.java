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

    private final Scheduler scheduler = new Scheduler();

    private static final long RR_TIME_SLICE_MS = 1000;

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
            if (resources.get(i) == 0L) {   // resource free
                Task t = scheduler.select(waitingQueue);
                processingQueue.offer(t);
                resources.set(i, System.currentTimeMillis());
                System.out.printf("[PROCESS] %-8s | R%d -> %s%n",
                        scheduler.getCurrentAlgo(), i + 1, t);
            }
        }

        Iterator<Task> it = processingQueue.iterator();
        for (int i = 0; i < resources.size() && it.hasNext(); i++) {
            Task t = it.next();
            long used = scheduler.getCurrentAlgo() == Scheduler.Algo.RR ? t.consume(RR_TIME_SLICE_MS) : t.consume(t.getRemainingTimeMs());
            if (t.isCompleted()) {
                it.remove();
                readyQueue.offer(t);
                resources.set(i, 0L);
                System.out.println("[READY] " + t.getName());
            } else {
                it.remove();
                waitingQueue.offer(t);
                resources.set(i, 0L);
                System.out.println("[RR] " + t);
            }
        }
        printQueues();
    }
    
    public boolean scaleUp() {
        if (groups <= 0 || groups >= 3) {
            return false;
        }
        return waitingQueue.size() >= 4;
    }

    public boolean scaleDown() {
        return groups > 0 && waitingQueue.isEmpty() && processingQueue.isEmpty() && readyQueue.isEmpty() && System.currentTimeMillis() - lastTaskTime > 30_000;
    }

    public void updateGroups(int newGroups) {
        if (groups == newGroups) return;
        groups = newGroups;
        while (resources.size() < groups) {
            resources.add(0L);
        }
        while (resources.size() > groups) {
            resources.remove(resources.size() - 1);
        }
        System.out.println("[LB] Emulator updated groups=" + groups);
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