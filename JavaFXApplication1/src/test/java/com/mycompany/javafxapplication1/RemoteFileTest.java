package com.mycompany.javafxapplication1;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RemoteFile.
 */
public class RemoteFileTest {

    @Test
    public void testConstructor() {
        RemoteFile remoteFile = new RemoteFile(7, 42, "report.pdf");

        assertEquals(7, remoteFile.getFileId());
        assertEquals(42, remoteFile.getOwnerUserId());
        assertEquals("report.pdf", remoteFile.getName());
        assertNull(remoteFile.getOwnerName());
        assertNull(remoteFile.getPermission());
        assertNull(remoteFile.getSharedTo());
    }

    @Test
    public void testSetters() {
        RemoteFile remoteFile = new RemoteFile(8, 99, "photo.png");

        remoteFile.setOwnerName("dana");
        remoteFile.setPermission("r");
        remoteFile.setSharedTo("team-a");

        assertEquals("dana", remoteFile.getOwnerName());
        assertEquals("r", remoteFile.getPermission());
        assertEquals("team-a", remoteFile.getSharedTo());
    }
}