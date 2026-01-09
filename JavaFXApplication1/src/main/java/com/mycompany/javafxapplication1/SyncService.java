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
    private final SQLiteDB sqlite = new SQLiteDB();

    public SyncService(User user, FileService fileService) {
        this.user = user;
        this.fileService = fileService;
    }

    @Override
    public void run() {
        sqlite.resetSendingDelete();
        sqlite.resetSendingCreate();
        while (true) {
            if (isOnline()) {
                syncDeletes();
                syncCreates();
            }
            try {
                Thread.sleep(3000);
            } catch (Exception e) {
            }
        }
    }

    private void syncDeletes() {
        for (LocalFile lf : sqlite.getPendingDeleteFiles(user.getUserId())) {
        int fileId = lf.getRemoteFileId();
        String fileName = lf.getFileName();
        sqlite.markSendingDelete(user.getUserId(), fileId);
            try {
                fileService.delete(user.getUserId(), user.getUsername(), fileId, fileName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void syncCreates() {
        for (LocalFile lf : sqlite.getPendingCreate(user.getUserId())) {
            sqlite.markSendingCreate(lf.getReqId());
            try {
                fileService.create(lf.getReqId(), user.getUserId(), user.getUsername(), lf.getFileName(), lf.getContent());
            } catch (Exception e) {
                e.printStackTrace();
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