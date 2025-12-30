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

    private final User user;
    private final FileService fileService;

    public SyncService(User user, FileService fileService) {
        this.user = user;
        this.fileService = fileService;
    }

    @Override
    public void run() {
        while (true) {
            boolean online = false;
            try {
                new MySQLDB().testConnection();
                online = true;
            } catch (Exception e) {}
            if (online) {
                syncDeletes();
            }
            try {
                Thread.sleep(5000);
            } catch (Exception e) {}
        }
    }

    private void syncDeletes() {
        SQLiteDB sqlite = new SQLiteDB();
        for (int fileId : sqlite.getPendingId(user.getUserId())) {
            try {
                fileService.delete(user.getUserId(), user.getUsername(), fileId);
                sqlite.clearPending(user.getUserId(), fileId);
            } catch (Exception e) {
            }
        }
    }
}
