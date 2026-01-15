package com.mycompany.cloudsystem.loadbalancer;

import java.lang.reflect.Field;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TrafficEmulator.
 */
public class TrafficEmulatorTest {

    @Test
    @DisplayName("processTasks moves completed tasks to ready queue")
    public void testProcessTasksCompletesTask() {
        TrafficEmulator emulator = new TrafficEmulator();
        emulator.updateGroups(1);
        emulator.addTask("req-1", "CREATE", 1, "payload");

        emulator.processTasks();

        assertEquals(1, emulator.getReadyQueue().size());
        assertEquals("req-1", emulator.getReadyQueue().peek().getName());
    }

    @Test
    @DisplayName("scaleUp requires groups in range and sufficient waiting tasks")
    public void testScaleUp() {
        TrafficEmulator emulator = new TrafficEmulator();
        assertFalse(emulator.scaleUp());

        emulator.updateGroups(1);
        emulator.addTask("t1", "CREATE", 1, "p1");
        emulator.addTask("t2", "CREATE", 1, "p2");
        emulator.addTask("t3", "CREATE", 1, "p3");
        emulator.addTask("t4", "CREATE", 1, "p4");

        assertTrue(emulator.scaleUp());

        emulator.updateGroups(3);
        assertFalse(emulator.scaleUp());
    }

    @Test
    @DisplayName("scaleDown returns true after idle period when queues empty")
    public void testScaleDown() throws Exception {
        TrafficEmulator emulator = new TrafficEmulator();
        emulator.updateGroups(1);

        Field lastTaskField = TrafficEmulator.class.getDeclaredField("lastTaskTime");
        lastTaskField.setAccessible(true);
        lastTaskField.setLong(emulator, System.currentTimeMillis() - 31_000);

        assertTrue(emulator.scaleDown());
    }
}
