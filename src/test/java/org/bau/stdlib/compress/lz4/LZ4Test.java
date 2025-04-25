package org.bau.stdlib.compress.lz4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

import org.bau.stdlib.compress.lz4.LZ4.XXHash;
import org.junit.Test;

public class LZ4Test {

    @Test
    public void randomFileCompression() throws IOException {
        Random r = new Random(1);
        File dir = new File("target");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        assertTrue(dir.exists() && dir.isDirectory());
        for (int i = 0; i < 3; i++) {
            int len = r.nextInt(5 * 1024 * 1024);
            byte[] data = new byte[len];
            int distinct = (i + 1) * 10;
            for (int j = 0; j < data.length; j++) {
                data[j] = (byte) r.nextInt(distinct);
            }
            String fileName = new File(dir, "test.txt").getAbsolutePath();
            String compressedFileName = new File(dir, "test.lz4").getAbsolutePath();
            String verifyFileName = new File(dir, "test.verify.txt").getAbsolutePath();
            try (FileOutputStream out = new FileOutputStream(fileName)) {
                out.write(data);
            }
            LZ4.main("-1", fileName, compressedFileName);
            LZ4.main("-d", compressedFileName, verifyFileName);
            int hash = LZ4.xxhashFile(fileName);
            int verify = LZ4.xxhashFile(verifyFileName);
            assertEquals(hash, verify);
            assertEquals(new File(fileName).length(), new File(verifyFileName).length());
        }
    }

    @Test
    public void smallXXHash() {
        XXHash h = new XXHash(0);
        int result = h.update("Hello world".getBytes(StandardCharsets.UTF_8), 0, 11);
        assertEquals(0x9705d437, result);
    }

    @Test
    public void largeXXHash() {
        XXHash h = new XXHash(0);
        byte[] buff = new byte[1234];
        for (int i = 0; i < buff.length; i++) {
            buff[i] = (byte) i;
        }
        int result = h.update(buff, 0, buff.length);
        assertEquals(391477346, result);
    }

    @Test
    public void read4() {
        byte[] x = new byte[4];
        x[0] = 0x12;
        x[1] = 0x34;
        x[2] = 0x56;
        x[3] = 0x78;
        assertEquals(0x78563412, LZ4.read4(x, 0));
    }

    @Test
    public void randomBlockCompression() {
        Random r = new Random(1);
        for (int test = 0; test < 1_000; test++) {
            int len = r.nextInt(10_000);
            if (len == 0) {
                continue;
            }
            byte[] original = new byte[len];
            int distinctCount = 1 + r.nextInt(200);
            for (int i = 0; i < original.length; i++) {
                original[i] = (byte) r.nextInt(distinctCount);
            }
            LZ4 lz4 = new LZ4();
            byte[] compressed = new byte[original.length * 2];
            int compLen = lz4.compressBlock(original, original.length, compressed, 0);
            compressed = Arrays.copyOf(compressed, compLen + 10);
            byte[] verify = new byte[original.length + 10];
            int verifyLen = LZ4.decompressBlock(compressed, compLen, verify, 0);
            verify = Arrays.copyOf(verify, verifyLen);
            if (!Arrays.equals(verify, original)) {
                System.out.println("");
                Arrays.fill(compressed, (byte) 0);
                lz4.compressBlock(original, original.length, compressed, 0);
                Arrays.fill(verify, (byte) 0);
                LZ4.decompressBlock(compressed, compLen, verify, 0);
            }
            assertTrue(Arrays.equals(verify, original));
        }
    }
}
