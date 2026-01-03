/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

/**
 *
 * @author ntu-user
 */
public class RemoteFile {

    private int fileId;
    private int ownerUserId;
    private String name;
    private String logicalPath;
    private String ownerName;
    private String permission;
    private String sharedTo;

    public RemoteFile(int fileId, int ownerUserId, String name, String logicalPath) {
        this.fileId = fileId;
        this.ownerUserId = ownerUserId;
        this.name = name;
        this.logicalPath = logicalPath;
    }

    public int getFileId() {
        return fileId;
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

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public String getSharedTo() {
        return sharedTo;
    }

    public void setSharedTo(String sharedTo) {
        this.sharedTo = sharedTo;
    }
}
