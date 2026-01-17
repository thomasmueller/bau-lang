package org.bau.stdlib.compress;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import org.bau.stdlib.util.Hash;

public class LZArith {

    private static int[] DT = new int[128];

    static {
        for (int i = 0; i < 128; i++) {
            DT[i] = 512 / (i + 2);
        }
    }

    final static int BLOCK_SIZE = 4 * 1024 * 1024;
    private final int[] hashTable = new int[64 * 1024];
    private final int[] prevTable = new int[64 * 1024];

    final InputStream in;
    final OutputStream out;

    final int[] state;
    long x1 = 0, x2 = 0xFFFFFFFFL, x = 0;

    LZArith(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
        state = new int[256 * (256 + 10)];
        Arrays.fill(state, 1 << 31);
        Arrays.fill(hashTable, -1);
        Arrays.fill(prevTable, -1);
    }

    int probability(int cxt) {
        return state[cxt] >>> 20;
    }

    void update(int cxt, int bit) {
        int tc = state[cxt];
        int n = tc & 127;
        int p = tc >>> 9;
        if (n < 127) {
            tc = (tc + 1) & 0xFFFFFFFF;
        }
        long delta = ((long) (bit << 23) - (long) p) * (long) DT[n];
        tc = (int) ((tc + (delta & 0xFFFFFF80L)) & 0xFFFFFFFFL);
        state[cxt] = tc;
    }

    void writeBit(int cxt, int bit) throws IOException {
        int p = probability(cxt);
        long xmid = (x1 + (((x2 - x1) & 0xFFFFFFFFL) >>> 12) * p) & 0xFFFFFFFFL;
        if (bit == 1) {
            x2 = xmid;
        } else {
            x1 = (xmid + 1) & 0xFFFFFFFFL;
        }
        update(cxt, bit);
        while (((x1 ^ x2) & 0xFF000000L) == 0) {
            out.write((int) (x2 >>> 24));
            x1 = (x1 << 8) & 0xFFFFFFFFL;
            x2 = ((x2 << 8) + 255) & 0xFFFFFFFFL;
        }
    }

    void writeByte(int cxt, int x) throws IOException {
        int b = (x >>> 4) + 16;
        writeBit(cxt + 1, (b >> 3) & 1);
        writeBit(cxt + (b >> 3), (b >> 2) & 1);
        writeBit(cxt + (b >> 2), (b >> 1) & 1);
        writeBit(cxt + (b >> 1), b & 1);
        cxt += 15 * (b - 15);
        b = (x & 15) | 16;
        writeBit(cxt + 1, (b >> 3) & 1);
        writeBit(cxt + (b >> 3), (b >> 2) & 1);
        writeBit(cxt + (b >> 2), (b >> 1) & 1);
        writeBit(cxt + (b >> 1), b & 1);
    }

    void flush() throws IOException {
        out.write((int) (x1 >>> 24));
        out.flush();
    }

    void initRead() throws IOException {
        for (int i = 0; i < 4; i++) {
            x = ((x << 8) + (in.read() & 0xFF)) & 0xFFFFFFFFL;
        }
    }

    int readByte(int cxt) throws IOException {
        int hi = 1, lo = 1;
        hi += hi + readBit(cxt + hi);
        hi += hi + readBit(cxt + hi);
        hi += hi + readBit(cxt + hi);
        hi += hi + readBit(cxt + hi);
        cxt += 15 * (hi - 15);
        lo += lo + readBit(cxt + lo);
        lo += lo + readBit(cxt + lo);
        lo += lo + readBit(cxt + lo);
        lo += lo + readBit(cxt + lo);
        return ((hi - 16) << 4) | (lo - 16);
    }

    int readBit(int cxt) throws IOException {
        int p = probability(cxt);
        long xmid = x1 + ((x2 - x1) >>> 12) * p;
        int bit = (x <= xmid) ? 1 : 0;
        if (bit == 1) {
            x2 = xmid;
        } else {
            x1 = xmid + 1;
        }
        update(cxt, bit);
        while (((x1 ^ x2) & 0xFF000000L) == 0) {
            x1 = (x1 << 8) & 0xFFFFFFFFL;
            x2 = ((x2 << 8) + 255) & 0xFFFFFFFFL;
            x = ((x << 8) + (in.read() & 0xFF)) & 0xFFFFFFFFL;
        }
        return bit;
    }

