/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.cloudsystem.loadbalancer;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 *
 * @author ntu-user
 */
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

    List<Request> select(Queue<Request> readyQueue, int slots) {
        List<Request> selected = new ArrayList<>();
        if (readyQueue.isEmpty()) return selected;
        chooseAlgo(readyQueue.size());
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
