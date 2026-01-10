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
        sqlite.resetSyncState("CREATING", "PENDING_CREATE");
        sqlite.resetSyncState("DELETING", "PENDING_DELETE");
        sqlite.resetSyncState("UPDATING", "PENDING_UPDATE");
        while (true) {
            if (isOnline()) {
                syncDeletes();
                syncCreates();
                syncUpdates();
            }
            try {
                Thread.sleep(3000);
            } catch (Exception e) {
            }
        }
    }

    private void syncDeletes() {
        for (LocalFile lf : sqlite.getFilesByState(user.getUserId(), "PENDING_DELETE")) {
            int fileId = lf.getRemoteFileId();
            sqlite.updateSyncStateByFileId(fileId, "DELETING");
            try {
                fileService.delete(user.getUserId(), user.getUsername(), fileId, lf.getFileName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void syncCreates() {
        for (LocalFile lf : sqlite.getFilesByState(user.getUserId(), "PENDING_CREATE")) {
            sqlite.updateSyncStateByReqId(lf.getReqId(), "CREATING");
            try {
                fileService.create(lf.getReqId(), user.getUserId(), user.getUsername(), lf.getFileName(), lf.getContent());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private void syncUpdates() {
        for (LocalFile lf : sqlite.getFilesByState(user.getUserId(), "PENDING_UPDATE")) {
            int fileId = lf.getRemoteFileId();
            sqlite.updateSyncStateByFileId(fileId, "UPDATING");
            try {
                fileService.update(user.getUserId(), user.getUsername(), fileId, lf.getFileName(), lf.getContent());
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