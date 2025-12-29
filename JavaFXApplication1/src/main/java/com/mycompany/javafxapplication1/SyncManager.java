/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

/**
 *
 * @author ntu-user
 */
public class SyncManager {

    public void start(User user, FileService fileService) {
        new Thread(() -> {
            while (true) {
                try {
                    new MySQLDB().testConnection();
                    new SyncService(new SQLiteDB(), fileService).syncDeletes(user.getUserId(), user.getUsername());
                } catch (Exception ignore) {
                }
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ignore) {}
            }
        }).start();
    }
}
