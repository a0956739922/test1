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

    private final FileService fileService;
    private final SQLiteDB sqlite = new SQLiteDB();
    private volatile boolean running = true;

    public SyncService(FileService fileService) {
        this.fileService = fileService;
    }

    @Override
    public void run() {
        sqlite.resetSyncState("CREATING", "PENDING_CREATE");
        sqlite.resetSyncState("DELETING", "PENDING_DELETE");
        sqlite.resetSyncState("UPDATING", "PENDING_UPDATE");

        while (running) {
            try {
                if (!isOnline()) {
                    Thread.sleep(3000);
                    continue;
                }
                User current = sqlite.loadSession();
                if (current == null) {
                    Thread.sleep(3000);
                    continue;
                }
                int userId = current.getUserId();
                String username = current.getUsername();
                syncDeletes(userId, username);
                syncCreates(userId, username);
                syncUpdates(userId, username);
                Thread.sleep(3000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void syncDeletes(int userId, String username) {
        for (LocalFile lf : sqlite.getFilesByState(userId, "PENDING_DELETE")) {
            Integer remoteId = lf.getRemoteFileId();
            if (remoteId == null) continue;
            sqlite.updateSyncStateByFileId(remoteId, "DELETING");
            try {
                fileService.delete(userId, username, remoteId, lf.getFileName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void syncCreates(int userId, String username) {
        for (LocalFile lf : sqlite.getFilesByState(userId, "PENDING_CREATE")) {
            String reqId = lf.getReqId();
            if (reqId == null) continue;
            sqlite.updateSyncStateByReqId(reqId, "CREATING");
            try {
                fileService.create(reqId, userId, username, lf.getFileName(), lf.getContent());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void syncUpdates(int userId, String username) {
        for (LocalFile lf : sqlite.getFilesByState(userId, "PENDING_UPDATE")) {
            Integer remoteId = lf.getRemoteFileId();
            if (remoteId == null) continue;
            sqlite.updateSyncStateByFileId(remoteId, "UPDATING");
            try {
                fileService.update(userId, username, remoteId, lf.getFileName(), lf.getContent());
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

    public void shutdown() {
        running = false;
        interrupt();
    }
}