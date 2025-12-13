/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.loadbalancer;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

/**
 *
 * @author ntu-user
 */
public class TrafficEmulator {

    private final Queue<Request> waitingQueue = new LinkedList<>();
    private final Queue<Request> processingQueue = new LinkedList<>();
    private final Queue<Request> readyQueue = new LinkedList<>();
    private final long[] resources;
    private int effectiveServerCount;
    private final Scheduler scheduler;
    private final Random random = new Random();

    public TrafficEmulator(int serverCount, Scheduler scheduler) {
        this.scheduler = scheduler;
        this.resources = new long[serverCount];
        this.effectiveServerCount = serverCount;
    }

    public void add(Request r) {
        waitingQueue.offer(r);
        effectiveServerCount = estimateServers(r.getSize());
        System.out.println("[LB] Estimated servers = " + effectiveServerCount);
    }
    
    private int estimateServers(long sizeBytes) {
        if (sizeBytes < 5 * 1024 * 1024) return 1;
        if (sizeBytes < 20 * 1024 * 1024) return 2;
        if (sizeBytes < 50 * 1024 * 1024) return 3;
        return resources.length;
    }

    public void step() {
        long now = System.currentTimeMillis();
        Iterator<Request> it = processingQueue.iterator();
        while (it.hasNext()) {
            Request r = it.next();
            int s = r.getAssignedServer();
            if (now >= resources[s]) {
                it.remove();
                readyQueue.offer(r);
                resources[s] = 0;
                System.out.println("Ready → " + r + " (server " + s + ")");
            }
        }
        while (!waitingQueue.isEmpty()) {
            Request head = waitingQueue.peek();
            Scheduler.Algorithm algo = scheduler.chooseAlgo(head.getSize());
            Request selected;
            switch (algo) {
                case FCFS:        selected = scheduler.pickFCFS(waitingQueue); break;
                case SJF:         selected = scheduler.pickSJF(waitingQueue);  break;
                case ROUND_ROBIN: selected = scheduler.pickRR(waitingQueue);   break;
                default:          selected = scheduler.pickFCFS(waitingQueue);
            }
            int server = scheduler.nextServer(effectiveServerCount);
            if (resources[server] != 0) break;
            waitingQueue.remove(selected);
            long delay = (1 + random.nextInt(5)) * 1000L;
            selected.setStartTime(now);
            selected.setAssignedServer(server);
            processingQueue.offer(selected);
            resources[server] = now + delay;
            System.out.println("Dispatch → " + selected + " server=" + server + " delay=" + delay);
        }
    }

    public Queue<Request> getReadyQueue() {
        return readyQueue;
    }
}
