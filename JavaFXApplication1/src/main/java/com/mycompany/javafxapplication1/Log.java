/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

/**
 *
 * @author ntu-user
 */
public class Log {
    
    private int id;
    private int userId;
    private String username;
    private String action;
    private String detail;
    private String timestamp;

    public Log(int id, int userId, String username, String action, String detail, String timestamp) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.action = action;
        this.detail = detail;
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getAction() {
        return action;
    }

    public String getDetail() {
        return detail;
    }

    public String getTimestamp() {
        return timestamp;
    }
}