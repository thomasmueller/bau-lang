package org.bau.stdlib.compress.lz4;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class LZ4 {

    final static int BLOCK_SIZE = 4 * 1024 * 1024;
    private final int[] hashTable = new int[4 * 1024];

    public static void main(String... args) throws IOException {
        if (args.length == 3 && args[0].equals("-d")) {
            String inputFileName = args[1];
            String outputFileName = args[2];
            try {
                long result = decompressFile(inputFileName, outputFileName);
                System.out.println("Decompressed " + result + " bytes");
            } catch (Exception e) {
                System.out.println("Failed to decompress " + inputFileName + " to " + outputFileName + ": " + e);
                e.printStackTrace();
            }
        } else if (args.length == 2 && args[0].equals( "-d") && args[1].equals("-")) {
            try {
                decompressStream(System.in, System.out);
            } catch (Exception e) {
                System.out.println("Failed to decompress stdin: "+ e);
                e.printStackTrace();
            }
        } else if (args.length >= 2 && (args[0].equals("-1") || args[0].equals("-2") || args[0].equals("-3")
            || args[0].equals("-4") || args[0].equals("-5") || args[0].equals("-6")
            || args[0].equals("-7") || args[0].equals("-8") || args[0].equals("-9")))  {
            int level = args[0].charAt(1) - '0';
            if (args.length == 3) {
                String inputFileName = args[1];
                String outputFileName = args[2];
                try {
                    long result = compressFile(inputFileName, outputFileName, level);
                    System.out.println("Compressed " + result + " bytes");
                } catch (Exception e) {
                    System.out.println("Failed to compress " + inputFileName + " to " + outputFileName + ": " + e);
                    e.printStackTrace();
                }
            } else if (args.length == 2 && args[1].equals("-")) {
                try {
                    compressStream(System.in, System.out, level);
                } catch (Exception e) {
                    System.out.println("Failed to compress stdin: "+ e);
                    e.printStackTrace();
                }
            }
        } else if (args.length == 2 && args[0].equals("-h")) {
            if (args[1].equals("-")) {
                try {
                    int result = xxhashStream(System.in);
                    System.out.printf("%08x\n", result);
                } catch (Exception e) {
                    System.out.println("Failed to read: "+ e);
                    e.printStackTrace();
                }
            } else {
                String inputFileName = args[1];
                try {
                    int result = xxhashFile(inputFileName);
                    System.out.printf("%08x\n", result);
                } catch (Exception e) {
                    System.out.println("Failed to read " + inputFileName + ": " + e);
                    e.printStackTrace();
                }
            }
        }
    }

    private static void readExact(InputStream in, byte[] buffer, int pos, int len) throws IOException {
        if (readFully(in, buffer, pos, len) != len) {
            throw new IOException("EOF");
        }
    }

    private static int readFully(InputStream in, byte[] buffer, int pos, int len) throws IOException {
        int blockRead = 0;
        while (blockRead < len) {
            int bytesRead = in.read(buffer, blockRead, len - blockRead);
            if (bytesRead == -1) {
                return blockRead;
            }
            blockRead += bytesRead;
        }
        return blockRead;
    }

    static int xxhashFile(String inputFileName) throws IOException {
        try (FileInputStream in = new FileInputStream(inputFileName)) {
            return xxhashStream(in);
        }
    }

    private static int xxhashStream(InputStream in) throws IOException {
        XXHash hash = new XXHash(0);
        byte[] buffer = new byte[BLOCK_SIZE];
        int result = 0;
        while (true) {
            int read = readFully(in, buffer, 0, buffer.length);
            if (read <= 0 ) {
                return result;
            }
            result = hash.update(buffer, 0, read);
        }
    }

    private static long compressFile(String inputFileName, String outputFileName, int level) throws IOException {
        try (FileInputStream in = new FileInputStream(inputFileName)) {
            new File(outputFileName).delete();
            try (FileOutputStream out = new FileOutputStream(outputFileName)) {
                return compressStream(in, out, level);
            }
        }
    }

    private static long compressStream(InputStream in, OutputStream out, int level) throws IOException {
        byte[] header = new byte[7];
        int magic = 0x184D2204;
        LZ4.write4(header, 0, magic);
        header[4] = (1 << 6) | (1 << 5);
        header[5] = (byte) (7 << 4);
        XXHash hash = new XXHash(0);
        header[6] = (byte) ((hash.update(header, 4, 2) >>> 8) & 0xff);
        out.write(header);
        int blockSize = 4 * 1024 * 1024;
        byte[] block = new byte[blockSize];
        byte[] outBlock = new byte[5 * 1024 * 1024];
        LZ4 comp = new LZ4();
        long totalSize = 0;
        while(true) {
            int read = readFully(in, block, 0, block.length);
            if (read == 0) {
                break;
            }
            totalSize += read;
            int end = comp.compressBlock(block, read, outBlock, 4);
            if (end >= read) {
                // can not compress
                int writeBlockSize = 1 << 31;
                writeBlockSize |= read;
                LZ4.write4(outBlock, 0, writeBlockSize);
                System.arraycopy(block, 0, outBlock, 4, read);
                out.write(outBlock, 0, read + 4);
            } else {
                LZ4.write4(outBlock, 0, end - 4);
                out.write(outBlock, 0, end);
            }
        }
        LZ4.write4(outBlock, 0, 0);
        out.write(outBlock, 0, 4);
        out.flush();
        return totalSize;
    }

    private static long decompressFile(String inputFileName, String outputFileName) throws IOException {
        try (FileInputStream in = new FileInputStream(inputFileName)) {
            new File(outputFileName).delete();
            try (FileOutputStream out = new FileOutputStream(outputFileName)) {
                return decompressStream(in, out);
            }
        }
    }

    private static long decompressStream(InputStream in, OutputStream out) throws IOException {
        byte[] header = new byte[7];
        readExact(in, header, 0, 7);
        int magic = LZ4.read4(header, 0);
        if (magic != 0x184d2204) {
            throw new IOException("Incorrect magic");
        }
        int flags = header[4];
        int version = flags >>> 6;
        if (version != 1) {
            throw new IOException("Incorrect version");
        }
        boolean blockIndependanceFlag = ((flags >>> 5) & 1) == 1;
        if (!blockIndependanceFlag) {
            throw new IOException("Unsupported block dependence");
        }
        boolean blockChecksumFlag = ((flags >>> 4) & 1) == 1;
        if (blockChecksumFlag) {
            throw new IOException("Unsupported block checksum flag");
        }
        boolean contentSizeFlag = ((flags >>> 3) & 1) == 1;
        if (contentSizeFlag) {
            throw new IOException("Unsupported content size flag");
        }
        boolean contentChecksumFlag = ((flags >>> 2) & 1) == 1;
        if (contentChecksumFlag) {
            // TODO currently, the content checksum flag is ignored
        }
        if (((flags >>> 1) & 1) != 0) {
            throw new IOException("Unsupported reserved");
        }
        if ((flags & 1) == 1) {
            throw new IOException("Unsupported dict flag");
        }
        int bd = header[5];
        int blockMaxSize = (bd >>> 4) & 0x7;
        if (blockMaxSize < 4 || blockMaxSize > 7) {
            throw new IOException("Unsupported block max size");
        }
        int headerChecksum = header[6] & 0xff;
        XXHash hash = new XXHash(0);
        int xxhash = (hash.update(header, 4, 2) >>> 8) & 0xff;
        if ((xxhash & 0xff) != headerChecksum) {
            throw new IOException("Header checksum mismatch");
        }
        byte[] block = new byte[4 * 1024 * 1024];
        byte[] outBlock = new byte[4 * 1024 * 1024 + 128];
        long outputFileSize = 0;
        while(true) {
            readExact(in, header, 0, 4);
            int blockSize = LZ4.read4(header, 0);
            if (blockSize == 0) {
                break;
            }
            boolean uncompressed = ((blockSize >>> 31) & 1) == 1;
            blockSize &= 0x7fffffff;
            if (blockSize > 4 * 1024 * 1024) {
                throw new IOException("Unsupported block size");
            }
            readExact(in, block, 0, blockSize);
            if (uncompressed) {
                out.write(block, 0, blockSize);
                outputFileSize += blockSize;
            } else {
                int size = LZ4.decompressBlock(block, blockSize, outBlock, 0);
                out.write(outBlock, 0, size);
                outputFileSize += size;
            }
        }
        out.flush();
        return outputFileSize;
    }

    private static long read8(byte[] data, int pos) {
        return (data[pos + 0] & 0xffL)
                | ((data[pos + 1] & 0xffL) << 8)
                | ((data[pos + 2] & 0xffL) << 16)
                | ((data[pos + 3] & 0xffL) << 24)
                | ((data[pos + 4] & 0xffL) << 32)
                | ((data[pos + 5] & 0xffL) << 40)
                | ((data[pos + 6] & 0xffL) << 48)
                | ((data[pos + 7] & 0xffL) << 56);
    }

    private static int hash5(byte[] data, int pos) {
        long x = read8(data, pos);
        long prime5bytes = 889523592379L;
        return (int) (((x << 24) * prime5bytes) >>> (64 - 12));
    }

    private static int runLenCount(byte[] a, int aLen, int ai, int bi) {
        int runLen = 0;
        while (ai + runLen < aLen - 24) {
            long ax = read8(a, ai + runLen);
            long bx = read8(a, bi + runLen);
            long diff = ax ^ bx;
            if (diff == 0) {
                runLen += 8;
            } else {
                runLen += Long.numberOfTrailingZeros(diff) >> 3;
                return runLen;
            }
        }
        while (ai + runLen < aLen - 16 && a[ai + runLen] == a[bi + runLen]) {
            runLen += 1;
        }
        return runLen;
    }

    int compressBlock(byte[] inData, int inLen, byte[] outData, int outPos) {
        if (inLen > inData.length) {
            throw new IllegalArgumentException("Buffer too small");
        }
        int literalLen = 0;
        int inPos = 0;
        int skipTrigger = 6;
        int searchMatch = 1 << skipTrigger;
        while (true) {
            int runLen;
            int candidatePos;
            if (inPos + 16 < inLen) {
                int h = hash5(inData, inPos);
                candidatePos = hashTable[h];
                hashTable[h] = inPos;
                if (candidatePos >= inPos || candidatePos + 0xffff < inPos) {
                    int step = searchMatch >> skipTrigger;
                    literalLen += step;
                    inPos += step;
                    searchMatch += 1;
                    continue;
                } else {
                    runLen = runLenCount(inData, inLen, inPos, candidatePos);
                    if (runLen < 4) {
                        int step = searchMatch >> skipTrigger;
                        literalLen += step;
                        inPos += step;
                        searchMatch += 1;
                        continue;
                    }
                    int p = inPos + runLen - 2;
                    h = hash5(inData, p);
                    hashTable[h] = p;
                }
            } else {
                // the last few bytes in the block
                // are always encoded as literals
                literalLen += inLen - inPos;
                inPos = inLen;
                runLen = 4;
                candidatePos = 0;
            }
            int tagPos = outPos;
            outPos += 1;
            // possibly increase the match len backwards
            while (candidatePos > 0 && literalLen > 0 && inPos > 0 && inData[inPos - 1] == inData[candidatePos - 1]) {
                runLen += 1;
                inPos -= 1;
                literalLen -= 1;
                candidatePos -= 1;
            }
            int copyLen = literalLen;
            if (literalLen >= 0xf) {
                while (literalLen - 0xf >= 0xff) {
                    outData[outPos] = (byte) 0xff;
                    outPos += 1;
                    literalLen -= 0xff;
                }
                outData[outPos] = (byte) (literalLen - 0xf);
                outPos += 1;
                literalLen = 0xf;
            }
            for (int i = 0; i < copyLen; i++) {
                int x = inData[inPos - copyLen + i];
                outData[outPos + i] = (byte) x;
            }
            outPos += copyLen;
            if (inPos < inLen) {
                int offset = inPos - candidatePos;
                outData[outPos] = (byte) offset;
                outData[outPos + 1] = (byte) (offset >> 8);
                outPos += 2;
            } else {
                int tag = literalLen << 4;
                outData[tagPos] = (byte) tag;
                break;
            }
            int skipLen = runLen;
            runLen -= 4;
            if (runLen >= 0xf) {
                while (runLen - 0xf >= 0xff) {
                    outData[outPos] = (byte) 0xff;
                    outPos += 1;
                    runLen -= 0xff;
                }
                outData[outPos] = (byte) (runLen - 0xf);
                outPos += 1;
                runLen = 0xf;
            }
            int tag = (literalLen << 4) | runLen;
            outData[tagPos] = (byte) tag;
            inPos += skipLen;
            literalLen = 0;
            searchMatch = 1 << skipTrigger;
        }
        return outPos;
    }

    static int read4(byte[] data, int pos) {
        return (data[pos + 0] & 0xff)
                | ((data[pos + 1] & 0xff) << 8)
                | ((data[pos + 2] & 0xff) << 16)
                | ((data[pos + 3] & 0xff) << 24);
    }

    static void write4(byte[] data, int pos, int x) {
        data[pos] = (byte) x;
        data[pos + 1] = (byte) (x >> 8);
        data[pos + 2] = (byte) (x >> 16);
        data[pos + 3] = (byte) (x >> 24);
    }

    static int decompressBlock(byte[] inData, int inLen, byte[] outData, int outPos) {
        if (inLen > inData.length) {
            throw new IllegalArgumentException("Buffer too small");
        }
        int p = 0;
        while (true) {
            int tag = inData[p] & 0xff;
            p += 1;
            int literalLen = tag >>> 4;
            if (literalLen == 0xf) {
                while (true) {
                    int x = inData[p] & 0xff;
                    p += 1;
                    literalLen += x;
                    if (x != 0xff) {
                        break;
                    }
                }
            }
            for (int i = 0; i < literalLen; i += 4) {
                int x = read4(inData, p + i);
                write4(outData, outPos + i, x);
                outData[outPos + i] = inData[p + i];
            }
            outPos += literalLen;
            p += literalLen;
            if (p + 1 >= inLen) {
                break;
            }
            int offset = ((inData[p] & 0xff) | ((inData[p + 1] & 0xff) << 8));
            if (offset == 0 || offset > outPos) {
                throw new IllegalStateException("Illegal offset");
            }
            p += 2;
            if (p >= inLen - 1) {
                break;
            }
            int runLen = tag & 0xf;
            if (runLen == 0xf) {
                while (true) {
                    int x = inData[p] & 0xff;
                    p += 1;
                    runLen += x;
                    if (x != 0xff) {
                        break;
                    }
                }
            }
            runLen += 4;
            if (offset >= 4) {
                for (int i = 0; i < runLen; i += 4) {
                    int x = read4(outData, outPos + i - offset);
                    write4(outData, outPos + i, x);
                }
            } else {
                for (int i = 0; i < runLen; i++) {
                    outData[outPos + i] = outData[outPos + i - offset];
                }
            }
            outPos += runLen;
        }
        return outPos;
    }

    public static class XXHash {

        private static final int PRIME1 = (int) 2654435761L;
        private static final int PRIME2 = (int) 2246822519L;
        private static final int PRIME3 = (int) 3266489917L;
        private static final int PRIME4 = 668265263;
        private static final int PRIME5 = 374761393;

        private int v1, v2, v3, v4;
        private int total;

        public XXHash(int seed) {
            v1 = seed + PRIME1 + PRIME2;
            v2 = seed + PRIME2;
            v3 = seed;
            v4 = seed - PRIME1;
        }

        public int update(byte[] buf, int start, int len) {
            int end = start + len;
            int pos = start;
            if (len >= 16) {
                int limit = end - 16;
                do {
                    v1 = Integer.rotateLeft(LZ4.read4(buf, pos + 0) * PRIME2 + v1, 13) * PRIME1;
                    v2 = Integer.rotateLeft(LZ4.read4(buf, pos + 4) * PRIME2 + v2, 13) * PRIME1;
                    v3 = Integer.rotateLeft(LZ4.read4(buf, pos + 8) * PRIME2 + v3, 13) * PRIME1;
                    v4 = Integer.rotateLeft(LZ4.read4(buf, pos + 12) * PRIME2 + v4, 13) * PRIME1;
                    pos += 16;
                } while (pos <= limit);
            }
            total += len;
            int h32 = total;
            if (total >= 16) {
                h32 += Integer.rotateLeft(v1, 1) + Integer.rotateLeft(v2, 7) +
                        Integer.rotateLeft(v3, 12) + Integer.rotateLeft(v4, 18);
            } else {
                h32 += v3 + PRIME5;
            }
            while (pos + 4 <= end) {
                h32 = Integer.rotateLeft(LZ4.read4(buf, pos) * PRIME3 + h32, 17) * PRIME4;
                pos += 4;
            }
            while (pos < end) {
                h32 = Integer.rotateLeft((buf[pos] & 0xff) * PRIME5 + h32, 11) * PRIME1;
                pos += 1;
            }
            h32 = (h32 ^ (h32 >>> 15)) * PRIME2;
            h32 = (h32 ^ (h32 >>> 13)) * PRIME3;
            return h32 ^ (h32 >>> 16);
        }
    }

}
