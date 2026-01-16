/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
/**
 *
 * @author ntu-user
 */
public class DbStatusClient {

    private static final int DEFAULT_PORT = 3306;
    private static final int CONNECT_TIMEOUT_MS = 1000;
    private static final int CHECK_INTERVAL_MS = 2000;

    private final String host;
    private final int port;
    private volatile boolean dbAvailable = false;
    private Thread monitorThread;

    public DbStatusClient() {
        this("mysql", DEFAULT_PORT);
    }

    public DbStatusClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public boolean isDbAvailable() {
        return dbAvailable;
    }

    public void start() {
        if (monitorThread != null) {
            return;
        }
        monitorThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                dbAvailable = checkSocket();
                try {
                    Thread.sleep(CHECK_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "db-socket-monitor");
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    private boolean checkSocket() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
