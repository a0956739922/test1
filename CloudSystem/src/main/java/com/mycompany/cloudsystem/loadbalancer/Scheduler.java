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
    private int rrCursor = 0;

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
        if (best != null) {
            queue.remove(best);
        }
        return best;
    }

    private int priorityOf(Task t) {
        String action = t.getAction() == null ? "" : t.getAction().toUpperCase();
        if ("CREATE".equals(action) || "UPLOAD".equals(action)) {
            return 1;
        }
        if ("UPDATE".equals(action) || "DELETE".equals(action)) {
            return 2;
        }
        return 3;
    }
    
    private Task selectRR(Queue<Task> queue) {
        if (queue.isEmpty()) return null;
        rrCursor = rrCursor % queue.size();
        Iterator<Task> it = queue.iterator();
        Task selected = null;
        int currentIndex = 0;
        while (it.hasNext()) {
            Task t = it.next();
            if (currentIndex == rrCursor) {
                selected = t;
                it.remove();
                break;
            }
            currentIndex++;
        }
        rrCursor++;
        return selected;
    }
}