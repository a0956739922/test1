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
    private Integer localId;
    private Integer remoteId;
    private String reqId;
    private int ownerUserId;
    private String name;
    private String logicalPath;
    private String ownerName;
    private String permission;
    private String content;
    private String sharedTo;

    public FileModel(Integer remoteId, int ownerUserId, String name, String logicalPath) {
        this.remoteId = remoteId;
        this.ownerUserId = ownerUserId;
        this.name = name;
        this.logicalPath = logicalPath;
    }
    
    public Integer getLocalId() {
        return localId;
    }

    public Integer getRemoteId() {
        return remoteId;
    }
    
    public String getReqId() {
        return reqId;
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
    
    public String getContent() {
        return content;
    }
    
    public String getSharedTo() {
        return sharedTo;
    }
    
    public void setLocalId(Integer localId) {
        this.localId = localId;
    }
    
    public void setRemoteId(Integer remoteId) {
        this.remoteId = remoteId;
    }
    
    public void setReqId(String reqId) {
        this.reqId = reqId;
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
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public void setSharedTo(String sharedTo) {
        this.sharedTo = sharedTo;
    }

}
