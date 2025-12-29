/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Project/Maven2/JavaApp/src/main/java/${packagePath}/${mainClassName}.java to edit this template
 */
package com.mycompany.javafxapplication1;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
/**
 *
 * @author ntu-user
 */
public class SQLiteDB {
    
    private String dbUrl = "jdbc:sqlite:soft40051.db";
    private int timeout = 30;

    public SQLiteDB() {
        createLocalSessionTable();
        createLocalFilesTable();
    }
        
    private void createLocalSessionTable() {
        String sql = "CREATE TABLE IF NOT EXISTS local_session (" +
                 "user_id INTEGER, " +
                 "username TEXT, " +
                 "role TEXT, " +
                 "last_login TEXT" +
                 ");";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {
            stmt.setQueryTimeout(timeout);
            stmt.executeUpdate(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void createLocalFilesTable() {
        String sql = "CREATE TABLE IF NOT EXISTS local_files (" +
                "local_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "remote_file_id INTEGER, " +
                "owner_user_id INTEGER NOT NULL, " +
                "name TEXT NOT NULL, " +
                "logical_path TEXT NOT NULL, " +
                "content TEXT, " +
                "sync_state TEXT NOT NULL, " +
                "updated_at INTEGER" +
                ");";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {
            stmt.setQueryTimeout(timeout);
            stmt.executeUpdate(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void saveSession(User user) {
        clearSession();
        String sql = "INSERT INTO local_session (user_id, username, role, last_login) VALUES (?, ?, ?, datetime('now'))";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, user.getUserId());
            stmt.setString(2, user.getUsername());
            stmt.setString(3, user.getRole());
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public User loadSession() {
        String sql = "SELECT * FROM local_session LIMIT 1";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return new User(rs.getInt("user_id"), rs.getString("username"), "", rs.getString("role"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public void clearSession() {
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM local_session");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<FileModel> getAllOwnedFiles(int userId) {
        List<FileModel> files = new ArrayList<>();
        String sql = "SELECT * FROM local_files WHERE owner_user_id = ? AND sync_state != 'PENDING_DELETE' AND sync_state != 'SENDING_DELETE' ORDER BY updated_at DESC";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                files.add(new FileModel(rs.getInt("remote_file_id"), rs.getInt("owner_user_id"), rs.getString("name"), rs.getString("logical_path")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return files;
    }

    public void cacheRemoteOwnedFiles(int userId, List<FileModel> remoteFiles) {
        String selectSql = "SELECT sync_state FROM local_files WHERE remote_file_id = ? AND owner_user_id = ?";
        String insertSql = "INSERT INTO local_files (remote_file_id, owner_user_id, name, logical_path, sync_state, updated_at) VALUES (?, ?, ?, ?, 'SYNCED', ?)";
        String updateSql = "UPDATE local_files SET name = ?, logical_path = ?, updated_at = ? WHERE remote_file_id = ? AND owner_user_id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            for (FileModel f : remoteFiles) {
                PreparedStatement check = conn.prepareStatement(selectSql);
                check.setInt(1, f.getId());
                check.setInt(2, userId);
                ResultSet rs = check.executeQuery();
                long now = System.currentTimeMillis();
                if (rs.next()) {
                    String state = rs.getString("sync_state");
                    if ("PENDING_DELETE".equals(state) || "SENDING_DELETE".equals(state)) {
                        continue;
                    }
                    PreparedStatement update = conn.prepareStatement(updateSql);
                    update.setString(1, f.getName());
                    update.setString(2, f.getLogicalPath());
                    update.setLong(3, now);
                    update.setInt(4, f.getId());
                    update.setInt(5, userId);
                    update.executeUpdate();
                } else {
                    PreparedStatement insert = conn.prepareStatement(insertSql);
                    insert.setInt(1, f.getId());
                    insert.setInt(2, userId);
                    insert.setString(3, f.getName());
                    insert.setString(4, f.getLogicalPath());
                    insert.setLong(5, now);
                    insert.executeUpdate();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void markPendingDelete(int ownerUserId, int remoteFileId) {
        String sql = "UPDATE local_files SET sync_state = 'PENDING_DELETE', updated_at = ? WHERE remote_file_id = ? AND owner_user_id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, System.currentTimeMillis());
            stmt.setInt(2, remoteFileId);
            stmt.setInt(3, ownerUserId);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Integer> getPendingDeleteId(int userId) {
        List<Integer> id = new ArrayList<>();
        String sql = "SELECT remote_file_id FROM local_files WHERE owner_user_id = ? AND sync_state = 'PENDING_DELETE'";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                id.add(rs.getInt("remote_file_id"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return id;
    }
    
    public void markSendingDelete(int ownerUserId, int remoteFileId) {
        String sql = "UPDATE local_files SET sync_state = 'SENDING_DELETE', updated_at = ? WHERE remote_file_id = ? AND owner_user_id = ? AND sync_state = 'PENDING_DELETE'";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, System.currentTimeMillis());
            stmt.setInt(2, remoteFileId);
            stmt.setInt(3, ownerUserId);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void finalizeDelete(int remoteFileId) {
        String sql = "DELETE FROM local_files WHERE remote_file_id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, remoteFileId);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}