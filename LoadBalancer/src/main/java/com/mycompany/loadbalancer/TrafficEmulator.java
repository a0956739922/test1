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
    private final Scheduler scheduler;
    private final Random random = new Random();

    public TrafficEmulator(int serverCount, Scheduler scheduler) {
        this.scheduler = scheduler;
        this.resources = new long[serverCount];
    }

    public void add(Request r) {
        long arrivalDelay = 1000L + random.nextInt(4000);
        r.setArrivalTime(System.currentTimeMillis() + arrivalDelay);
        waitingQueue.offer(r);
    }

    public void step() {
        long now = System.currentTimeMillis();
        Iterator<Request> pit = processingQueue.iterator();
        while (pit.hasNext()) {
            Request r = pit.next();
            int s = r.getAssignedServer();
            if (now >= resources[s]) {
                pit.remove();
                resources[s] = 0;
                readyQueue.offer(r);
            }
        }
        while (!waitingQueue.isEmpty()) {
            Request head = waitingQueue.peek();
            if (now < head.getArrivalTime()) {
                break;
            }
            Scheduler.Algorithm algo = scheduler.chooseAlgo(head.getSize());
            Request selected;
            switch (algo) {
                case FCFS:
                    selected = scheduler.pickFCFS(waitingQueue);
                    break;
                case SJF:
                    selected = scheduler.pickSJF(waitingQueue);
                    break;
                case ROUND_ROBIN:
                    selected = scheduler.pickRR(waitingQueue);
                    break;
                default:
                    selected = scheduler.pickFCFS(waitingQueue);
            }
            int server = scheduler.nextServer(resources.length);
            if (resources[server] != 0) {
                break;
            }
            waitingQueue.remove(selected);
            long serviceDelay = 1000L + random.nextInt(4000);
            selected.setAssignedServer(server);
            selected.setStartTime(now);
            processingQueue.offer(selected);
            resources[server] = now + serviceDelay;
        }
    }

    public Queue<Request> getReadyQueue() {
        return readyQueue;
    }
}