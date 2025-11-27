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

    private SimpleIntegerProperty userId;
    private SimpleStringProperty username;
    private SimpleStringProperty passwordHash;
    private SimpleStringProperty role;

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
}