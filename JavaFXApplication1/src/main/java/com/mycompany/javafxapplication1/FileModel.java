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
    private long id;
    private long ownerUserId;
    private String name;
    private String logicalPath;
    private long sizeBytes;
    private String ownerName;
    private String permission;

    public FileModel(long id, long ownerUserId, String name, String logicalPath, long sizeBytes) {
        this.id = id;
        this.ownerUserId = ownerUserId;
        this.name = name;
        this.logicalPath = logicalPath;
        this.sizeBytes = sizeBytes;
    }

    public long getId() {
        return id;
    }
    
    public long getOwnerUserId() {
        return ownerUserId;
    }
    
    public String getName() {
        return name;
    }
    
    public String getLogicalPath() {
        return logicalPath; 
    }
    
    public long getSizeBytes() {
        return sizeBytes; 
    }
    
    public String getOwnerName() {
        return ownerName;
    }
    
    public String getPermission() {
        return permission;
    }
    
    public void setId(long id) {
        this.id = id;
    }

    public void setOwnerUserId(long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLogicalPath(String logicalPath) {
        this.logicalPath = logicalPath;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }
    
    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

}
