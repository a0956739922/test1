package com.mycompany.cloudsystem.loadbalancer;

import java.util.Queue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TrafficEmulatorTest {

    @Test
    public void testAddTask() {
        TrafficEmulator emu = new TrafficEmulator();
        emu.updateGroups(1);

        emu.addTask("req-1", "CREATE", 500, "{}");

        assertNotNull(emu.getReadyQueue());
    }

    @Test
    public void testProcessTasksMovesToReady() throws Exception {
        TrafficEmulator emu = new TrafficEmulator();
        emu.updateGroups(1);

        emu.addTask("req-1", "DELETE", 200, "{}");
        emu.processTasks();

        Thread.sleep(300);
        emu.processTasks();

        Queue<Task> ready = emu.getReadyQueue();
        assertEquals(1, ready.size());
    }

    @Test
    public void testScaleUpConditionTriggered() {
        TrafficEmulator emu = new TrafficEmulator();
        emu.updateGroups(1);
        for (int i = 1; i <= 4; i++) {
            emu.addTask("req-" + i, "UPDATE", 1000, "{}");
        }

        assertTrue(emu.scaleUp());
    }

    @Test
    public void testScaleUpIncreasesProcessingCapacity() throws Exception {
        TrafficEmulator emu = new TrafficEmulator();
        
        emu.updateGroups(1);
        for (int i = 1; i <= 6; i++) {
            emu.addTask("req-" + i, "CREATE", 300, "{}");
        }

        emu.processTasks();
        Thread.sleep(400);
        emu.processTasks();

        int readyAfterGroup1 = emu.getReadyQueue().size();

        emu.updateGroups(2);
        for (int i = 7; i <= 12; i++) {
            emu.addTask("req-" + i, "CREATE", 300, "{}");
        }

        emu.processTasks();
        Thread.sleep(400);
        emu.processTasks();

        int readyAfterGroup2 = emu.getReadyQueue().size();

        assertTrue(readyAfterGroup2 > readyAfterGroup1);
    }

    @Test
    public void testScaleDownFalseWhenBusy() {
        TrafficEmulator emu = new TrafficEmulator();
        emu.updateGroups(1);

        emu.addTask("req-1", "DELETE", 1000, "{}");

        assertFalse(emu.scaleDown());
    }

    @Test
    public void testScaleDownTrueWhenIdle() throws Exception {
        TrafficEmulator emu = new TrafficEmulator();
        emu.updateGroups(1);
        Thread.sleep(31_000);
        emu.getReadyQueue().clear();
        assertTrue(emu.scaleDown());
    }

    @Test
    public void testGetReadyQueueNeverNull() {
        TrafficEmulator emu = new TrafficEmulator();
        assertNotNull(emu.getReadyQueue());
    }
}
