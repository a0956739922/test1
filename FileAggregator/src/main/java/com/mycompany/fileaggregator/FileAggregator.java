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
public class FileAggregator {

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
        Path tmpDir = Path.of("/home/ntu-user/tmp/upload");
        Files.createDirectories(tmpDir);
        Path localFile = Files.createTempFile(tmpDir, "create-", ".tmp");
        Files.writeString(localFile, content);
        long sizeBytes = Files.size(localFile);
        String key = crypto.generateFileKey();
        Path zipFile = Path.of(localFile.toString() + ".zip");
        crypto.encryptZip(localFile.toString(), zipFile.toString(), key);
        List<File> chunks = crypto.splitChunks(zipFile.toString());
        JsonArrayBuilder chunkArr = Json.createArrayBuilder();
        long fileId = db.insertFile(ownerId,
                Json.createObjectBuilder()
                        .add("file_name", fileName)
                        .add("logical_path", logicalPath)
                        .add("size_bytes", sizeBytes)
                        .add("encryption_key", key)
                        .add("total_chunks", chunks.size())
                        .add("chunks", Json.createArrayBuilder().build())
                        .build()
        );
        for (int i = 0; i < chunks.size(); i++) {
            File chunk = chunks.get(i);
            String volume = VOLUMES[i];
            String container = containerForVolume(volume);
            String remoteDir = "/home/ntu-user/data/" + fileId;
            String remotePath = remoteDir + "/" + i + ".part";
            sftp.mkdirIfNotExists(remoteDir, container);
            sftp.upload(chunk.getAbsolutePath(), remotePath, container);
            chunkArr.add(Json.createObjectBuilder()
                    .add("index", i)
                    .add("volume", volume)
                    .add("remote_path", remotePath)
                    .add("crc32", crypto.calcFileCRC32(chunk.getAbsolutePath()))
                    .add("size_bytes", chunk.length()));
        }
        db.updateMetadata(fileId,
                Json.createObjectBuilder()
                        .add("file_id", fileId)
                        .add("file_name", fileName)
                        .add("logical_path", logicalPath)
                        .add("size_bytes", sizeBytes)
                        .add("encryption_key", key)
                        .add("total_chunks", chunks.size())
                        .add("chunks", chunkArr.build())
                        .build()
        );
        for (File f : chunks) f.delete();
        Files.deleteIfExists(zipFile);
        Files.deleteIfExists(localFile);
        return fileId;
    }

    public String download(long fileId) throws Exception {
        String baseDir = "/home/ntu-user/tmp/" + fileId;
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
        File[] files = dir.listFiles();
        File oriFileName = new File(baseDir, fileName);
        for (File file : files) {
            if (file.getName().endsWith(".tmp")) {
                file.renameTo(oriFileName);
            }
        }
        new File(zipOut).delete();
        Files.walk(Path.of(chunkDirPath))
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        return baseDir + "/" + fileName;
    }
    
    public String loadContent(long fileId) throws Exception {
        String filePath = download(fileId);
        File file = new File(filePath);
        if (!file.exists()) {
            throw new Exception("Downloaded file not found: " + filePath);
        }
        return Files.readString(file.toPath());
    }

    public void update(long fileId, String newName, String newLogicalPath, String content) throws Exception {
        JsonObject oldMeta = db.getMetadata(fileId);
        if (oldMeta == null) {
            throw new Exception("metadata missing");
        }
        String oldName = oldMeta.getString("file_name");
        String oldPath = oldMeta.getString("logical_path");
        boolean hasNewContent = content != null;
        boolean nameChanged = newName != null && !newName.equals(oldName);
        boolean pathChanged = newLogicalPath != null && !newLogicalPath.equals(oldPath);
        if (!hasNewContent) {

            if (!nameChanged && !pathChanged) {
                return;
            }
            JsonObject updated = Json.createObjectBuilder(oldMeta)
                    .add("file_name", nameChanged ? newName : oldName)
                    .add("logical_path", pathChanged ? newLogicalPath : oldPath)
                    .build();
            db.updateMetadata(fileId, updated);
            db.updateFile(
                    fileId,
                    nameChanged ? newName : oldName,
                    pathChanged ? newLogicalPath : oldPath,
                    null
            );
            return;
        }
        JsonArray oldChunks = oldMeta.getJsonArray("chunks");
        for (int i = 0; i < oldChunks.size(); i++) {
            JsonObject c = oldChunks.getJsonObject(i);
            String volume = c.getString("volume");
            String container = containerForVolume(volume);
            sftp.delete(c.getString("remote_path"), container);
        }
        Path tmp = Files.createTempFile("update-", ".tmp");
        Files.writeString(tmp, content);
        String key = crypto.generateFileKey();
        String zipPath = tmp.toString() + ".zip";
        crypto.encryptZip(tmp.toString(), zipPath, key);
        List<File> chunks = crypto.splitChunks(zipPath);
        JsonArrayBuilder chunkArr = Json.createArrayBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            File chunk = chunks.get(i);
            String volume = VOLUMES[i];
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
        long sizeBytes = content.getBytes().length;
        String finalName = nameChanged ? newName : oldName;
        String finalLogicalPath = pathChanged ? newLogicalPath : oldPath;
        JsonObject newMeta = Json.createObjectBuilder()
                .add("file_id", fileId)
                .add("file_name", finalName)
                .add("logical_path", finalLogicalPath)
                .add("size_bytes", sizeBytes)
                .add("encryption_key", key)
                .add("total_chunks", chunks.size())
                .add("chunks", chunkArr.build())
                .build();
        db.updateMetadata(fileId, newMeta);
        db.updateFile(fileId, finalName, finalLogicalPath, sizeBytes);
        for (File chunk : chunks) chunk.delete();
        Files.deleteIfExists(tmp);
        Files.deleteIfExists(Path.of(zipPath));
    }


    public void delete(long fileId) throws Exception {
        db.deleteShares(fileId);
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
        if (db.shareExists(fileId, targetId)) {
            db.updateShare(fileId, targetId, permission);
        } else {
            db.insertShare(fileId, ownerId, targetId, permission);
        }
    }

}