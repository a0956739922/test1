package com.mycompany.javafxapplication1;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SQLiteDB.
 */
public class SQLiteDBTest {

    private SQLiteDB createIsolatedDb(Path tempDir) throws Exception {
        SQLiteDB db = new SQLiteDB();
        Field dbUrlField = SQLiteDB.class.getDeclaredField("dbUrl");
        dbUrlField.setAccessible(true);
        dbUrlField.set(db, "jdbc:sqlite:" + tempDir.resolve("test.db"));

        Method createSession = SQLiteDB.class.getDeclaredMethod("createLocalSessionTable");
        createSession.setAccessible(true);
        createSession.invoke(db);

        Method createFiles = SQLiteDB.class.getDeclaredMethod("createLocalFilesTable");
        createFiles.setAccessible(true);
        createFiles.invoke(db);

        return db;
    }

    @Test
    public void testSessionLifecycle(@TempDir Path tempDir) throws Exception {
        SQLiteDB db = createIsolatedDb(tempDir);
        User user = new User(1, "alice", "hash", "admin");

        db.saveSession(user);
        User loaded = db.loadSession();

        assertNotNull(loaded);
        assertEquals(1, loaded.getUserId());
        assertEquals("alice", loaded.getUsername());
        assertEquals("admin", loaded.getRole());

        db.clearSession();
        assertNull(db.loadSession());
    }

    @Test
    public void testInsertAndFetchFiles(@TempDir Path tempDir) throws Exception {
        SQLiteDB db = createIsolatedDb(tempDir);

        db.insertLocalFile("req-1", null, 5, "bob", "todo.txt", "rw", null, "content", "PENDING_CREATE");

        List<LocalFile> files = db.getAllOwnedFiles(5);
        assertEquals(1, files.size());
        LocalFile file = files.get(0);
        assertEquals("todo.txt", file.getFileName());
        assertEquals("bob", file.getUsername());
        assertEquals("rw", file.getPermission());
        assertEquals("PENDING_CREATE", file.getSyncState());
    }

    @Test
    public void testUpdateLocalFile(@TempDir Path tempDir) throws Exception {
        SQLiteDB db = createIsolatedDb(tempDir);
        db.insertLocalFile("req-2", null, 7, "carol", "note.txt", "rw", null, "old", "PENDING_CREATE");

        LocalFile file = db.getAllOwnedFiles(7).get(0);
        db.updateLocalFile(file.getLocalId(), "note2.txt", "new-content");

        String stored = db.getLocalFileContent(file.getLocalId());
        assertEquals("new-content", stored);
    }

    @Test
    public void testShareToHelpers(@TempDir Path tempDir) throws Exception {
        SQLiteDB db = createIsolatedDb(tempDir);
        db.insertLocalFile("req-3", 99, 8, "dana", "share.txt", "rw", null, "content", "SYNCED");

        assertEquals("", db.getShareTo(99));

        db.updateShareTo(99, "target:read");
        assertEquals("target:read", db.getShareTo(99));
    }

    @Test
    public void testIsFileExists(@TempDir Path tempDir) throws Exception {
        SQLiteDB db = createIsolatedDb(tempDir);
        db.insertLocalFile("req-4", 50, 9, "erin", "exists.txt", "rw", null, "content", "SYNCED");

        assertTrue(db.isFileExists(9, "exists.txt"));

        db.markDeleted(9, 50);
        assertFalse(db.isFileExists(9, "exists.txt"));
    }
}