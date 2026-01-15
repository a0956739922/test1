package com.mycompany.javafxapplication1;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.json.JsonObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FileService.
 */
public class FileServiceTest {

    private static class StubMySQLDB extends MySQLDB {
        final List<String> logs = new ArrayList<>();

        @Override
        public void log(Integer userId, String username, String action, String detail) {
            logs.add(action + ":" + detail);
        }
    }

    private static class StubSQLiteDB extends SQLiteDB {
        private String shareTo = "";
        private final java.util.Map<Integer, String> contents = new java.util.HashMap<>();

        @Override
        public String getShareTo(Integer remoteFileId) {
            return shareTo == null ? "" : shareTo;
        }

        @Override
        public void updateShareTo(Integer remoteFileId, String newShareString) {
            this.shareTo = newShareString;
        }

        @Override
        public String getLocalFileContent(Integer localFileId) {
            return contents.get(localFileId);
        }

        void setLocalFileContent(Integer localFileId, String content) {
            contents.put(localFileId, content);
        }
    }

    private static class StubMqttPubUI extends MqttPubUI {
        JsonObject lastPayload;

        @Override
        public void send(JsonObject json) {
            lastPayload = json;
        }
    }

    private FileService buildService(StubMySQLDB remote, StubSQLiteDB local, StubMqttPubUI mqtt) throws Exception {
        FileService service = new FileService();
        Field remoteField = FileService.class.getDeclaredField("remote");
        remoteField.setAccessible(true);
        remoteField.set(service, remote);
        Field localField = FileService.class.getDeclaredField("local");
        localField.setAccessible(true);
        localField.set(service, local);
        Field mqttField = FileService.class.getDeclaredField("mqtt");
        mqttField.setAccessible(true);
        mqttField.set(service, mqtt);
        return service;
    }

    @Test
    @DisplayName("create publishes MQTT payload and returns request id")
    public void testCreatePublishesRequest() throws Exception {
        StubMySQLDB remote = new StubMySQLDB();
        StubSQLiteDB local = new StubSQLiteDB();
        StubMqttPubUI mqtt = new StubMqttPubUI();
        FileService service = buildService(remote, local, mqtt);

        String reqId = service.create("req-1", 7, "alice", "doc.txt", "hello");

        assertEquals("req-1", reqId);
        assertNotNull(mqtt.lastPayload);
        assertEquals("create", mqtt.lastPayload.getString("action"));
        assertEquals("doc.txt", mqtt.lastPayload.getString("fileName"));
        assertEquals("hello", mqtt.lastPayload.getString("content"));
    }

    @Test
    @DisplayName("finalizeLocalShare merges and updates share list")
    public void testFinalizeLocalShare() throws Exception {
        StubMySQLDB remote = new StubMySQLDB();
        StubSQLiteDB local = new StubSQLiteDB();
        local.updateShareTo(10, "alice:read, bob:write");
        StubMqttPubUI mqtt = new StubMqttPubUI();
        FileService service = buildService(remote, local, mqtt);

        service.finalizeLocalShare(10, "bob", "read");

        assertEquals("alice:read,bob:read", local.getShareTo(10));
    }

    @Test
    @DisplayName("downloadLocal writes stored content to file")
    public void testDownloadLocal(@TempDir Path tempDir) throws Exception {
        StubMySQLDB remote = new StubMySQLDB();
        StubSQLiteDB local = new StubSQLiteDB();
        StubMqttPubUI mqtt = new StubMqttPubUI();
        FileService service = buildService(remote, local, mqtt);

        local.setLocalFileContent(4, "sample-content");
        File target = tempDir.resolve("out.txt").toFile();

        service.downloadLocal(4, target);

        String stored = Files.readString(target.toPath());
        assertEquals("sample-content", stored);
    }
}
