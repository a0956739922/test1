package com.mycompany.javafxapplication1;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MySQLDB hashing helpers.
 */
public class MySQLDBTest {

    @Test
    @DisplayName("hashPassword produces deterministic hash for same input and verifyPassword matches")
    public void testHashAndVerifyPassword() {
        MySQLDB db = new MySQLDB();

        String first = db.hashPassword("secret");
        String second = db.hashPassword("secret");

        assertNotNull(first);
        assertEquals(first, second);
        assertTrue(db.verifyPassword("secret", first));
        assertFalse(db.verifyPassword("wrong", first));
    }
}