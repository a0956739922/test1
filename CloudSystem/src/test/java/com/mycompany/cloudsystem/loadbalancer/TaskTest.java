package com.mycompany.cloudsystem.loadbalancer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Task.
 */
public class TaskTest {

    @Test
    @DisplayName("Constructor sets fields and getters return expected values")
    public void testConstructorAndGetters() {
        Task task = new Task("job-1", "CREATE", 500, "payload");

        assertEquals("job-1", task.getName());
        assertEquals("CREATE", task.getAction());
        assertEquals("payload", task.getPayload());
        assertEquals(500, task.getRemainingTimeMs());
    }

    @Test
    @DisplayName("consume reduces remaining time and reports usage")
    public void testConsumeAndCompletion() {
        Task task = new Task("job-2", "UPDATE", 100, "payload");

        long used = task.consume(40);
        assertEquals(40, used);
        assertEquals(60, task.getRemainingTimeMs());
        assertFalse(task.isCompleted());

        used = task.consume(100);
        assertEquals(60, used);
        assertEquals(0, task.getRemainingTimeMs());
        assertTrue(task.isCompleted());
    }

    @Test
    @DisplayName("toString includes action and remaining time")
    public void testToString() {
        Task task = new Task("job-3", "DELETE", 75, "payload");

        String result = task.toString();
        assertTrue(result.contains("job-3"));
        assertTrue(result.contains("DELETE"));
        assertTrue(result.contains("remaining="));
    }
}