    public static byte[] compress(byte[] data) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        LZArith lza = new LZArith(in, out);
        int[] compressed = new int[data.length * 2];
        int len = lza.lzCompressBlock(data, data.length, compressed, 0);
        int prevLiteral = 0;
        int p = 0;
        for (int i = 0; i < len;) {
            int literalLen = compressed[i++];
            for (int j = 0; j < literalLen; j++) {
                lza.writeBit(0, 0);
                int x = compressed[i++];
                lza.writeByte((5 + prevLiteral) * 256, x);
                prevLiteral = x & 0x3f;
                p++;
            }
            if (i >= len) {
                break;
            }
            int runLen = -compressed[i++];
            assertTrue(runLen >= 0);
            if (runLen >= 0) {
                int offset = compressed[i++];
                lza.writeBit(0, 1);
                int x = runLen;
                while (x >= 255) {
                    lza.writeByte(10 + 256, 255);
                    x -= 255;
                }
                lza.writeByte(10 + 256, x);
                if(offset != 1) {
                    if (runLen > 5) {
                        lza.writeByte(10 + 2 * 256, (offset >>> 8) & 0xff);
                    }
                    lza.writeByte(10 + 3 * 256, (offset & 0xff));
                }
                p += runLen;
                prevLiteral = data[p - 1] & 0x3f;
            }
        }
        lza.writeBit(0, 1);
        lza.writeBit(1, 0);
        lza.writeByte(10 + 256, 0);
        lza.flush();
        out.flush();
        return out.toByteArray();
    }

    public static byte[] expand(byte[] data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        LZArith lza = new LZArith(new ByteArrayInputStream(data), out);
        lza.initRead();
        int p = 0;
        int prevLiteral = 0;
        byte[] result = new byte[data.length * 10];
        while (true) {
            if (lza.readBit(0) == 0) {
                int x = lza.readByte((5 + prevLiteral) * 256);
                result[p++] = (byte) x;
                prevLiteral = x & 0x3f;
            } else {
                int rle = 0; // lza.readBit(1);
                int runLen = 0;
                while (true) {
                    int x = lza.readByte(10 + 256);
                    runLen += x;
                    if (x != 255) {
                        break;
                    }
                }
                if (runLen == 0) {
                    break;
                }
                int offset = 0;
                if(rle == 1) {
                    offset = 1;
                } else {
                    if (runLen > 5) {
                        offset = lza.readByte(10 + 2 * 256);
                        offset <<= 8;
                    }
                    offset += lza.readByte(10 + 3 * 256);
                }
                for (int j = 0; j < runLen; j++) {
                    result[p] = result[p - offset];
                    prevLiteral = result[p] & 0x3f;
                    p++;
                }
            }
        }
        return Arrays.copyOf(result, p);
    }

    // +literal (0..n), n bytes literals, -run (0..n), offset

    int lzCompressBlock(byte[] inData, int inLen, int[] out, int outPos) {
        if (inLen > inData.length) {
            throw new IllegalArgumentException("Buffer too small");
        }
        int literalLen = 0;
        int inPos = 0;
        while (true) {
            int runLen;
            int backRef = 0;
            if (inPos + 16 < inLen) {
                runLen = 0; // inPos < 1 ? 0 : RLE.runLength(inData, inPos - 1, 3, inData.length - inPos - 2);
                if (runLen != 0) {
                    runLen--;
                    backRef = inPos - 1;
                } else {
                    int h = hash3(inData, inPos);
                    int ref = hashTable[h];
                    prevTable[inPos & 0xffff] = ref;
                    hashTable[h] = inPos;
                    runLen = 0;
                    for (int i = 0; i < 20; i++) {
                        if (ref < 0 || ref >= inPos || ref + 0xffff < inPos) {
                            break;
                        }
                        int thisRun = runLenCount(inData, inLen, inPos, ref);
                        if (thisRun > runLen) {
                            runLen = thisRun;
                            backRef = ref;
                        }
                        int prev = prevTable[ref & 0xffff];
                        if (prev == ref) {
                            throw new AssertionError();
                        }
                        if (prev == 0) {
                            break;
                        }
                        ref = prev;
                    }
                    int offset = inPos - backRef;
                    if (runLen < 3 || (runLen < 6 && offset > 255)) {
                        literalLen++;
                        inPos++;
                        continue;
                    }
                }
                int p = inPos;
                p++;
                for (int j = 1; j < runLen; j++) {
                    int h = hash3(inData, p);
                    prevTable[p & 0xffff] = hashTable[h];
                    hashTable[h] = p;
                    p++;
                }
            } else {
                // the last few bytes in the block
                // are always encoded as literals
                literalLen += inLen - inPos;
                inPos = inLen;
                runLen = 0;
                backRef = 0;
            }
            // possibly increase the match len backwards
//            while (backRef > 0 && literalLen > 0 && inPos > 0 && inData[inPos - 1] == inData[backRef - 1]) {
//                runLen += 1;
//                inPos -= 1;
//                literalLen -= 1;
//                backRef -= 1;
//            }

            out[outPos++] = literalLen;
            for (int i = 0; i < literalLen; i++) {
                int x = inData[inPos - literalLen + i];
                out[outPos++] = x & 0xff;
            }
            if (inPos >= inLen) {
                break;
            }
//            if (outPos < 100)
//                System.out.println("[" + outPos + "]: run = " + runLen);
            out[outPos++] = -runLen;
            int offset = inPos - backRef;
//            if (outPos < 100)
//                System.out.println("[" + outPos + "]: offset = " + offset);
            out[outPos++] = offset;
//            System.out.println("[" + outPos + "]: literal=" + literalLen + " runLen=" + runLen + " offset=" + offset);
            inPos += runLen;
            literalLen = 0;
        }
        return outPos;
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

    private static int hash3(byte[] data, int pos) {
        long x = (data[pos + 0] & 0xffL) | ((data[pos + 1] & 0xffL) << 8) | ((data[pos + 2] & 0xffL) << 16);
        return (int) (Hash.hash(x) & 0xffff);
    }

    private static long read8(byte[] data, int pos) {
        return (data[pos + 0] & 0xffL) | ((data[pos + 1] & 0xffL) << 8) | ((data[pos + 2] & 0xffL) << 16)
                | ((data[pos + 3] & 0xffL) << 24) | ((data[pos + 4] & 0xffL) << 32) | ((data[pos + 5] & 0xffL) << 40)
                | ((data[pos + 6] & 0xffL) << 48) | ((data[pos + 7] & 0xffL) << 56);
    }

}
