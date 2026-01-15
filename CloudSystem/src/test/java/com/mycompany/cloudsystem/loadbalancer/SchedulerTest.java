package com.mycompany.cloudsystem.loadbalancer;

import java.util.ArrayDeque;
import java.util.Queue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Scheduler.
 */
public class SchedulerTest {

    @Test
    @DisplayName("chooseAlgo selects FCFS, PRIORITY, or RR based on waiting size")
    public void testChooseAlgo() {
        Scheduler scheduler = new Scheduler();

        scheduler.chooseAlgo(2);
        assertEquals(Scheduler.Algo.FCFS, scheduler.getCurrentAlgo());

        scheduler.chooseAlgo(5);
        assertEquals(Scheduler.Algo.PRIORITY, scheduler.getCurrentAlgo());

        scheduler.chooseAlgo(6);
        assertEquals(Scheduler.Algo.RR, scheduler.getCurrentAlgo());
    }

    @Test
    @DisplayName("select uses FCFS ordering by default")
    public void testSelectFcfs() {
        Scheduler scheduler = new Scheduler();
        Queue<Task> queue = new ArrayDeque<>();
        Task first = new Task("t1", "UPLOAD", 1, "p1");
        Task second = new Task("t2", "UPDATE", 1, "p2");
        queue.add(first);
        queue.add(second);

        Task selected = scheduler.select(queue);
        assertSame(first, selected);
        assertEquals(1, queue.size());
    }

    @Test
    @DisplayName("select uses PRIORITY to pick highest priority action")
    public void testSelectPriority() {
        Scheduler scheduler = new Scheduler();
        scheduler.chooseAlgo(4);
        Queue<Task> queue = new ArrayDeque<>();
        Task low = new Task("t1", "READ", 1, "p1");
        Task mid = new Task("t2", "UPDATE", 1, "p2");
        Task high = new Task("t3", "CREATE", 1, "p3");
        queue.add(low);
        queue.add(mid);
        queue.add(high);

        Task selected = scheduler.select(queue);
        assertSame(high, selected);
        assertEquals(2, queue.size());
    }

    @Test
    @DisplayName("select uses RR to rotate through the queue")
    public void testSelectRoundRobin() {
        Scheduler scheduler = new Scheduler();
        scheduler.chooseAlgo(10);
        Queue<Task> queue = new ArrayDeque<>();
        Task t1 = new Task("t1", "CREATE", 1, "p1");
        Task t2 = new Task("t2", "CREATE", 1, "p2");
        Task t3 = new Task("t3", "CREATE", 1, "p3");
        queue.add(t1);
        queue.add(t2);
        queue.add(t3);

        Task first = scheduler.select(queue);
        assertSame(t1, first);

        Task second = scheduler.select(queue);
        assertSame(t2, second);

        Task third = scheduler.select(queue);
        assertSame(t3, third);
    }
}
