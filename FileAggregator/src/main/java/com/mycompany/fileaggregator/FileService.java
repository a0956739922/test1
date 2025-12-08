/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.fileaggregator;

import java.io.File;
import java.util.List;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

/**
 *
 * @author ntu-user
 */
public class FileService {

    private final FileDB db = new FileDB();
    private final FileCrypto crypto = new FileCrypto();
    private final FileSftp sftp = new FileSftp();
    private static final int CHUNK_SIZE = 512 * 1024;

    public long upload(long ownerId, String localFilePath, String logicalPath) throws Exception {
        File original = new File(localFilePath);
        long sizeBytes = original.length();
        String key = crypto.generateFileKey();
        String zipPath = localFilePath + ".zip";
        crypto.encryptZip(localFilePath, zipPath, key);
        JsonObject initMeta = Json.createObjectBuilder()
                .add("file_id", -1)
                .add("file_name", original.getName())
                .add("logical_path", logicalPath)
                .add("size_bytes", sizeBytes)
                .add("encryption_key", key)
                .add("total_chunks", 0)
                .add("chunks", Json.createArrayBuilder().build())
                .build();
        long fileId = db.insertFile(ownerId, initMeta);
        List<File> chunks = crypto.splitChunks(zipPath, CHUNK_SIZE);
        JsonArrayBuilder chunkArr = Json.createArrayBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            File chunk = chunks.get(i);
            String server = sftp.nextServer();
            String remoteDir = "/home/ntu-user/data/" + fileId;
            String remotePath = remoteDir + "/" + i + ".part";
            sftp.mkdirIfNotExists(remoteDir, server);
            String crc32 = crypto.calcFileCRC32(chunk.getAbsolutePath());
            sftp.upload(chunk.getAbsolutePath(), remotePath, server);
            chunkArr.add(Json.createObjectBuilder()
                    .add("index", i)
                    .add("server", server)
                    .add("remote_path", remotePath)
                    .add("crc32", crc32)
                    .add("size_bytes", chunk.length()));
        }
        JsonObject finalMeta = Json.createObjectBuilder()
                .add("file_id", fileId)
                .add("file_name", original.getName())
                .add("logical_path", logicalPath)
                .add("size_bytes", sizeBytes)
                .add("encryption_key", key)
                .add("total_chunks", chunks.size())
                .add("chunks", chunkArr.build())
                .build();
        db.updateMetadata(fileId, finalMeta);
        return fileId;
    }

    public long create(long ownerId, String localFilePath, String logicalPath) throws Exception {
        return upload(ownerId, localFilePath, logicalPath);
    }

    public String download(long fileId, String outputDir) throws Exception {
        JsonObject meta = db.getMetadata(fileId);
        if (meta == null) {
            throw new Exception("File metadata not found for id=" + fileId);
        }
        String key = meta.getString("encryption_key");
        String fileName = meta.getString("file_name");
        String chunkDirPath = outputDir + File.separator + "chunks_" + fileId;
        File chunkDir = new File(chunkDirPath);
        if (!chunkDir.exists()) {
            chunkDir.mkdirs();
        }
        JsonArray chunksMeta = meta.getJsonArray("chunks");
        for (int i = 0; i < chunksMeta.size(); i++) {
            JsonObject c = chunksMeta.getJsonObject(i);
            String server = c.getString("server");
            String remotePath = c.getString("remote_path");
            int index = c.getInt("index");
            File localChunk = new File(chunkDirPath + File.separator + index + ".part");
            sftp.download(remotePath, localChunk.getAbsolutePath(), server);
        }
        String zipOut = outputDir + File.separator + fileName + ".zip";
        crypto.mergeChunks(chunkDirPath, zipOut);
        crypto.decryptZip(zipOut, outputDir, key);

        return outputDir + File.separator + fileName;
    }

    public void updateFile(long fileId, String newLocalFilePath) throws Exception {
        JsonObject oldMeta = db.getMetadata(fileId);
        if (oldMeta == null) {
            throw new Exception("File metadata not found for id=" + fileId);
        }
        JsonArray oldChunks = oldMeta.getJsonArray("chunks");
        for (int i = 0; i < oldChunks.size(); i++) {
            JsonObject c = oldChunks.getJsonObject(i);
            String server = c.getString("server");
            String remotePath = c.getString("remote_path");
            sftp.delete(remotePath, server);
        }
        File newFile = new File(newLocalFilePath);
        long sizeBytes = newFile.length();
        String logicalPath = oldMeta.getString("logical_path");
        String fileName = oldMeta.getString("file_name");
        String key = crypto.generateFileKey();
        String zipPath = newLocalFilePath + ".zip";
        crypto.encryptZip(newLocalFilePath, zipPath, key);
        List<File> chunks = crypto.splitChunks(zipPath, CHUNK_SIZE);
        JsonArrayBuilder chunkArr = Json.createArrayBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            File chunk = chunks.get(i);
            String server = sftp.nextServer();
            String remotePath = "/data/" + fileId + "/" + i + ".part";
            String crc32 = crypto.calcFileCRC32(chunk.getAbsolutePath());
            sftp.upload(chunk.getAbsolutePath(), remotePath, server);
            chunkArr.add(
                    Json.createObjectBuilder()
                            .add("index", i)
                            .add("server", server)
                            .add("remote_path", remotePath)
                            .add("crc32", crc32)
                            .add("size_bytes", chunk.length())
            );
        }
        JsonObject newMeta = Json.createObjectBuilder()
                .add("file_id", fileId)
                .add("file_name", fileName)
                .add("logical_path", logicalPath)
                .add("size_bytes", sizeBytes)
                .add("encryption_key", key)
                .add("total_chunks", chunks.size())
                .add("chunks", chunkArr.build())
                .build();

        db.updateMetadata(fileId, newMeta);
    }

    public void delete(long fileId) throws Exception {
        JsonObject meta = db.getMetadata(fileId);
        if (meta != null) {
            JsonArray chunks = meta.getJsonArray("chunks");
            for (int i = 0; i < chunks.size(); i++) {
                JsonObject c = chunks.getJsonObject(i);
                String server = c.getString("server");
                String remotePath = c.getString("remote_path");
                sftp.delete(remotePath, server);
            }
        }
        db.markFileDeleted(fileId);
    }

    public void share(long fileId, long ownerId, long targetId, String permission) throws Exception {
        db.insertShare(fileId, ownerId, targetId, permission);
    }
}