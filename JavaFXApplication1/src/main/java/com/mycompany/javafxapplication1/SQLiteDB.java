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
/**
 *
 * @author ntu-user
 */
public class SQLiteDB {
    
    private String dbUrl = "jdbc:sqlite:soft40051.db";
    private int timeout = 30;

    public SQLiteDB() {
        createLocalSessionTable();
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
                return new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        "",
                        rs.getString("role")
                );
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
    
    public void log(String msg) {
        System.out.println("[SQLiteDB] " + msg);
    }
    
}