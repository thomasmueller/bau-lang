package org.bau.stdlib.compress;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.bau.stdlib.compress.lz4.LZ4;
import org.junit.Test;

public class CompressionTest {

    enum CompressType {
        LZA,
        DEFLATE_NORMAL,
        DEFLATE_FAST,
        LZ4,
        DEFLATE_BEST,
        SR3,
    }

    public static void main(String... args) {
        String text = RandomTextGenerator.generate(1, 100_000_000);
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        for (CompressType t : CompressType.values()) {
            System.out.println(t);
            for (int test = 0; test < 2; test++) {
                long start = System.nanoTime();
                byte[] compressed = compress(data, t);
                long timeCompress = System.nanoTime() - start;
                System.out.println("  compress " + data.length + " => " + compressed.length + " in "
                        + timeCompress / data.length + " ns/byte");
                start = System.nanoTime();
                byte[] verify = decompress(compressed, t);
                long timeDecompress = System.nanoTime() - start;
                assertTrue(Arrays.equals(verify, data));
                System.out.println("  decompress in " + timeDecompress / data.length + " ns/byte");
            }
        }
    }

    private static byte[] compress(byte[] data, CompressType type) {
        switch (type) {
        case LZ4: {
            byte[] compressed = new byte[data.length * 2];
            LZ4 lz = new LZ4();
            int len = lz.compressBlock(data, data.length, compressed, 0);
            return Arrays.copyOf(compressed, len);
        }
        case SR3: {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            ByteArrayInputStream byteIn = new ByteArrayInputStream(data);
            SR3 sr = new SR3(byteIn, byteOut);
            try {
                sr.compress();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return byteOut.toByteArray();
        }
        case LZA:
            try {
                return LZArith.compress(data);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        case DEFLATE_FAST:
            return deflate(data, Deflater.BEST_SPEED);
        case DEFLATE_NORMAL:
            return deflate(data, Deflater.DEFAULT_COMPRESSION);
        case DEFLATE_BEST:
            return deflate(data, Deflater.BEST_COMPRESSION);
        }
        throw new IllegalArgumentException();
    }

    private static byte[] decompress(byte[] data, CompressType type) {
        switch (type) {
        case LZ4: {
            data = Arrays.copyOf(data, data.length + 4);
            byte[] test = new byte[data.length * 2];
            int len = LZ4.decompressBlock(data, data.length - 4, test, 0);
            return Arrays.copyOf(test, len);
        }
        case SR3: {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            ByteArrayInputStream byteIn = new ByteArrayInputStream(data);
            SR3 sr = new SR3(byteIn, byteOut);
            try {
                sr.decompress();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return byteOut.toByteArray();
        }
        case LZA:
            try {
                return LZArith.expand(data);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        case DEFLATE_FAST:
        case DEFLATE_NORMAL:
        case DEFLATE_BEST:
            return inflate(data);
        }
        throw new IllegalArgumentException();
    }

    public static byte[] deflate(byte[] input, int level) {
        Deflater deflater = new Deflater(level);
        deflater.setInput(input);
        deflater.finish();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(input.length);
        byte[] buffer = new byte[4096];
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            outputStream.write(buffer, 0, count);
        }
        deflater.end();
        deflater.close();
        return outputStream.toByteArray();
    }

    public static byte[] inflate(byte[] input) {
        Inflater inflater = new Inflater();
        try {
            inflater.setInput(input);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(input.length);
            byte[] buffer = new byte[4096];
            while (!inflater.finished()) {
                int count;
                try {
                    count = inflater.inflate(buffer);
                } catch (DataFormatException e) {
                    throw new RuntimeException(e);
                }
                outputStream.write(buffer, 0, count);
            }
            inflater.end();
            return outputStream.toByteArray();
        } finally {
            inflater.close();
        }
    }

    @Test
    public void testAll() throws IOException {
        String text = RandomTextGenerator.generate(1, 10_000);
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        for (CompressType t : CompressType.values()) {
            long start = System.nanoTime();
            byte[] compress = compress(data, t);
            long timeCompress = System.nanoTime() - start;
            assertTrue(timeCompress >= 0);
            // System.out.println("Compress " + data.length + " => " + compress.length + " in "
            //         + timeCompress / data.length + " ns/byte");
            start = System.nanoTime();
            byte[] verify = decompress(compress, t);
            long timeDecompress = System.nanoTime() - start;
            assertTrue(timeDecompress >= 0);
            assertTrue(Arrays.equals(verify, data));
            // System.out.println("Decompress in " + timeDecompress / data.length + " ns/byte");
        }

    }

//    @Test
//    public void lzArith() throws IOException {
//
//        String text = RandomTextGenerator.generate(1, 10_000_000);
//        byte[] data = text.getBytes(StandardCharsets.UTF_8);
//
//        for (int i = 0; i < 3; i++) {
//            long start = System.nanoTime();
//            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
//            ByteArrayInputStream byteIn = new ByteArrayInputStream(data);
//            LZArith lza = new LZArith(byteIn, byteOut);
//            lza.compress();
//            byte[] bytes = byteOut.toByteArray();
//
//            long timeCompress = System.nanoTime() - start;
//            System.out.println("Compress " + data.length + " => " + bytes.length + " in " + timeCompress / data.length + " ns/byte");
//
//            start = System.nanoTime();
//            ByteArrayOutputStream testOut = new ByteArrayOutputStream();
//            sr = new SR3(new ByteArrayInputStream(bytes), testOut);
//            sr.decompress();
//            byte[] test = testOut.toByteArray();
//            long timeDecompress = System.nanoTime() - start;
//
//            assertTrue(Arrays.equals(test, data));
//
//            System.out.println("Decompress in " + timeDecompress / data.length + " ns/byte");
//
//        }
//
//    }

}

