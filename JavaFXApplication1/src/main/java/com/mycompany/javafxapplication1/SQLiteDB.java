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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
        String sql = "CREATE TABLE IF NOT EXISTS local_files (" 
                   + "local_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                   + "remote_file_id INTEGER, "
                   + "req_id TEXT, "
                   + "owner_user_id INTEGER NOT NULL, "
                   + "username TEXT NOT NULL, "
                   + "name TEXT NOT NULL, "
                   + "permission TEXT NOT NULL, "
                   + "share_to TEXT, "
                   + "content TEXT, "
                   + "sync_state TEXT NOT NULL DEFAULT 'SYNCED', "
                   + "deleted INTEGER NOT NULL DEFAULT 0, "
                   + "updated_at TEXT);";
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
        String sql = "SELECT * FROM local_session";
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

    public ObservableList<LocalFile> getAllOwnedFiles(Integer userId) {
        ObservableList<LocalFile> files = FXCollections.observableArrayList();
        String sql = "SELECT * FROM local_files WHERE owner_user_id = ? AND deleted = 0 ORDER BY updated_at DESC";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                LocalFile lf = new LocalFile();
                lf.setLocalId(rs.getInt("local_id"));
                lf.setRemoteFileId(rs.getObject("remote_file_id") == null ? null : rs.getInt("remote_file_id"));
                lf.setReqId(rs.getString("req_id"));
                lf.setUserId(rs.getInt("owner_user_id"));
                lf.setUsername(rs.getString("username"));
                lf.setName(rs.getString("name"));
                lf.setPermission(rs.getString("permission"));
                lf.setSharedTo(rs.getString("share_to"));
                lf.setContent(rs.getString("content"));
                lf.setSyncState(rs.getString("sync_state"));
                lf.setDeleted(rs.getInt("deleted") == 1);
                lf.setUpdatedAt(rs.getString("updated_at"));
                files.add(lf);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return files;
    }

    public void cacheRemoteOwnedFiles(Integer userId, List<RemoteFile> remoteFiles) {
        String selectSql = "SELECT sync_state, deleted FROM local_files WHERE remote_file_id = ? AND owner_user_id = ?";
        String updateSql = "UPDATE local_files SET name = ?, permission = ?, username = ?, share_to = ?, updated_at = datetime('now') " +
                           "WHERE remote_file_id = ? AND owner_user_id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            for (RemoteFile file : remoteFiles) {
                try (PreparedStatement check = conn.prepareStatement(selectSql)) {
                    check.setInt(1, file.getFileId());
                    check.setInt(2, userId);
                    try (ResultSet rs = check.executeQuery()) {
                        if (rs.next()) {
                            String syncState = rs.getString("sync_state");
                            boolean deleted = rs.getInt("deleted") == 1;
                            if (!"SYNCED".equals(syncState) || deleted) {
                                continue;
                            }
                            try (PreparedStatement update = conn.prepareStatement(updateSql)) {
                                update.setString(1, file.getName());
                                update.setString(2, file.getPermission());
                                update.setString(3, file.getOwnerName());
                                update.setString(4, file.getSharedTo());
                                update.setInt(5, file.getFileId());
                                update.setInt(6, userId);
                                update.executeUpdate();
                            }
                        } else {
                            insertLocalFile(null, file.getFileId(), userId, file.getOwnerName(), file.getName(), file.getPermission(), file.getSharedTo(), null, "SYNCED");
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void insertLocalFile(String reqId, Integer remoteFileId, Integer userId, String username, String name, String permission, String shareTo, String content, String syncState) {
        String sql = "INSERT INTO local_files (req_id, remote_file_id, owner_user_id, username, name, permission, share_to, content, sync_state, deleted, updated_at) " 
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, datetime('now'))";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reqId);
            stmt.setObject(2, remoteFileId);
            stmt.setInt(3, userId);
            stmt.setString(4, username);
            stmt.setString(5, name);
            stmt.setString(6, permission);
            stmt.setString(7, shareTo);
            stmt.setString(8, content);
            stmt.setString(9, syncState);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<LocalFile> getFilesByState(String syncState) {
        List<LocalFile> files = new ArrayList<>();
        String sql = "SELECT * FROM local_files WHERE sync_state = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, syncState);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                LocalFile lf = new LocalFile();
                lf.setLocalId(rs.getInt("local_id"));
                lf.setRemoteFileId(rs.getObject("remote_file_id") == null ? null : rs.getInt("remote_file_id"));
                lf.setReqId(rs.getString("req_id"));
                lf.setUserId(rs.getInt("owner_user_id"));
                lf.setUsername(rs.getString("username"));
                lf.setName(rs.getString("name"));
                lf.setSharedTo(rs.getString("share_to"));
                lf.setContent(rs.getString("content"));
                files.add(lf);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return files;
    }

    public void updateSyncState(String newState, Integer remoteFileId, String reqId) {
        String sql;
        if (remoteFileId != null) {
            sql = "UPDATE local_files SET sync_state = ?, updated_at = datetime('now') WHERE remote_file_id = ?";
        } else {
            sql = "UPDATE local_files SET sync_state = ?, updated_at = datetime('now') WHERE req_id = ?";
        }
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newState);
            if (remoteFileId != null) {
                stmt.setInt(2, remoteFileId);
            } else {
                stmt.setString(2, reqId);
            }
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void resetSyncState(String fromState, String toState) {
        String sql = "UPDATE local_files SET sync_state = ? WHERE sync_state = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, toState);
            stmt.setString(2, fromState);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void markAsDeleted(Integer userId, Integer remoteFileId) {
        String sql = "UPDATE local_files SET deleted = 1, sync_state = 'PENDING_DELETE', updated_at = datetime('now') WHERE remote_file_id = ? AND owner_user_id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, remoteFileId);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void deleteLocalFile(Integer localId) {
        String sql = "DELETE FROM local_files WHERE local_id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, localId);
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
    
    public void finalizeCreate(String reqId, Integer remoteFileId) {
        String sql = "UPDATE local_files SET remote_file_id = ?, sync_state = 'SYNCED', req_id = NULL, content = NULL, updated_at = datetime('now') WHERE req_id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, remoteFileId);
            stmt.setString(2, reqId);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void finalizeUpdate(Integer remoteFileId) {
        String sql = "UPDATE local_files SET sync_state = 'SYNCED', updated_at = datetime('now') WHERE remote_file_id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, remoteFileId);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void updateRemoteFileContent(Integer userId, Integer remoteFileId, String newName, String newContent, String newState) {
        String sql = "UPDATE local_files SET name = ?, content = ?, sync_state = ?, updated_at = datetime('now') WHERE remote_file_id = ? AND owner_user_id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newName);
            stmt.setString(2, newContent);
            stmt.setString(3, newState);
            stmt.setInt(4, remoteFileId);
            stmt.setInt(5, userId);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void updateLocalFile(Integer localId, String name, String content) {
        String sql = "UPDATE local_files SET name = ?, content = ? WHERE local_id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, content);
            ps.setInt(3, localId);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public String getLocalFileContent(Integer localFileId) {
        String sql = "SELECT content FROM local_files WHERE local_id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, localFileId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getString("content");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public String getShareTo(Integer remoteFileId) {
        String sql = "SELECT share_to FROM local_files WHERE remote_file_id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, remoteFileId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String val = rs.getString("share_to");
                return val == null ? "" : val;
            }
        } catch (Exception e) {
            e.printStackTrace(); 
        }
        return "";
    }
    
    public void updateShareTo(Integer remoteFileId, String newShareString) {
        String sql = "UPDATE local_files SET share_to = ?, updated_at = datetime('now') WHERE remote_file_id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newShareString);
            stmt.setInt(2, remoteFileId);
            stmt.executeUpdate();
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
    }
    
    public boolean isFileExists(Integer userId, String fileName) {
        String sql = "SELECT 1 FROM local_files WHERE owner_user_id = ? AND name = ? AND deleted = 0";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, fileName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}