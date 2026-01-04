/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

/**
 *
 * @author ntu-user
 */
public class SyncService extends Thread {

    private final FileService fileService = new FileService();
    private final SQLiteDB sqlite = new SQLiteDB();

    @Override
    public void run() {
        sqlite.resetSendingDelete();
        sqlite.resetSendingCreate();
        while (true) {
            syncDeletes();
            syncCreates();
            try {
                Thread.sleep(3000);
            } catch (Exception e) {
            }
        }
    }

    private void syncDeletes() {
        if (!isOnline()) return;
        for (int userId : sqlite.getPendingDeleteUser()) {
            String username = sqlite.getUsername(userId);
            for (int fileId : sqlite.getPendingDelete(userId)) {
                sqlite.markSendingDelete(userId, fileId);
                try {
                    fileService.delete(userId, username, fileId);
                } catch (Exception e) {
                }
            }
        }
    }
    
    private void syncCreates() {
        if (!isOnline()) return;
        for (int userId : sqlite.getPendingCreateUser()) {
            for (LocalFile lf : sqlite.getPendingCreate(userId)) {
                sqlite.markSendingCreate(lf.getReqId());
                try {
                    fileService.create(lf.getReqId(), userId, lf.getUsername(), lf.getFileName(), lf.getContent());
                } catch (Exception e) {
                }
            }
        }
    }
    
    private boolean isOnline() {
        try {
            new MySQLDB().testConnection();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
