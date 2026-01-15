package com.mycompany.javafxapplication1;

import javafx.beans.property.SimpleStringProperty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for User.
 */
public class UserTest {

    @Test
    @DisplayName("Constructor sets fields and getters return values")
    public void testConstructorAndGetters() {
        User user = new User(101, "alice", "hash123", "admin");

        assertEquals(101, user.getUserId());
        assertEquals("alice", user.getUsername());
        assertEquals("hash123", user.getPasswordHash());
        assertEquals("admin", user.getRole());
    }

    @Test
    @DisplayName("setRole updates role and roleProperty reflects change")
    public void testRoleUpdates() {
        User user = new User(202, "bob", "pw", "user");

        SimpleStringProperty roleProperty = user.roleProperty();
        assertEquals("user", roleProperty.get());

        user.setRole("manager");
        assertEquals("manager", user.getRole());
        assertEquals("manager", roleProperty.get());
    }
}
