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
        String sql 
                = "CREATE TABLE IF NOT EXISTS local_files (" 
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

    public ObservableList<LocalFile> getAllOwnedFiles(int userId) {
        ObservableList<LocalFile> files = FXCollections.observableArrayList();
        String sql
                = "SELECT * FROM local_files "
                + "WHERE owner_user_id = ? AND deleted = 0 ORDER BY updated_at DESC";
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

    public void cacheRemoteOwnedFiles(int userId, List<RemoteFile> remoteFiles) {
        String selectSql
                = "SELECT sync_state, deleted FROM local_files "
                + "WHERE remote_file_id = ? AND owner_user_id = ?";
        String insertSql 
                = "INSERT INTO local_files (remote_file_id, owner_user_id, name, permission, username, share_to, sync_state, deleted, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, 'SYNCED', 0, datetime('now'))";
        String updateSql
                = "UPDATE local_files SET name = ?, permission = ?, username = ?, share_to = ?, updated_at = datetime('now') "
                + "WHERE remote_file_id = ? AND owner_user_id = ?";
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
                            try (PreparedStatement insert = conn.prepareStatement(insertSql)) {
                                insert.setInt(1, file.getFileId());
                                insert.setInt(2, userId);
                                insert.setString(3, file.getName());
                                insert.setString(4, file.getPermission());
                                insert.setString(5, file.getOwnerName());
                                insert.setString(6, file.getSharedTo());
                                insert.executeUpdate();
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getUsername(int userId) {
        String sql
                = "SELECT username FROM local_files "
                + "WHERE owner_user_id = ? LIMIT 1";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("username");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "UNKNOWN";
    }
    
    public List<Integer> getPendingCreateUser() {
        List<Integer> userIds = new ArrayList<>();
        String sql
                = "SELECT DISTINCT owner_user_id FROM local_files "
                + "WHERE sync_state = 'PENDING_CREATE'";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                userIds.add(rs.getInt("owner_user_id"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return userIds;
    }
    
    public List<Integer> getPendingDeleteUser() {
        List<Integer> userIds = new ArrayList<>();
        String sql
                = "SELECT owner_user_id FROM local_files "
                + "WHERE sync_state = 'PENDING_DELETE'";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                userIds.add(rs.getInt("owner_user_id"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return userIds;
    }
    
    public void markPendingCreate(int userId, String username, String reqId, String name, String permission, String content) {
        String sql
                = "INSERT INTO local_files "
                + "(req_id, remote_file_id, owner_user_id, username, name, permission, share_to, content, sync_state, deleted, updated_at) "
                + "VALUES (?, NULL, ?, ?, ?, ?, NULL, ?, 'PENDING_CREATE', 0, datetime('now'))";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reqId);
            stmt.setInt(2, userId);
            stmt.setString(3, username);
            stmt.setString(4, name);
            stmt.setString(5, permission);
            stmt.setString(6, content);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public List<LocalFile> getPendingCreate(int userId) {
        List<LocalFile> files = new ArrayList<>();
        String sql = "SELECT * FROM local_files WHERE owner_user_id = ? AND sync_state = 'PENDING_CREATE'";
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
    
    public void markSendingCreate(String reqId) {
        String sql
                = "UPDATE local_files SET sync_state = 'CREATING' "
                + "WHERE req_id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reqId);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void resetSendingCreate() {
        String sql 
                = "UPDATE local_files SET sync_state = 'PENDING_CREATE' "
                + "WHERE sync_state = 'CREATING'";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void markPendingDelete(int userId, int fileId) {
        String sql
                = "UPDATE local_files SET deleted = 1, sync_state = 'PENDING_DELETE', updated_at = datetime('now') "
                + "WHERE remote_file_id = ? AND owner_user_id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, fileId);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public List<Integer> getPendingDelete(int userId) {
        List<Integer> fileIds = new ArrayList<>();
        String sql 
                = "SELECT remote_file_id FROM local_files "
                + "WHERE owner_user_id = ? AND sync_state = 'PENDING_DELETE'";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                fileIds.add(rs.getInt("remote_file_id"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileIds;
    }
    
    public void markSendingDelete(int userId, int fileId) {
        String sql 
                = "UPDATE local_files SET sync_state = 'DELETING' "
                + "WHERE owner_user_id = ? AND remote_file_id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, fileId);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void resetSendingDelete() {
        String sql
                = "UPDATE local_files SET sync_state = 'PENDING_DELETE' "
                + "WHERE sync_state = 'DELETING'";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void markDeleted(int userId, int fileId) {
        String sql 
                = "UPDATE local_files SET deleted = 1, updated_at = datetime('now') "
                + "WHERE remote_file_id = ? AND owner_user_id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, fileId);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void deletePendingCreate(int fileId) {
        String sql = "DELETE FROM local_files WHERE local_id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, fileId);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void finalizeDelete(int fileId) {
        String sql = "DELETE FROM local_files WHERE remote_file_id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, fileId);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void finalizeCreate(String reqId, int remoteFileId) {
        String sql
                = "UPDATE local_files SET remote_file_id = ?, sync_state = 'SYNCED', req_id = NULL, content = NULL, updated_at = datetime('now') "
                + "WHERE req_id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, remoteFileId);
            stmt.setString(2, reqId);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public String getShareTo(int remoteFileId) {
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
    
    public void updateShareTo(int remoteFileId, String newShareString) {
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
    
    public String getLocalFileContent(int localFileId) {
        String sql = "SELECT content FROM local_files WHERE local_id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, localFileId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("content");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public void updateLocalFile(int localId, String name, String content) {
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

    
}
