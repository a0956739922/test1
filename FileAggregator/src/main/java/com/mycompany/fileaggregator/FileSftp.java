/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.fileaggregator;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import java.util.Properties;

/**
 *
 * @author ntu-user
 */
public class FileSftp {

    private final String USERNAME = "ntu-user";
    private final String PASSWORD = "ntu-user";
    private final int REMOTE_PORT = 22;
    private static final String BASE = "/home/ntu-user/data";
    private final int SESSION_TIMEOUT = 10000;
    private final int CHANNEL_TIMEOUT = 5000;

    private ChannelSftp connect(String host) throws Exception {
        JSch jsch = new JSch();
        jsch.setKnownHosts("/home/ntu-user/.ssh/known_hosts");
        Session session = jsch.getSession(USERNAME, host, REMOTE_PORT);
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.setPassword(PASSWORD);
        session.connect(SESSION_TIMEOUT);
        Channel sftp = session.openChannel("sftp");
        sftp.connect(CHANNEL_TIMEOUT);
        return (ChannelSftp) sftp;
    }
    
    private void close(ChannelSftp channel) {
        try {
            if (channel != null) channel.disconnect();
            Session session = channel.getSession();
            if (session != null && session.isConnected()) session.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void upload(String localFile, String remoteFile, String server) throws Exception {
        ChannelSftp channelSftp = connect(server);
        try {
            channelSftp.put(localFile, remoteFile);
        } finally {
            close(channelSftp);
        }
    }

    public void download(String remoteFile, String localFile, String server) throws Exception {
        ChannelSftp channelSftp = connect(server);
        try {
            channelSftp.get(remoteFile, localFile);
        } finally {
            close(channelSftp);
        }
    }

    public void delete(String remoteFile, String server) throws Exception {
        ChannelSftp channelSftp = connect(server);
        try {
            channelSftp.rm(remoteFile);
        } finally {
            close(channelSftp);
        }
    }
    
    public void mkdirIfNotExists(String path, String server) throws Exception {
        ChannelSftp channelSftp = connect(server);
        try {
            try {
                channelSftp.stat(BASE);
            } catch (Exception e) {
                channelSftp.mkdir(BASE);
            }
            try {
                channelSftp.stat(path);
            } catch (Exception e) {
                channelSftp.mkdir(path);
            }
        } finally {
            close(channelSftp);
        }
    }

}