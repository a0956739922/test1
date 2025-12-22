/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.loadbalancer;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
/**
 *
 * @author ntu-user
 */
public class LoadBalancer {

    private final Queue<Request> waitingQueue = new LinkedList<>();
    private final Queue<Request> processingQueue = new LinkedList<>();
    private final Queue<Request> readyQueue = new LinkedList<>();

    private final Random random = new Random();
    private final Scheduler scheduler = new Scheduler();

    private int groups = 0;

    private static final int MAX_GROUPS = 3;
    private static final int REQ_PER_GROUP = 5;

    public interface DispatchHandler {
        void dispatch(Request r);
    }

    public interface ScaleUpHandler {
        void onScaleUp(int newGroupCount);
    }

    private DispatchHandler dispatchHandler;
    private ScaleUpHandler scaleUpHandler;

    public void setDispatchHandler(DispatchHandler handler) {
        this.dispatchHandler = handler;
    }

    public void setScaleUpHandler(ScaleUpHandler handler) {
        this.scaleUpHandler = handler;
    }

    public void receiveRequest(String reqId) {
        long delay = 1000 + random.nextInt(4000);
        Request r = new Request(reqId, delay);
        waitingQueue.offer(r);
    }

    public void tick() {
        moveWaitingToProcessing();
        moveProcessingToReady();
        dispatchReady();
        checkScaleUp();
    }

    public void ensureAtLeastOneGroup() {
        if (groups == 0) {
            groups = 1;
            if (scaleUpHandler != null) {
                scaleUpHandler.onScaleUp(groups);
            }
        }
    }

    private void moveWaitingToProcessing() {
        int capacity = groups * REQ_PER_GROUP;
        while (processingQueue.size() < capacity && !waitingQueue.isEmpty()) {
            Request r = waitingQueue.poll();
            r.delayStartTime = System.currentTimeMillis();
            processingQueue.offer(r);
        }
    }

    private void moveProcessingToReady() {
        long now = System.currentTimeMillis();
        Iterator<Request> it = processingQueue.iterator();
        while (it.hasNext()) {
            Request r = it.next();
            if (now >= r.delayStartTime + r.delay) {
                it.remove();
                readyQueue.offer(r);
            }
        }
    }

    private void dispatchReady() {
        int capacity = groups * REQ_PER_GROUP;
        int dispatchSlots = capacity - processingQueue.size();
        List<Request> toDispatch = scheduler.select(readyQueue, dispatchSlots);
        for (Request r : toDispatch) {
            if (dispatchHandler != null) {
                dispatchHandler.dispatch(r);
            }
            readyQueue.remove(r);
        }
    }

    private void checkScaleUp() {
        int capacity = groups * REQ_PER_GROUP;
        double waitingRatio = capacity == 0 ? 0 : (double) waitingQueue.size() / capacity;
        if (waitingRatio >= 0.8 && groups < MAX_GROUPS) {
            groups++;
            if (scaleUpHandler != null) {
                scaleUpHandler.onScaleUp(groups);
            }
        }
    }

    public int getWaitingCount() {
        return waitingQueue.size();
    }

    public int getProcessingCount() {
        return processingQueue.size();
    }

    public int getReadyCount() {
        return readyQueue.size();
    }
}