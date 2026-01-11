/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.cloudsystem.loadbalancer;

import java.util.Iterator;
import java.util.Queue;

/**
 *
 * @author ntu-user
 */
public class Scheduler {

    public enum Algo { FCFS, PRIORITY, RR }

    private Algo currentAlgo = Algo.FCFS;
    private int rrIndex = 0;

    public void chooseAlgo(int waitingSize) {
        if (waitingSize <= 2) {
            currentAlgo = Algo.FCFS;
        } else if (waitingSize <= 5) {
            currentAlgo = Algo.PRIORITY;
        } else {
            currentAlgo = Algo.RR;
        }
    }

    public Algo getCurrentAlgo() {
        return currentAlgo;
    }

    public Task select(Queue<Task> queue) {
        if (queue.isEmpty()) return null;

        switch (currentAlgo) {
            case PRIORITY:
                return selectPriority(queue);
            case RR:
                return selectRR(queue);
            case FCFS:
            default:
                return queue.poll();
        }
    }

    private Task selectPriority(Queue<Task> queue) {
        Task best = null;
        for (Task t : queue) {
            if (best == null || priorityOf(t) < priorityOf(best)) {
                best = t;
            }
        }
        queue.remove(best);
        return best;
    }

    private int priorityOf(Task t) {
        if ("CREATE".equals(t.getAction()) || "UPLOAD".equals(t.getAction())) {
            return 1;
        }
        if ("UPDATE".equals(t.getAction()) || "DELETE".equals(t.getAction())) {
            return 2;
        }
        return 3;
    }

    private Task selectRR(Queue<Task> queue) {
        rrIndex = rrIndex % queue.size();
        Iterator<Task> it = queue.iterator();
        Task selected = null;
        for (int i = 0; it.hasNext(); i++) {
            Task t = it.next();
            if (i == rrIndex) {
                selected = t;
                it.remove();
                break;
            }
        }
        return selected;
    }
}
