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
        while (true) {
            syncDeletes();
            try {
                Thread.sleep(3000);
            } catch (Exception e) {
            }
        }
    }

    private void syncDeletes() {
        if (!isOnline()) return;
        for (int userId : sqlite.getUsersWithPendingDelete()) {
            String username = sqlite.getUsernameByUserId(userId);
            for (int fileId : sqlite.getPendingDeleteId(userId)) {
                sqlite.markSendingDelete(userId, fileId);
                try {
                    fileService.delete(userId, username, fileId);
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
