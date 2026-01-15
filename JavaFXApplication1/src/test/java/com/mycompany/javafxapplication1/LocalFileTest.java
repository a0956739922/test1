package com.mycompany.javafxapplication1;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for LocalFile.
 */
public class LocalFileTest {

    @Test
    public void testSettersAndGetters() {
        LocalFile file = new LocalFile();

        file.setLocalId(1);
        file.setRemoteFileId(20);
        file.setReqId("req-abc");
        file.setUserId(10);
        file.setUsername("carol");
        file.setName("notes.txt");
        file.setPermission("rw");
        file.setSharedTo("team");
        file.setContent("hello world");
        file.setSyncState("SYNCED");
        file.setDeleted(true);
        file.setUpdatedAt("2024-05-30T10:15:00Z");

        assertEquals(1, file.getLocalId());
        assertEquals(20, file.getRemoteFileId());
        assertEquals("req-abc", file.getReqId());
        assertEquals(10, file.getUserId());
        assertEquals("carol", file.getUsername());
        assertEquals("notes.txt", file.getFileName());
        assertEquals("rw", file.getPermission());
        assertEquals("team", file.getSharedTo());
        assertEquals("hello world", file.getContent());
        assertEquals("SYNCED", file.getSyncState());
        assertTrue(file.isDeleted());
        assertEquals("2024-05-30T10:15:00Z", file.getUpdatedAt());
    }

    @Test
    public void testDefaults() {
        LocalFile file = new LocalFile();

        assertNull(file.getLocalId());
        assertNull(file.getRemoteFileId());
        assertNull(file.getReqId());
        assertNull(file.getUserId());
        assertNull(file.getUsername());
        assertNull(file.getFileName());
        assertNull(file.getPermission());
        assertNull(file.getSharedTo());
        assertNull(file.getContent());
        assertNull(file.getSyncState());
        assertFalse(file.isDeleted());
        assertNull(file.getUpdatedAt());
    }
}