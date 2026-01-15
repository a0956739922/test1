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
    private final DbStatusClient statusClient;

    private boolean lastDbAvailable = false;

    public SyncService(DbStatusClient statusClient) {
        this.statusClient = statusClient;
    }

    @Override
    public void run() {
        while (true) {
            try {
                boolean dbAvailable = statusClient.isDbAvailable();
                if (!lastDbAvailable && dbAvailable) {
                    System.out.println("[SYNC] DB reconnected, trigger immediate sync");
                    syncDeletes();
                    syncCreates();
                    syncUpdates();
                }
                if (!dbAvailable) {
                    lastDbAvailable = false;
                    Thread.sleep(3000);
                    continue;
                }
                syncDeletes();
                syncCreates();
                syncUpdates();

                lastDbAvailable = true;
                Thread.sleep(3000);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void syncDeletes() {
        for (LocalFile lf : sqlite.getFilesByState("PENDING_DELETE")) {
            Integer remoteId = lf.getRemoteFileId();
            if (remoteId == null) continue;
            sqlite.updateSyncState("DELETING", remoteId, null);
            try {
                fileService.delete(lf.getUserId(), lf.getUsername(), remoteId, lf.getFileName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void syncCreates() {
        for (LocalFile lf : sqlite.getFilesByState("PENDING_CREATE")) {
            String reqId = lf.getReqId();
            if (reqId == null) continue;
            sqlite.updateSyncState("CREATING", null, reqId);
            try {
                fileService.create(reqId, lf.getUserId(), lf.getUsername(), lf.getFileName(), lf.getContent());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void syncUpdates() {
        for (LocalFile lf : sqlite.getFilesByState("PENDING_UPDATE")) {
            Integer remoteId = lf.getRemoteFileId();
            if (remoteId == null) continue;
            sqlite.updateSyncState("UPDATING", remoteId, null);
            try {
                fileService.update(lf.getUserId(), lf.getUsername(), remoteId, lf.getFileName(), lf.getContent());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}