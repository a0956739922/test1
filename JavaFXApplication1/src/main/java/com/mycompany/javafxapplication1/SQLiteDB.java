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
        createLocalFilesTable();
    }

    private void createLocalFilesTable() {
        String sql = "CREATE TABLE IF NOT EXISTS local_files ("
                + "local_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "remote_file_id INTEGER, "
                + "owner_user_id INTEGER NOT NULL, "
                + "username TEXT NOT NULL, "
                + "name TEXT NOT NULL, "
                + "logical_path TEXT NOT NULL, "
                + "permission TEXT NOT NULL, "
                + "share_to TEXT , "
                + "content TEXT, "
                + "pending INTEGER NOT NULL DEFAULT 0, "
                + "deleted INTEGER NOT NULL DEFAULT 0, "
                + "updated_at TEXT"
                + ");";
        try (Connection conn = DriverManager.getConnection(dbUrl); Statement stmt = conn.createStatement()) {
            stmt.setQueryTimeout(timeout);
            stmt.executeUpdate(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<FileModel> getAllOwnedFiles(int userId) {
        List<FileModel> files = new ArrayList<>();
        String sql = " SELECT * FROM local_files WHERE owner_user_id = ? AND pending = 0 AND deleted = 0 ORDER BY updated_at DESC";
        try (Connection conn = DriverManager.getConnection(dbUrl); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                FileModel fm = new FileModel(rs.getInt("remote_file_id"), rs.getInt("owner_user_id"), rs.getString("name"), rs.getString("logical_path"));
                fm.setPermission(rs.getString("permission"));
                fm.setOwnerName(rs.getString("username"));
                fm.setSharedTo(rs.getString("share_to"));
                files.add(fm);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return files;
    }

    public void cacheRemoteOwnedFiles(int userId, List<FileModel> remoteFiles) {
        String selectSql = "SELECT pending, deleted FROM local_files WHERE remote_file_id = ? AND owner_user_id = ?";
        String insertSql
                = "INSERT INTO local_files "
                + "(remote_file_id, owner_user_id, name, logical_path, permission, username, share_to, pending, deleted, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, 0, 0, datetime('now'))";
        String updateSql
                = "UPDATE local_files SET name = ?, logical_path = ?, permission = ?, "
                + "username = ?, share_to = ?, updated_at = datetime('now') "
                + "WHERE remote_file_id = ? AND owner_user_id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            for (FileModel f : remoteFiles) {
                PreparedStatement check = conn.prepareStatement(selectSql);
                check.setInt(1, f.getId());
                check.setInt(2, userId);
                ResultSet rs = check.executeQuery();
                if (rs.next()) {
                    int deleted = rs.getInt("deleted");
                    if (deleted == 1) {
                        continue;
                    }
                    PreparedStatement update = conn.prepareStatement(updateSql);
                    update.setString(1, f.getName());
                    update.setString(2, f.getLogicalPath());
                    update.setString(3, f.getPermission());
                    update.setString(4, f.getOwnerName());
                    update.setString(5, f.getSharedTo());
                    update.setInt(6, f.getId());
                    update.setInt(7, userId);
                    update.executeUpdate();

                } else {
                    PreparedStatement insert = conn.prepareStatement(insertSql);
                    insert.setInt(1, f.getId());
                    insert.setInt(2, userId);
                    insert.setString(3, f.getName());
                    insert.setString(4, f.getLogicalPath());
                    insert.setString(5, f.getPermission());
                    insert.setString(6, f.getOwnerName());
                    insert.setString(7, f.getSharedTo());
                    insert.executeUpdate();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void markPendingDelete(int ownerUserId, int remoteFileId) {
        String sql
                = "UPDATE local_files SET pending = 1, deleted = 1, updated_at = datetime('now') "
                + "WHERE remote_file_id = ? AND owner_user_id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, remoteFileId);
            stmt.setInt(2, ownerUserId);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void finalizeDelete(int remoteFileId) {
        String sql = "DELETE FROM local_files WHERE remote_file_id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, remoteFileId);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Integer> getPendingId(int userId) {
        List<Integer> id = new ArrayList<>();
        String sql = "SELECT remote_file_id FROM local_files WHERE owner_user_id = ? AND pending = 1";
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
    
    public void clearPending(int ownerUserId, int remoteFileId) {
        String sql ="UPDATE local_files SET pending = 0, updated_at = datetime('now') WHERE owner_user_id = ? AND remote_file_id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, ownerUserId);
            stmt.setInt(2, remoteFileId);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
