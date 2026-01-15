package com.mycompany.cloudsystem.fileaggregator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FileCrypto.
 */
public class FileCryptoTest {

    @Test
    @DisplayName("generateFileKey returns 32-char non-empty key")
    public void testGenerateFileKey() {
        FileCrypto crypto = new FileCrypto();

        String key = crypto.generateFileKey();

        assertNotNull(key);
        assertEquals(32, key.length());
    }

    @Test
    @DisplayName("calcCRC32 is deterministic for the same input")
    public void testCalcCRC32() {
        FileCrypto crypto = new FileCrypto();
        byte[] data = "hello".getBytes();

        String first = crypto.calcCRC32(data);
        String second = crypto.calcCRC32(data);

        assertEquals(first, second);
    }

    @Test
    @DisplayName("splitChunks and mergeChunks round-trip file content")
    public void testSplitAndMerge(@TempDir Path tempDir) throws Exception {
        FileCrypto crypto = new FileCrypto();
        Path source = tempDir.resolve("source.bin");
        byte[] content = new byte[2048];
        for (int i = 0; i < content.length; i++) {
            content[i] = (byte) (i % 256);
        }
        Files.write(source, content);

        List<java.io.File> parts = crypto.splitChunks(source.toString());
        assertFalse(parts.isEmpty());

        Path merged = tempDir.resolve("merged.bin");
        crypto.mergeChunks(tempDir.toString(), merged.toString());

        byte[] mergedContent = Files.readAllBytes(merged);
        assertArrayEquals(content, mergedContent);
    }

    @Test
    @DisplayName("calcChunksCRC returns CRC list for each chunk")
    public void testCalcChunksCRC(@TempDir Path tempDir) throws Exception {
        FileCrypto crypto = new FileCrypto();
        Path source = tempDir.resolve("crc-source.txt");
        Files.writeString(source, "chunk-test-data");

        List<java.io.File> parts = crypto.splitChunks(source.toString());
        List<String> crcList = crypto.calcChunksCRC(parts);

        assertEquals(parts.size(), crcList.size());
        assertTrue(crcList.stream().allMatch(val -> val != null && !val.isEmpty()));
    }
}
