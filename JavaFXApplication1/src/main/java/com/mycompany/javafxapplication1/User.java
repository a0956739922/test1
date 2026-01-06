/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
/**
 *
 * @author ntu-user
 */
public class User {

    private final SimpleIntegerProperty userId;
    private final SimpleStringProperty username;
    private final SimpleStringProperty passwordHash;
    private final SimpleStringProperty role;

    public User(int userId, String username, String passwordHash, String role) {
        this.userId = new SimpleIntegerProperty(userId);
        this.username = new SimpleStringProperty(username);
        this.passwordHash = new SimpleStringProperty(passwordHash);
        this.role = new SimpleStringProperty(role);
    }

    public int getUserId() {
        return userId.get();
    }

    public String getUsername() {
        return username.get();
    }

    public String getPasswordHash() {
        return passwordHash.get();
    }

    public String getRole() {
        return role.get();
    }

    public void setRole(String role) {
        this.role.set(role);
    }

    public SimpleStringProperty roleProperty() {
        return role;
    }
}