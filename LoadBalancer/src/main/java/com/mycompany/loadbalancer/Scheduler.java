/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.loadbalancer;

import java.util.Queue;
/**
 *
 * @author ntu-user
 */
public class Scheduler {

    public enum Algorithm { FCFS, SJF, ROUND_ROBIN }
    private int rrIndex = 0;

    public Algorithm chooseAlgo(Queue<Request> queue) {
        int n = queue.size();
        if (n <= 1) return Algorithm.FCFS;
        long min = Long.MAX_VALUE;
        long max = 0;
        for (Request r : queue) {
            long s = r.getSize();
            min = Math.min(min, s);
            max = Math.max(max, s);
        }
        if (max > min * 4) return Algorithm.SJF;
        if (n >= 5) return Algorithm.ROUND_ROBIN;
        return Algorithm.FCFS;
    }

    public Request pickFCFS(Queue<Request> queue) {
        return queue.peek();
    }

    public Request pickSJF(Queue<Request> queue) {
        Request min = null;
        for (Request r : queue) {
            if (min == null || r.getSize() < min.getSize()) {
                min = r;
            }
        }
        return min;
    }

    public Request pickRR(Queue<Request> queue) {
        if (queue.isEmpty()) return null;
        int index = rrIndex % queue.size();
        rrIndex++;
        int i = 0;
        for (Request r : queue) {
            if (i == index) return r;
            i++;
        }
        return null;
    }

    public int nextServer(int total) {
        int server = rrIndex % total;
        rrIndex++;
        return server;
    }
}