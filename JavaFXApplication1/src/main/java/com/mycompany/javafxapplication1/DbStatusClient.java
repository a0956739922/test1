/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
/**
 *
 * @author ntu-user
 */
public class DbStatusClient {

    private volatile boolean dbAvailable = false;

    public boolean isDbAvailable() {
        return dbAvailable;
    }

    public void start() {
        new Thread(() -> {
            while (true) {
                try (
                    Socket socket = new Socket("localhost", 6869);
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(socket.getInputStream()));
                    PrintWriter writer = new PrintWriter(
                            socket.getOutputStream(), true)
                ) {
                    while (true) {
                        writer.println("PING");
                        String reply = reader.readLine();
                        dbAvailable = "DB_UP".equals(reply);
                        Thread.sleep(2000);
                    }
                } catch (Exception e) {
                    dbAvailable = false;
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                }
            }
        }).start();
    }
}
