/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.loadbalancer;

/**
 *
 * @author ntu-user
 */
public class LoadBalancer {

    private static final int MAX_GROUPS = 3;
    private static final int THRESHOLD_COUNT = 4;

    private final Scheduler scheduler = new Scheduler();
    private final TrafficEmulator emulator = new TrafficEmulator(4, scheduler);

    private int activeGroups = 1;
    private boolean scaleInProgress = false;

    public enum ScaleDecision {
        NONE,
        SCALE_UP
    }

    public void acceptRequest(long size) {
        emulator.add(new Request(size));
        emulator.step();
    }

    public ScaleDecision evaluateScale() {
        if (activeGroups >= MAX_GROUPS) {
            return ScaleDecision.NONE;
        }
        if (activeGroups == 1 && emulator.getWaitingSize() == 1 && emulator.getProcessingSize() == 0) {
            scaleInProgress = true;
            return ScaleDecision.SCALE_UP;
        }
        if (emulator.getWaitingSize() > THRESHOLD_COUNT) {
            scaleInProgress = true;
            return ScaleDecision.SCALE_UP;
        }
        return ScaleDecision.NONE;
    }

    public void onScaleUpCompleted() {
        if (activeGroups < MAX_GROUPS) {
            activeGroups++;
        }
        scaleInProgress = false;
    }

    public int getWaiting() {
        return emulator.getWaitingSize();
    }

    public int getProcessing() {
        return emulator.getProcessingSize();
    }

    public int getReady() {
        return emulator.getReadySize();
    }
}