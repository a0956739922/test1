/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.loadbalancer;

/**
 *
 * @author ntu-user
 */
import java.util.*;

class Scheduler {

    enum Algo { FCFS, SJF, RR }

    private Algo currentAlgo = Algo.FCFS;

    void chooseAlgo(int readySize) {
        if (readySize <= 2) {
            currentAlgo = Algo.FCFS;
        } else if (readySize <= 5) {
            currentAlgo = Algo.SJF;
        } else {
            currentAlgo = Algo.RR;
        }
    }

    /**
     * 從 readyQueue 中選出「最多 slots 個」request
     */
    List<Request> select(Queue<Request> readyQueue, int slots) {
        List<Request> selected = new ArrayList<>();

        if (readyQueue.isEmpty()) return selected;

        chooseAlgo(readyQueue.size());
        System.out.println("[Scheduler] Algo = " + currentAlgo);

        for (int i = 0; i < slots && !readyQueue.isEmpty(); i++) {
            Request r;

            switch (currentAlgo) {
                case SJF:
                    r = selectSJF(readyQueue);
                    break;
                case RR:
                case FCFS:
                default:
                    r = readyQueue.poll();
            }

            selected.add(r);
        }

        return selected;
    }

    private Request selectSJF(Queue<Request> queue) {
        Request shortest = null;
        for (Request r : queue) {
            if (shortest == null || r.delay < shortest.delay) {
                shortest = r;
            }
        }
        queue.remove(shortest);
        return shortest;
    }
}
