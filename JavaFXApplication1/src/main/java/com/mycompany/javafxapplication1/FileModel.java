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
    private String name;
    private String logicalPath;
    private long sizeBytes;

    public FileModel(int id, String name, String logicalPath, long sizeBytes) {
        this.id = id;
        this.name = name;
        this.logicalPath = logicalPath;
        this.sizeBytes = sizeBytes;
    }

    public int getId() { 
        return id; 
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
}
