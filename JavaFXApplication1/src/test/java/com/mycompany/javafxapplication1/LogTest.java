package com.mycompany.javafxapplication1;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Log.
 */
public class LogTest {

    @Test
    @DisplayName("Constructor populates fields and getters return values")
    public void testConstructorAndGetters() {
        Log log = new Log(1, 50, "erin", "UPLOAD", "file.txt", "2024-06-01T09:45:00Z");

        assertEquals(1, log.getId());
        assertEquals(50, log.getUserId());
        assertEquals("erin", log.getUsername());
        assertEquals("UPLOAD", log.getAction());
        assertEquals("file.txt", log.getDetail());
        assertEquals("2024-06-01T09:45:00Z", log.getTimestamp());
    }
}
