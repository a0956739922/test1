/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.fileaggregator;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.zip.CRC32;
/**
 *
 * @author ntu-user
 */
public class FileCrypto {

    private String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz!@#$%^&*";
    private static final int FIXED_CHUNKS = 4;

    public String generateFileKey() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public File encryptZip(String inputFilePath, String outputZipPath, String encryptionKey) throws Exception {
        ZipParameters params = new ZipParameters();
        params.setEncryptFiles(true);
        params.setCompressionLevel(CompressionLevel.MAXIMUM);
        params.setEncryptionMethod(EncryptionMethod.AES);
        ZipFile zipFile = new ZipFile(outputZipPath, encryptionKey.toCharArray());
        zipFile.addFile(new File(inputFilePath), params);
        return new File(outputZipPath);
    }

    public void decryptZip(String zipPath, String extractToPath, String encryptionKey) throws Exception {
        ZipFile zipFile = new ZipFile(zipPath, encryptionKey.toCharArray());
        zipFile.extractAll(extractToPath);
        new ProcessBuilder("chown", "-R", "ntu-user:ntu-user", extractToPath).start().waitFor();
    }

    public String calcCRC32(byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data, 0, data.length);
        return Long.toHexString(crc.getValue());
    }

    public String calcFileCRC32(String path) throws Exception {
        byte[] data = Files.readAllBytes(new File(path).toPath());
        return calcCRC32(data);
    }

    public List<String> calcChunksCRC(List<File> chunks) throws Exception {
        List<String> list = new ArrayList<>();
        for (File f : chunks) {
            byte[] data = java.nio.file.Files.readAllBytes(f.toPath());
            list.add(calcCRC32(data));
        }
        return list;
    }

    public List<File> splitChunks(String zipPath) throws Exception {
        File source = new File(zipPath);
        long size = source.length();
        int chunkSize = (int) Math.ceil((double) size / FIXED_CHUNKS);
        if (chunkSize <= 0) chunkSize = 1;
        List<File> chunks = new ArrayList<>();
        byte[] buffer = new byte[chunkSize];
        int index = 0;
        try (FileInputStream fis = new FileInputStream(zipPath)) {
            int read;
            while ((read = fis.read(buffer)) != -1) {
                File chunk = new File(zipPath + "." + index + ".part");
                try (FileOutputStream fos = new FileOutputStream(chunk)) {
                    fos.write(buffer, 0, read);
                }
                chunks.add(chunk);
                index++;
            }
        }
        return chunks;
    }

    public void mergeChunks(String chunkDir, String outputZipPath) throws Exception {
        File dir = new File(chunkDir);
        File[] parts = dir.listFiles((d, name) -> name.endsWith(".part"));
        Arrays.sort(parts, Comparator.comparingInt(f -> {String[] t = f.getName().split("\\.");return Integer.valueOf(t[t.length - 2]);}));
        try (FileOutputStream fos = new FileOutputStream(outputZipPath)) {
            for (File part : parts) {
                Files.copy(part.toPath(), fos);
            }
        }
    }

}