/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.fileaggregator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;

/**
 *
 * @author ntu-user
 */
public class FileManager {

    public static List<File> downloadChunks(JsonObject meta, FileSftp sftp, String outDir) throws Exception {
        List<File> list = new ArrayList<>();
        for (JsonValue v : meta.getJsonArray("chunks")) {
            JsonObject c = v.asJsonObject();
            String server = c.getString("server");
            String remote = c.getString("remote_path");
            File local = new File(outDir + "/" + c.getInt("index") + ".part");
            sftp.download(remote, local.getAbsolutePath(), server);
            list.add(local);
        }
        return list;
    }

    public static JsonObject rename(JsonObject meta, String newName) {
        return Json.createObjectBuilder(meta)
                .add("file_name", newName)
                .build();
    }

    public static JsonObject move(JsonObject meta, String newLogicalPath) {
        return Json.createObjectBuilder(meta)
                .add("logical_path", newLogicalPath)
                .build();
    }

    public static void deleteChunks(JsonObject meta, FileSftp sftp) throws Exception {
        for (JsonValue v : meta.getJsonArray("chunks")) {
            JsonObject c = v.asJsonObject();
            sftp.delete(c.getString("remote_path"), c.getString("server"));
        }
    }

    public static void validateNotDeleted(JsonObject meta) {
        if (meta.containsKey("is_deleted") && meta.getBoolean("is_deleted")) {
            throw new RuntimeException("File already deleted");
        }
    }
}