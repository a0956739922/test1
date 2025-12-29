/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

import java.util.List;

/**
 *
 * @author ntu-user
 */
public class SyncService {

    private final SQLiteDB sqlite;
    private final FileService fileService;

    public SyncService(SQLiteDB sqlite, FileService fileService) {
        this.sqlite = sqlite;
        this.fileService = fileService;
    }

    public void syncDeletes(int userId, String username) {
        List<Integer> pending = sqlite.getPendingDeleteId(userId);
        for (int fileId : pending) {
            try {
                fileService.delete(userId, username, fileId);
                sqlite.markSendingDelete(userId, fileId);
            } catch (Exception e) {
            }
        }
    }
}