/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

import javafx.collections.ObservableList;
/**
 *
 * @author ntu-user
 */
public class UserService {

    private MySQLDB remote = new MySQLDB();
    private SQLiteDB local = new SQLiteDB();

    public void login(String username, String password) throws Exception {
        try {
            remote.ensureDefaultAdmin();
        } catch (Exception e) {
            System.out.println("[UserService] MySQL unreachable before login, skip ensureDefaultAdmin.");
        }
        try {
            User user = remote.getUserByName(username);
            if (user == null) {
                remote.log(null, username, "LOGIN_FAIL", "User not found: ");
                throw new IllegalArgumentException("USER_NOT_FOUND");
            }
            if (!remote.verifyPassword(password, user.getPasswordHash())) {
                remote.log(null, username, "LOGIN_FAIL", "Wrong password");
                throw new IllegalArgumentException("PASSWORD_WRONG");
            }
            local.saveSession(user);
            remote.log(user.getUserId(), username, "LOGIN_SUCCESS", "");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            System.out.println("[UserService] MySQL unreachable, offline mode fallback.");
            throw e;
        }
    }

    public void createUser(String username, String password, String role) throws Exception {
        if (username == null) throw new IllegalArgumentException("USERNAME_EMPTY");
        if (username.contains(" ")) throw new IllegalArgumentException("USERNAME_SPACE");
        if (remote.getUserByName(username) != null) throw new IllegalArgumentException("USERNAME_EXISTS");
        if (password == null || password.isEmpty()) throw new IllegalArgumentException("PASSWORD_EMPTY");
        remote.addUser(username, remote.hashPassword(password), role);
        remote.log(null, username, "CREATE_USER", "role=" + role);
    }

    public void deleteUser(int userId) throws Exception {
        String deletedName = remote.getUserById(userId).getUsername();
        remote.deleteUser(userId);
        remote.log(null, deletedName, "DELETE_USER", "id=" + userId);
    }

    public void promote(int userId) throws Exception {
        String username = remote.getUserById(userId).getUsername();
        remote.updateRole(userId, "admin");
        remote.log(userId, username, "PROMOTE_USER", "id=" + userId);
    }

    public void demote(int userId) throws Exception {
        String username = remote.getUserById(userId).getUsername();
        remote.updateRole(userId, "standard");
        remote.log(userId, username, "DEMOTE_USER", "id=" + userId);
    }

    public void updatePassword(int userId, String newPass) throws Exception {
        if (newPass.isEmpty()) throw new IllegalArgumentException("PASSWORD_EMPTY");
        String username = remote.getUserById(userId).getUsername();
        remote.updatePassword(userId, remote.hashPassword(newPass));
        remote.log(userId, username, "UPDATE_PASSWORD", "id=" + userId);
    }

    public ObservableList<User> getAllUsers() throws Exception {
        return remote.getAllUsers();
    }

    public User getSessionUser() {
        return local.loadSession();
    }
    
    public void logout() {
        local.clearSession();
    }
}
