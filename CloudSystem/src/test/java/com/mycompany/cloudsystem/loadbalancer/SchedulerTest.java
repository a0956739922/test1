package com.mycompany.cloudsystem.loadbalancer;

import java.util.LinkedList;
import java.util.Queue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SchedulerTest {

    @Test
    public void testChooseAlgoFCFS() {
        Scheduler scheduler = new Scheduler();
        scheduler.chooseAlgo(1);

        assertEquals(Scheduler.Algo.FCFS, scheduler.getCurrentAlgo());
    }

    @Test
    public void testChooseAlgoPriority() {
        Scheduler scheduler = new Scheduler();
        scheduler.chooseAlgo(4);

        assertEquals(Scheduler.Algo.PRIORITY, scheduler.getCurrentAlgo());
    }

    @Test
    public void testChooseAlgoRR() {
        Scheduler scheduler = new Scheduler();
        scheduler.chooseAlgo(10);

        assertEquals(Scheduler.Algo.RR, scheduler.getCurrentAlgo());
    }

    @Test
    public void testSelectFCFS() {
        Scheduler scheduler = new Scheduler();
        scheduler.chooseAlgo(1);

        Queue<Task> q = new LinkedList<>();
        q.add(new Task("A", "CREATE", 100, "{}"));
        q.add(new Task("B", "DELETE", 100, "{}"));

        Task t = scheduler.select(q);

        assertEquals("A", t.getName());
        assertEquals(1, q.size());
    }

    @Test
    public void testSelectPriority() {
        Scheduler scheduler = new Scheduler();
        scheduler.chooseAlgo(4); // PRIORITY

        Queue<Task> q = new LinkedList<>();
        q.add(new Task("low", "DELETE", 100, "{}"));
        q.add(new Task("high", "CREATE", 100, "{}"));

        Task t = scheduler.select(q);

        assertEquals("high", t.getName());
        assertEquals(1, q.size());
    }

    @Test
    public void testSelectRRRotation() {
        Scheduler scheduler = new Scheduler();
        scheduler.chooseAlgo(10); // RR

        Queue<Task> q = new LinkedList<>();
        q.add(new Task("A", "CREATE", 100, "{}"));
        q.add(new Task("B", "CREATE", 100, "{}"));
        q.add(new Task("C", "CREATE", 100, "{}"));

        Task first = scheduler.select(q);
        Task second = scheduler.select(q);
        Task third = scheduler.select(q);

        assertEquals("A", first.getName());
        assertEquals("C", second.getName());
        assertEquals("B", third.getName());
    }

    @Test
    public void testSelectEmptyQueueReturnsNull() {
        Scheduler scheduler = new Scheduler();
        scheduler.chooseAlgo(1);

        Queue<Task> q = new LinkedList<>();

        assertNull(scheduler.select(q));
    }
}
