/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.fileaggregator;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
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
    private static final String[] VOLUMES = {
            "fs-vol-1", "fs-vol-2", "fs-vol-3", "fs-vol-4"
    };
    
    private static String containerForVolume(String volume) {
        return switch (volume) {
            case "fs-vol-1" -> "soft40051-files-container1";
            case "fs-vol-2" -> "soft40051-files-container2";
            case "fs-vol-3" -> "soft40051-files-container3";
            case "fs-vol-4" -> "soft40051-files-container4";
            default -> throw new IllegalArgumentException("Unknown volume: " + volume);
        };
    }
    
    public long create(long ownerId, String fileName, String logicalPath, String content) throws Exception {
        Path tmp = Files.createTempFile("create-", ".tmp");
        try {
            Files.writeString(tmp, content);
            return upload(ownerId, tmp.toString(), fileName, logicalPath);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    public long upload(long ownerId, String localFilePath, String fileName, String logicalPath) throws Exception {
        File original = new File(localFilePath);
        long sizeBytes = original.length();
        String key = crypto.generateFileKey();
        String zipPath = localFilePath + ".zip";
        crypto.encryptZip(localFilePath, zipPath, key);
        JsonObject initMeta = Json.createObjectBuilder()
                .add("file_id", -1)
                .add("file_name", fileName)
                .add("logical_path", logicalPath)
                .add("size_bytes", sizeBytes)
                .add("encryption_key", key)
                .add("total_chunks", 0)
                .add("chunks", Json.createArrayBuilder().build())
                .build();
        long fileId = db.insertFile(ownerId, initMeta);
        List<File> chunks = crypto.splitChunks(zipPath);
        JsonArrayBuilder chunkArr = Json.createArrayBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            File chunk = chunks.get(i);
            String volume = VOLUMES[i];
            String container = containerForVolume(volume);
            String remoteDir = "/home/ntu-user/data/" + fileId;
            String remotePath = remoteDir + "/" + i + ".part";
            sftp.mkdirIfNotExists(remoteDir, container);
            String crc32 = crypto.calcFileCRC32(chunk.getAbsolutePath());
            sftp.upload(chunk.getAbsolutePath(), remotePath, container);
            chunkArr.add(Json.createObjectBuilder()
                    .add("index", i)
                    .add("volume", volume)
                    .add("remote_path", remotePath)
                    .add("crc32", crc32)
                    .add("size_bytes", chunk.length()));
        }
        JsonObject finalMeta = Json.createObjectBuilder()
                .add("file_id", fileId)
                .add("file_name", fileName)
                .add("logical_path", logicalPath)
                .add("size_bytes", sizeBytes)
                .add("encryption_key", key)
                .add("total_chunks", chunks.size())
                .add("chunks", chunkArr.build())
                .build();
        db.updateMetadata(fileId, finalMeta);
        for (File chunk : chunks) chunk.delete();
        new File(zipPath).delete();
        return fileId;
    }

    public String download(long fileId) throws Exception {
        String baseDir = "/tmp/data/" + fileId;
        JsonObject meta = db.getMetadata(fileId);
        if (meta == null) {
            throw new Exception("File metadata not found: " + fileId);
        }
        String fileName = meta.getString("file_name");
        String key = meta.getString("encryption_key");
        String chunkDirPath = baseDir + "/chunks";
        File chunkDir = new File(chunkDirPath);
        if (!chunkDir.exists()) {
            chunkDir.mkdirs();
        }
        JsonArray chunksMeta = meta.getJsonArray("chunks");
        for (int i = 0; i < chunksMeta.size(); i++) {
            JsonObject c = chunksMeta.getJsonObject(i);
            String volume = c.getString("volume");
            String container = containerForVolume(volume);
            String remotePath = c.getString("remote_path");
            File localChunk = new File(chunkDirPath + "/" + i + ".part");
            sftp.download(remotePath, localChunk.getAbsolutePath(), container);
        }
        String zipOut = baseDir + "/data.zip";
        crypto.mergeChunks(chunkDirPath, zipOut);
        crypto.decryptZip(zipOut, baseDir, key);
        File dir = new File(baseDir);
        File[] files = dir.listFiles(File::isFile);
        File tmpFile = files[0];
        File finalFile = new File(baseDir + "/" + fileName);
        tmpFile.renameTo(finalFile);
        new File(zipOut).delete();
        Files.walk(Path.of(chunkDirPath))
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        return baseDir + "/" + fileName;
    }

    public void update(long fileId, String newLocalFilePath, String newLogicalPath) throws Exception {
        JsonObject oldMeta = db.getMetadata(fileId);
        if (oldMeta == null) throw new Exception("metadata missing");
        JsonArray oldChunks = oldMeta.getJsonArray("chunks");
        for (int i = 0; i < oldChunks.size(); i++) {
            JsonObject c = oldChunks.getJsonObject(i);
            String volume = c.getString("volume");
            String container = containerForVolume(volume);
            sftp.delete(c.getString("remote_path"), container);
        }
        File newFile = new File(newLocalFilePath);
        long sizeBytes = newFile.length();
        String newName = newFile.getName();
        String logicalPath = newLogicalPath != null ? newLogicalPath : oldMeta.getString("logical_path");
        String key = crypto.generateFileKey();
        String zipPath = newLocalFilePath + ".zip";
        crypto.encryptZip(newLocalFilePath, zipPath, key);
        List<File> chunks = crypto.splitChunks(zipPath);
        JsonArrayBuilder chunkArr = Json.createArrayBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            File chunk = chunks.get(i);
            String volume = VOLUMES[i % VOLUMES.length];
            String container = containerForVolume(volume);
            String remotePath = "/home/ntu-user/data/" + fileId + "/" + i + ".part";
            String crc32 = crypto.calcFileCRC32(chunk.getAbsolutePath());
            sftp.upload(chunk.getAbsolutePath(), remotePath, container);
            chunkArr.add(Json.createObjectBuilder()
                    .add("index", i)
                    .add("volume", volume)
                    .add("remote_path", remotePath)
                    .add("crc32", crc32)
                    .add("size_bytes", chunk.length()));
        }
        JsonObject newMeta = Json.createObjectBuilder()
                .add("file_id", fileId)
                .add("file_name", newName)
                .add("logical_path", logicalPath)
                .add("size_bytes", sizeBytes)
                .add("encryption_key", key)
                .add("total_chunks", chunks.size())
                .add("chunks", chunkArr.build())
                .build();
        db.updateMetadata(fileId, newMeta);
        db.updateFile(fileId, newName, logicalPath, sizeBytes);
        for (File chunk : chunks) chunk.delete();
        new File(zipPath).delete();
    }
    
    public void renameMove(long fileId, String newName, String newLogicalPath) throws Exception {
        JsonObject meta = db.getMetadata(fileId);
        if (meta == null) throw new Exception("metadata missing");
        String oldName = meta.getString("file_name");
        String oldPath = meta.getString("logical_path");
        boolean nameChanged = newName != null && !newName.equals(oldName);
        boolean pathChanged = newLogicalPath != null && !newLogicalPath.equals(oldPath);
        if (!nameChanged && !pathChanged) return;
        JsonObject updated = Json.createObjectBuilder(meta)
                .add("file_name", nameChanged ? newName : oldName)
                .add("logical_path", pathChanged ? newLogicalPath : oldPath)
                .build();
        db.updateMetadata(fileId, updated);
        db.updateFile(fileId, newName, newLogicalPath, null);
    }

    public void delete(long fileId) throws Exception {
        JsonObject meta = db.getMetadata(fileId);
        if (meta != null) {
            JsonArray chunks = meta.getJsonArray("chunks");
            for (int i = 0; i < chunks.size(); i++) {
                JsonObject c = chunks.getJsonObject(i);
                String volume = c.getString("volume");
                String container = containerForVolume(volume);
                sftp.delete(c.getString("remote_path"), container);
            }
        }
        db.markFileDeleted(fileId);
    }

    public void share(long fileId, long ownerId, long targetId, String permission) throws Exception {
        db.insertShare(fileId, ownerId, targetId, permission);
    }
}