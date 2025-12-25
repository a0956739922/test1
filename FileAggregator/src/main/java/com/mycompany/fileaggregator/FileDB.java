/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.fileaggregator;

import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonWriter;
/**
 *
 * @author ntu-user
 */
public class FileDB {

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                "jdbc:mysql://lamp-server:3306/soft40051?useSSL=false",
                "admin",
                "GPx5ZPfEG0ek"
        );
    }

    private String toJson(JsonObject obj) {
        StringWriter sw = new StringWriter();
        try (JsonWriter writer = Json.createWriter(sw)) {
            writer.writeObject(obj);
        }
        return sw.toString();
    }

    private JsonObject fromJson(String json) {
        if (json == null) return null;
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            return reader.readObject();
        }
    }

    public int insertFile(int ownerId, JsonObject metadata) throws Exception {
        String sql =
            "INSERT INTO files (owner_user_id, name, logical_path, size_bytes, metadata, is_deleted, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, FALSE, NOW())";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, ownerId);
            stmt.setString(2, metadata.getString("file_name"));
            stmt.setString(3, metadata.getString("logical_path"));
            stmt.setInt(4, metadata.getJsonNumber("size_bytes").intValue());
            stmt.setString(5, toJson(metadata));
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return -1;
    }

    public void updateFile(int fileId, String newName, String newLogicalPath, Integer newSize) throws Exception {
        String sql;
        if (newSize == null) {
            sql = "UPDATE files SET name = ?, logical_path = ?, updated_at = NOW() WHERE id = ?";
        } else {
            sql = "UPDATE files SET name = ?, logical_path = ?, size_bytes = ?, updated_at = NOW() WHERE id = ?";
        }
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newName);
            stmt.setString(2, newLogicalPath);
            if (newSize == null) {
                stmt.setInt(3, fileId);
            } else {
                stmt.setInt(3, newSize);
                stmt.setInt(4, fileId);
            }
            stmt.executeUpdate();
        }
    }

    public void updateMetadata(int fileId, JsonObject metadata) throws Exception {
        String sql = "UPDATE files SET metadata = ?, updated_at = NOW() WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, toJson(metadata));
            stmt.setInt(2, fileId);
            stmt.executeUpdate();
        }
    }

    public JsonObject getMetadata(int fileId) throws Exception {
        String sql = "SELECT metadata FROM files WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, fileId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return fromJson(rs.getString("metadata"));
            }
        }
        return null;
    }

    public JsonObject getFileById(int fileId) throws Exception {
        String sql = "SELECT * FROM files WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, fileId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Json.createObjectBuilder()
                        .add("id", rs.getInt("id"))
                        .add("owner_user_id", rs.getInt("owner_user_id"))
                        .add("name", rs.getString("name"))
                        .add("logical_path", rs.getString("logical_path"))
                        .add("size_bytes", rs.getInt("size_bytes"))
                        .add("is_deleted", rs.getBoolean("is_deleted"))
                        .add("metadata", fromJson(rs.getString("metadata")))
                        .build();
            }
        }
        return null;
    }

    public void markFileDeleted(int fileId) throws Exception {
        String sql = "UPDATE files SET is_deleted = TRUE, updated_at = NOW() WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, fileId);
            stmt.executeUpdate();
        }
    }

    public void insertShare(int fileId, int ownerId, int targetId, String permission) throws Exception {
        String sql =
            "INSERT INTO file_shares (file_id, owner_user_id, target_user_id, permission, updated_at) " +
            "VALUES (?, ?, ?, ?, NOW())";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, fileId);
            stmt.setInt(2, ownerId);
            stmt.setInt(3, targetId);
            stmt.setString(4, permission);
            stmt.executeUpdate();
        }
    }

    public void updateShare(int fileId, int targetId, String permission) throws Exception {
        String sql =
            "UPDATE file_shares SET permission = ?, updated_at = NOW() " +
            "WHERE file_id = ? AND target_user_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, permission);
            stmt.setInt(2, fileId);
            stmt.setInt(3, targetId);
            stmt.executeUpdate();
        }
    }

    public void deleteShares(int fileId) throws Exception {
        String sql = "DELETE FROM file_shares WHERE file_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, fileId);
            stmt.executeUpdate();
        }
    }

    public boolean shareExists(int fileId, int targetId) throws Exception {
        String sql = "SELECT 1 FROM file_shares WHERE file_id = ? AND target_user_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, fileId);
            stmt.setInt(2, targetId);
            return stmt.executeQuery().next();
        }
    }

    public void log(Integer userId, String username, String action, String detail) throws Exception {
        String sql = "INSERT INTO logs (user_id, username, action, detail) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (userId == null) stmt.setNull(1, Types.INTEGER);
            else stmt.setInt(1, userId);
            stmt.setString(2, username);
            stmt.setString(3, action);
            stmt.setString(4, detail);
            stmt.executeUpdate();
        }
    }
}