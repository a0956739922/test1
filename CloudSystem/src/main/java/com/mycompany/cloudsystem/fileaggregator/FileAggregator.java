/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.cloudsystem.fileaggregator;

import java.io.File;
import java.nio.charset.StandardCharsets;
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

    public int create(String reqId, int ownerId, String fileName, String content) throws Exception {
        try {
            Path tmpDir = Path.of("/home/ntu-user/tmp/upload");
            Files.createDirectories(tmpDir);
            Path localFile = Files.createTempFile(tmpDir, "create-", ".tmp");
            Files.writeString(localFile, content);
            int sizeBytes = content.getBytes(StandardCharsets.UTF_8).length;
            String key = crypto.generateFileKey();
            Path zipFile = Path.of(localFile.toString() + ".zip");
            crypto.encryptZip(localFile.toString(), zipFile.toString(), key);
            List<File> chunks = crypto.splitChunks(zipFile.toString());
            JsonArrayBuilder chunkArr = Json.createArrayBuilder();
            int fileId = db.insertFile(
                    ownerId,
                    Json.createObjectBuilder()
                            .add("file_name", fileName)
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
                chunkArr.add(
                    Json.createObjectBuilder()
                            .add("index", i)
                            .add("volume", volume)
                            .add("remote_path", remotePath)
                            .add("crc32", crypto.calcFileCRC32(chunk.getAbsolutePath()))
                            .build()
                );
            }
            db.updateMetadata(
                fileId,
                Json.createObjectBuilder()
                        .add("file_id", fileId)
                        .add("file_name", fileName)
                        .add("size_bytes", sizeBytes)
                        .add("encryption_key", key)
                        .add("total_chunks", chunks.size())
                        .add("chunks", chunkArr.build())
                        .build()
            );
            for (File f : chunks) f.delete();
            Files.deleteIfExists(zipFile);
            Files.deleteIfExists(localFile);
            db.log(ownerId, null, "FILE_CREATE_OK", "req_id=" + reqId + ", fileId=" + fileId + ", fileName=" + fileName + ", sizeBytes=" + sizeBytes + ", chunks=" + chunks.size());
            return fileId;
        } catch (Exception e) {
            db.log(ownerId, null, "FILE_CREATE_FAIL", "req_id=" + reqId + ", fileName=" + fileName + ", error=" + e.getClass().getSimpleName());
            throw e;
        }
    }
    
    private String assembleFile(int fileId) throws Exception {
        String baseDir = "/home/ntu-user/tmp/" + fileId;
        JsonObject meta = db.getMetadata(fileId);
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
            String container = containerForVolume(c.getString("volume"));
            String remotePath = c.getString("remote_path");
            File localChunk = new File(chunkDirPath + "/" + i + ".part");
            sftp.download(remotePath, localChunk.getAbsolutePath(), container);
        }
        String zipOut = baseDir + "/data.zip";
        crypto.mergeChunks(chunkDirPath, zipOut);
        crypto.decryptZip(zipOut, baseDir, key);
        File dir = new File(baseDir);
        File[] files = dir.listFiles();
        if (files != null) {
            File oriFileName = new File(baseDir, fileName);
            for (File file : files) {
                if (file.getName().endsWith(".tmp")) {
                    file.renameTo(oriFileName);
                }
            }
        }
        new File(zipOut).delete();
        Files.walk(Path.of(chunkDirPath))
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        return baseDir + "/" + fileName;
    }

    public String download(String reqId, int ownerId, int fileId) throws Exception {
        try {
            String path = assembleFile(fileId);
            db.log(ownerId, null, "FILE_DOWNLOAD_OK", "req_id=" + reqId + ", fileId=" + fileId);
            return path;
        } catch (Exception e) {
            db.log(ownerId, null, "FILE_DOWNLOAD_FAIL", "req_id=" + reqId + ", fileId=" + fileId + ", error=" + e.getClass().getSimpleName());
            throw e;
        }
    }

    public String loadContent(int fileId) throws Exception {
        String filePath = assembleFile(fileId);
        return Files.readString(Path.of(filePath));
    }

    public void update(String reqId, int ownerId, int fileId, String newName, String content) throws Exception {
        String detail = "req_id=" + reqId + ", fileId=" + fileId;
        try {
            JsonObject oldMeta = db.getMetadata(fileId);
            String oldName = oldMeta.getString("file_name");
            boolean nameChanged = newName != null && !newName.equals(oldName);
            boolean contentChanged = content != null;
            String finalName = nameChanged ? newName : oldName;
            detail += ", nameChanged=" + nameChanged + ", contentChanged=" + contentChanged;
            if (contentChanged) {
                JsonArray oldChunks = oldMeta.getJsonArray("chunks");
                for (int i = 0; i < oldChunks.size(); i++) {
                    JsonObject c = oldChunks.getJsonObject(i);
                    sftp.delete(c.getString("remote_path"), containerForVolume(c.getString("volume")));
                }
                Path tmp = Files.createTempFile("update-", ".tmp");
                Files.writeString(tmp, content);
                int sizeBytes = content.getBytes(StandardCharsets.UTF_8).length;
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
                    sftp.upload(chunk.getAbsolutePath(), remotePath, container);
                    chunkArr.add(
                            Json.createObjectBuilder()
                                    .add("index", i)
                                    .add("volume", volume)
                                    .add("remote_path", remotePath)
                                    .add("crc32", crypto.calcFileCRC32(chunk.getAbsolutePath()))
                                    .build()
                    );
                }
                JsonObject newMeta = Json.createObjectBuilder()
                        .add("file_id", fileId)
                        .add("file_name", finalName)
                        .add("size_bytes", sizeBytes)
                        .add("encryption_key", key)
                        .add("total_chunks", chunks.size())
                        .add("chunks", chunkArr.build())
                        .build();
                db.updateMetadata(fileId, newMeta);
                db.updateFile(fileId, finalName, sizeBytes);
                detail += ", sizeBytes=" + sizeBytes + ", chunks=" + chunks.size();
                for (File chunk : chunks) chunk.delete();
                Files.deleteIfExists(tmp);
                Files.deleteIfExists(Path.of(zipPath));
            } else if (nameChanged) {
                JsonObject updated = Json.createObjectBuilder(oldMeta).add("file_name", finalName).build();
                db.updateMetadata(fileId, updated);
                db.updateFile(fileId, finalName, null);
            }
            db.log(ownerId, null, "FILE_UPDATE_OK", detail);
        } catch (Exception e) {
            db.log(ownerId, null, "FILE_UPDATE_FAIL", detail + ", error=" + e.getClass().getSimpleName());
            throw e;
        }
    }

    public void delete(String reqId, int ownerId, int fileId) throws Exception {
        try {
            JsonObject meta = db.getMetadata(fileId);
            db.deleteShares(fileId);
            if (meta != null) {
                JsonArray chunks = meta.getJsonArray("chunks");
                for (int i = 0; i < chunks.size(); i++) {
                    JsonObject c = chunks.getJsonObject(i);
                    sftp.delete(c.getString("remote_path"), containerForVolume(c.getString("volume")));
                }
            }
            db.markFileDeleted(fileId);
            db.log(ownerId, null, "FILE_DELETE_OK", "req_id=" + reqId + ", fileId=" + fileId);
        } catch (Exception e) {
            db.log(ownerId, null, "FILE_DELETE_FAIL", "req_id=" + reqId + ", fileId=" + fileId + ", error=" + e.getClass().getSimpleName());
            throw e;
        }
    }

    public void share(String reqId, int ownerId, int fileId, int targetId, String permission) throws Exception {
        try {
            if (db.shareExists(fileId, targetId)) {
                db.updateShare(fileId, targetId, permission);
            }
            else {
                db.insertShare(fileId, ownerId, targetId, permission);
            }
            db.log(ownerId, null, "FILE_SHARE_OK", "req_id=" + reqId + ", fileId=" + fileId  + ", targetId=" + targetId + ", permission=" + permission);
        } catch (Exception e) {
            db.log(ownerId, null, "FILE_SHARE_FAIL", "req_id=" + reqId + ", fileId=" + fileId + ", targetId=" + targetId + ", error=" + e.getClass().getSimpleName());
            throw e;
        }
    }

}