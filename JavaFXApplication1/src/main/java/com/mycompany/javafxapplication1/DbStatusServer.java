/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author ntu-user
 */
public class DbStatusServer extends Thread {

    private static final int PORT = 6869;

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            System.out.println("[DB-STATUS] Server listening on " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                new ClientHandler(socket).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler extends Thread {
        private final Socket socket;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(
                        socket.getOutputStream(), true)
            ) {
                String text;
                while ((text = reader.readLine()) != null) {

                    if ("PING".equals(text)) {
                        writer.println(checkDb() ? "DB_UP" : "DB_DOWN");
                    }

                    if ("bye".equals(text)) break;
                }

            } catch (IOException ignored) {}
        }

        private boolean checkDb() {
            try {
                new MySQLDB().testConnection();
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }
}
