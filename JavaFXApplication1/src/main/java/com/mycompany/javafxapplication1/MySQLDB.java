/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
/**
 *
 * @author ntu-user
 */
public class MySQLDB {
    
    private int iterations = 10000;
    private int keyLength = 256;
    private String characters = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private Random random = new SecureRandom();
    private String saltValue;

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                "jdbc:mysql://lamp-server:3306/soft40051?useSSL=false",
                "admin",
                "GPx5ZPfEG0ek"
        );
    }
    
    public MySQLDB() {
        try {
            File fp = new File(".salt");
            if (!fp.exists()) {
                saltValue = getSaltValue(30);
                FileWriter fw = new FileWriter(fp);
                fw.write(saltValue);
                fw.close();
            } else {
                Scanner sc = new Scanner(fp);
                saltValue = sc.nextLine();
                sc.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getSaltValue(int length) {
        StringBuilder finalVal = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            finalVal.append(characters.charAt(random.nextInt(characters.length())));
        }
        return finalVal.toString();
    }

    private byte[] hash(char[] password, byte[] salt) throws InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
        Arrays.fill(password, Character.MIN_VALUE);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        } finally {
            spec.clearPassword();
        }
    }

    public String hashPassword(String password) {
        try {
            byte[] securePassword = hash(password.toCharArray(), saltValue.getBytes());
            return Base64.getEncoder().encodeToString(securePassword);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean verifyPassword(String password, String storedHash) {
        String inputHash = hashPassword(password);
        return storedHash.equals(inputHash);
    }

    public void ensureDefaultAdmin() throws Exception {
        try (Connection conn = getConnection()) {
            PreparedStatement check = conn.prepareStatement(
                "SELECT user_id FROM users WHERE username = 'admin' LIMIT 1"
            );
            ResultSet rs = check.executeQuery();
            if (rs.next()) return;
            String hash = hashPassword("admin");
            PreparedStatement insert = conn.prepareStatement(
                "INSERT INTO users (username, password_hash, role) VALUES ('admin', ?, 'admin')"
            );
            insert.setString(1, hash);
            insert.executeUpdate();
        }
    }
    
    public void testConnection() throws Exception {
        Connection conn = getConnection();
        conn.close();
    }
    
    public User getUserByName(String username) throws Exception {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new User(
                    rs.getInt("user_id"),
                    rs.getString("username"),
                    rs.getString("password_hash"),
                    rs.getString("role")
                );
            }
        }
        return null;
    }
    
    public User getUserById(int userId) throws Exception {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new User(
                    rs.getInt("user_id"),
                    rs.getString("username"),
                    rs.getString("password_hash"),
                    rs.getString("role")
                );
            }
        }
        return null;
    }

    public void addUser(String username, String hash, String role) throws Exception {
        String sql = "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, hash);
            stmt.setString(3, role);
            stmt.executeUpdate();
        }
    }

    public void deleteUser(int userId) throws Exception {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        }
    }

    public void updateRole(int userId, String role) throws Exception {
        String sql = "UPDATE users SET role = ? WHERE user_id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, role);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        }
    }

    public void updatePassword(int userId, String hash) throws Exception {
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, hash);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        }
    }

    public ObservableList<User> getAllUsers() throws Exception {
        ObservableList<User> list = FXCollections.observableArrayList();
        String sql = "SELECT * FROM users";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(new User(
                    rs.getInt("user_id"),
                    rs.getString("username"),
                    rs.getString("password_hash"),
                    rs.getString("role")
                ));
            }
        }
        return list;
    }
    
    public List<FileModel> getAllFilesByUser(int userId) throws Exception {
        String sql = "SELECT id, owner_user_id, name, logical_path, size_bytes FROM files WHERE owner_user_id = ? AND is_deleted = 0";
        List<FileModel> files = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                files.add(new FileModel(
                    rs.getInt("id"),
                    rs.getInt("owner_user_id"),
                    rs.getString("name"),
                    rs.getString("logical_path"),
                    rs.getInt("size_bytes")
                ));
            }
        }
        return files;
    }
    
    public List<FileModel> getSharedFilesByUser(int userId) throws Exception {
        String sql = """
            SELECT f.id, f.owner_user_id, f.name, f.logical_path, f.size_bytes,
                   fs.permission, u.username AS owner_name
            FROM file_shares fs
            JOIN files f ON fs.file_id = f.id
            JOIN users u ON f.owner_user_id = u.user_id
            WHERE fs.target_user_id = ?
              AND f.is_deleted = 0
        """;
        List<FileModel> files = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                FileModel fm = new FileModel(
                        rs.getInt("id"),
                        rs.getInt("owner_user_id"),
                        rs.getString("name"),
                        rs.getString("logical_path"),
                        rs.getInt("size_bytes")
                );
                fm.setOwnerName(rs.getString("owner_name"));
                fm.setPermission(rs.getString("permission"));
                files.add(fm);
            }
        }
        return files;
    }

    public void log(Integer userId, String username, String action, String detail) throws Exception {
        try (Connection conn = getConnection()) {
            String sql = "INSERT INTO logs (user_id, username, action, detail) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            if (userId == null) stmt.setNull(1, Types.INTEGER);
            else stmt.setInt(1, userId);
            stmt.setString(2, username);  
            stmt.setString(3, action);
            stmt.setString(4, detail);
            stmt.executeUpdate();
        }
    }

}