/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.loadbalancer;

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

    private final Scheduler scheduler;
    private final Random random = new Random();

    private int activeProcessing = 0;
    private final int maxProcessing;

    public TrafficEmulator(int maxProcessing, Scheduler scheduler) {
        this.scheduler = scheduler;
        this.maxProcessing = maxProcessing;
    }

    public void add(Request r) {
        long arrivalDelay = 1000L + random.nextInt(4000);
        r.setArrivalTime(System.currentTimeMillis() + arrivalDelay);
        waitingQueue.offer(r);
    }

    public void step() {
        long now = System.currentTimeMillis();

        while (!waitingQueue.isEmpty()) {
            if (activeProcessing >= maxProcessing) {
                return;
            }

            Request head = waitingQueue.peek();
            if (now < head.getArrivalTime()) {
                return;
            }

            Scheduler.Algorithm algo = scheduler.chooseAlgo(waitingQueue);
            Request selected;

            switch (algo) {
                case SJF:
                    selected = scheduler.pickSJF(waitingQueue);
                    break;
                case ROUND_ROBIN:
                    selected = scheduler.pickRR(waitingQueue);
                    break;
                default:
                    selected = scheduler.pickFCFS(waitingQueue);
            }

            if (selected == null) {
                return;
            }

            waitingQueue.remove(selected);
            selected.setStartTime(now);
            processingQueue.offer(selected);
            activeProcessing++;

            // TODO: forward raw request to file aggregator after scale-up is completed
        }
    }

    public void markCompleted(Request r) {
        if (!processingQueue.remove(r)) {
            return;
        }

        activeProcessing--;
        readyQueue.offer(r);

        // TODO: notify UI if completion status is required
    }

    public void markFailed(Request r) {
        if (!processingQueue.remove(r)) {
            return;
        }

        activeProcessing--;
        r.setArrivalTime(System.currentTimeMillis());
        waitingQueue.offer(r);

        // TODO: apply retry backoff policy
    }

    public int getWaitingSize() {
        return waitingQueue.size();
    }

    public int getProcessingSize() {
        return activeProcessing;
    }

    public int getReadySize() {
        return readyQueue.size();
    }

    public int getMaxProcessing() {
        return maxProcessing;
    }

    public Queue<Request> getWaitingQueue() {
        return waitingQueue;
    }

    public Queue<Request> getProcessingQueue() {
        return processingQueue;
    }

    public Queue<Request> getReadyQueue() {
        return readyQueue;
    }
}
