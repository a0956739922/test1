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

    public User login(String username, String password) throws Exception {
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
            remote.log(user.getUserId(), username, "LOGIN_SUCCESS", "");
            return user;
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

    public void deleteUser(User actor, User target) throws Exception {
        if (target.getUsername().equals("admin")) throw new IllegalArgumentException("CANNOT_DELETE_DEFAULT_ADMIN");
        Integer currentUser = actor.getUserId();
        if (actor.getUserId() == target.getUserId()) {
            currentUser = null;
        }
        remote.deleteUser(target.getUserId());
        remote.log(currentUser, actor.getUsername(), "DELETE_USER", "target_id=" + target.getUserId());
    }

    public void promote(User admin, User target) throws Exception {
        remote.updateRole(target.getUserId(), "admin");
        remote.log(admin.getUserId(), admin.getUsername(), "PROMOTE_USER", "target_id=" + target.getUserId());
    }

    public void demote(User admin, User target) throws Exception {
        remote.updateRole(target.getUserId(), "standard");
        remote.log(admin.getUserId(), admin.getUsername(), "DEMOTE_USER", "target_id=" + target.getUserId());
    }

    public void updatePassword(User user, String currentPass, String newPass) throws Exception {
        if (newPass.isEmpty()) throw new IllegalArgumentException("PASSWORD_EMPTY");
        User fresh = remote.getUserByName(user.getUsername());
        if (!remote.verifyPassword(currentPass, fresh.getPasswordHash()))
            throw new IllegalArgumentException("WRONG_CURRENT_PASSWORD");
        remote.updatePassword(fresh.getUserId(), remote.hashPassword(newPass));
        remote.log(fresh.getUserId(), fresh.getUsername(), "UPDATE_PASSWORD", "target_id=" + fresh.getUserId());
    }
    
    public void adminUpdatePassword(User admin, User target, String newPass) throws Exception {
        if (newPass.isEmpty()) throw new IllegalArgumentException("PASSWORD_EMPTY");
        remote.updatePassword(target.getUserId(), remote.hashPassword(newPass));
        remote.log(admin.getUserId(),admin.getUsername(),"ADMIN_UPDATE_PASSWORD","target_id=" + target.getUserId());
    }

    public ObservableList<User> getAllUsers() throws Exception {
        return remote.getAllUsers();
    }
    
}
