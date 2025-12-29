/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

/**
 *
 * @author ntu-user
 */
public class FileModel {
    private int id;
    private int ownerUserId;
    private String name;
    private String logicalPath;
    private String ownerName;
    private String permission;

    public FileModel(int id, int ownerUserId, String name, String logicalPath) {
        this.id = id;
        this.ownerUserId = ownerUserId;
        this.name = name;
        this.logicalPath = logicalPath;
    }

    public int getId() {
        return id;
    }
    
    public int getOwnerUserId() {
        return ownerUserId;
    }
    
    public String getName() {
        return name;
    }
    
    public String getLogicalPath() {
        return logicalPath; 
    }
    
    public String getOwnerName() {
        return ownerName;
    }
    
    public String getPermission() {
        return permission;
    }
    
    public void setId(int id) {
        this.id = id;
    }

    public void setOwnerUserId(int ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLogicalPath(String logicalPath) {
        this.logicalPath = logicalPath;
    }
    
    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

}
