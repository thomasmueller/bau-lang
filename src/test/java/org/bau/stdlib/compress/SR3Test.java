package org.bau.stdlib.compress;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.Deflater;

import org.junit.Test;

public class SR3Test {

    @Test
    public void simple() throws IOException {
        byte[] data = new byte[1024 * 1024];
        test(data);
        Arrays.fill(data, (byte) 32);
        test(data);
        Random r = new Random(1);
        r.nextBytes(data);
        test(data);
    }

    // @Test
    public void calgary() throws IOException {
        File dir = new File(System.getProperty("user.home") + "/Downloads/calgary");
        if (!dir.exists()) {
            return;
        }
        for(File f : dir.listFiles()) {
            byte[] data = new byte[(int) f.length()];
            RandomAccessFile rf = new RandomAccessFile(f, "r");
            rf.readFully(data);
            rf.close();
            System.out.println(f.getName());
            // test(data, 0);
            test(data, 1);
            test(data, 2);
            test(data, 3);
            test(data, 4);
            test(data, 5);
            test(data, 6);
            test(data, 7);
        }
    }

    private static void test(byte[] data) throws IOException {
        test(data, 5);
    }

    private static void test(byte[] data, int param) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SR3 sr = new SR3(new ByteArrayInputStream(data), out);
        sr.di1 = param;
//        sr.add = di2;
        long start = System.nanoTime();
        sr.compress();
        long timeSR3 = (System.nanoTime() - start) / data.length;
        assertTrue(timeSR3 >= 0);
        byte[] compressed = out.toByteArray();
        start = System.nanoTime();
//        byte[] compLZ4 = LZ4Test.compress(data);
//        long timeLZ4 = (System.nanoTime() - start) / data.length;
        start = System.nanoTime();
//        byte[] compXZ = XZTest.compress(data);
//        long timeXZ = (System.nanoTime() - start) / data.length;
        start = System.nanoTime();
        byte[] compDeflate = deflate(data, param);
        long timeDeflate = (System.nanoTime() - start) / data.length;
        assertTrue(compDeflate.length >= 0 && timeDeflate >= 0);
        /*
        System.out.println(data.length + " => " + compressed.length +
                " (" + timeSR3 + " ns/byte)" +
                "; di2=" + param +
//                "; lz4: " + compLZ4.length +
//                " (" + timeLZ4 + " ns/byte)" +
                "; deflate: " + compDeflate.length +
                " (" + timeDeflate + " ns/byte)"
//                "; xz: " + compXZ.length +
//                " (" + timeXZ + " ns/byte)"
                );
        */
        ByteArrayOutputStream test = new ByteArrayOutputStream();
        sr = new SR3(new ByteArrayInputStream(compressed), test);
        sr.di1 = param;
//        sr.add = di2;
        sr.decompress();
        byte[] verify = test.toByteArray();
        assertTrue(Arrays.equals(data, verify));
    }

    public static byte[] deflate(byte[] input, int level) {
        Deflater deflater = new Deflater(level == 7 ? Deflater.BEST_COMPRESSION : level);
        // Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION);
        deflater.setInput(input);
        deflater.finish();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(input.length);
        byte[] buffer = new byte[4096];
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            outputStream.write(buffer, 0, count);
        }
        deflater.end();
        return outputStream.toByteArray();
    }

}
