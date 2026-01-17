package org.bau.stdlib.compress;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

public class SR3Block {

    private static int[] DT = new int[128];

    static {
        for (int i = 0; i < 128; i++) {
            DT[i] = 512 / (i + 2);
        }
    }

    int di1 = 5;
    int h;
    final int[] state;

    long x1 = 0, x2 = 0xffffffffL, x = 0;

    byte[] input;
    byte[] output;
    int inPos, outPos;

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.out.println("Usage: sr3 c|d input output");
            return;
        }
        long start = System.currentTimeMillis();
        RandomAccessFile in = new RandomAccessFile(args[1], "r");
        byte[] input = new byte[(int) in.length()];
        in.readFully(input);
        in.close();
        byte[] output = new byte[input.length * 10];

        SR3Block sr = new SR3Block(input, output);
        if (args[0].charAt(0) == 'c') {
            sr.output[0] = (byte) 's';
            sr.output[1] = (byte) 'R';
            sr.output[2] = (byte) 2;
            sr.output[3] = (byte) sr.di1;
            sr.outPos = 4;
            sr.compress();
        } else {
            if (sr.input[0] != 's' ||
                    sr.input[1] != 'R' ||
                    sr.input[2] != 2) {
                throw new IOException("Not SR3");
            }
            sr.di1 = sr.input[3];
            sr.inPos = 4;
            sr.decompress();
        }
        RandomAccessFile out = new RandomAccessFile(args[2], "rw");
        out.write(sr.output, 0, sr.outPos);
        out.close();
        System.out.printf("Done in %.2f sec%n", (System.currentTimeMillis() - start) / 1000.0);
    }

    SR3Block(byte[] input, byte[] output) {
        this.input = input;
        this.output = output;
        state = new int[(1024 + 1024) * 258];
        Arrays.fill(state, 1 << 31);
    }

    int probability(int cxt) {
        return state[cxt] >>> 20;
    }

    void update(int cxt, int bit) {
        int tc = state[cxt];
        int n = tc & 127;
        int p = tc >>> 9;
        if (n < 127) {
            tc = (tc + 1) & 0xffffffff;
        }
        long delta = ((long) (bit << 23) - (long) p) * (long) DT[n];
        tc = (int) ((tc + (delta & 0xffffff80L)) & 0xffffffffL);
        state[cxt] = tc;
    }

    void writeBit(int cxt, int bit) {
        int p = probability(cxt);
        long xmid = (x1 + (((x2 - x1) & 0xffffffffL) >>> 12) * p) & 0xffffffffL;
        if (bit == 1) {
            x2 = xmid;
        } else {
            x1 = (xmid + 1) & 0xffffffffL;
        }
        update(cxt, bit);
        while (((x1 ^ x2) & 0xff000000L) == 0) {
            output[outPos++] = (byte) (x2 >>> 24);
            x1 = (x1 << 8) & 0xffffffffL;
            x2 = ((x2 << 8) + 255) & 0xffffffffL;
        }
    }

    void writeByte(int cxt, int x) {
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

    void flush() {
        output[outPos] = (byte) (x1 >>> 24);
        output[outPos + 1] = (byte) 0xff;
        output[outPos + 2] = (byte) 0xff;
        output[outPos + 3] = (byte) 0xff;
        output[outPos + 4] = (byte) 0xff;
        outPos += 8;
    }

    void initRead() {
        for (int i = 0; i < 4; i++) {
            x = ((x << 8) + (input[inPos++] & 0xff)) & 0xffffffffL;
        }
    }

    int readByte(int cxt) {
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

    int readBit(int cxt) {
        int p = probability(cxt);
        long xmid = x1 + ((x2 - x1) >>> 12) * p;
        int bit = (x <= xmid) ? 1 : 0;
        if (bit == 1) {
            x2 = xmid;
        } else {
            x1 = xmid + 1;
        }
        update(cxt, bit);
        while (((x1 ^ x2) & 0xff000000L) == 0) {
            x1 = (x1 << 8) & 0xffffffffL;
            x2 = ((x2 << 8) + 255) & 0xffffffffL;
            x = ((x << 8) + (input[inPos++] & 0xff)) & 0xffffffffL;
        }
        return bit;
    }

    void compress() {
        int c1 = 0; // previous byte
        int[] t4 = new int[0x1000000]; // context -> last 3 bytes + count
        while (true) {
            int r = t4[h];
            int cxt;
            if ((r & 0xffffffffL) >= 0x04000000L) {
                cxt = 1024 + (r >>> 20);
            } else {
                cxt = c1 | ((r >>> 16) & 0x3f00);
            }
            cxt *= 258;
            if (inPos >= input.length) {
                // EOF marker
                writeBit(cxt, 1);
                writeBit(cxt + 1, 0);
                writeByte(cxt + 2, r & 0xff);
                break;
            }
            int c = input[inPos++] & 0xff;
            int comp3 = (c * 0x010101) ^ r;
            if ((comp3 & 0xff) == 0) {
                // match first
                writeBit(cxt, 0);
                if ((r & 0xffffffffL) < 0x3f000000L) {
                    t4[h] += 0x01000000;
                }
            } else if ((comp3 & 0xff00) == 0) {
                // match second
                writeBit(cxt, 1);
                writeBit(cxt + 1, 1);
                writeBit(cxt + 2, 0);
                t4[h] = (r & 0xff0000) | ((r << 8) & 0xff00) | c | 0x01000000;
            } else if ((comp3 & 0xff0000) == 0) {
                // match third
                writeBit(cxt, 1);
                writeBit(cxt + 1, 1);
                writeBit(cxt + 2, 1);
                t4[h] = ((r << 8) & 0xffff00) | c | 0x01000000;
            } else {
                // literal
                writeBit(cxt, 1);
                writeBit(cxt + 1, 0);
                writeByte(cxt + 2, c);
                t4[h] = ((r << 8) & 0xffff00) | c;
            }
            c1 = c;
            h = (h * (5 << di1) + c + 1) & 0xffffff;
        }
        flush();
    }

    void decompress() {
        initRead();
        int c1 = 0; // previous byte
        int[] t4 = new int[0x1000000]; // context -> last 3 bytes + count
        while (true) {
            int r = t4[h];
            int cxt;
            if ((r & 0xffffffffL) >= 0x04000000L) {
                cxt = 1024 + (r >>> 20);
            } else {
                cxt = c1 | ((r >>> 16) & 0x3f00);
            }
            cxt *= 258;
            if (readBit(cxt) == 1) {
                if (readBit(cxt + 1) == 1) {
                    if (readBit(cxt + 2) == 1) {
                        // match third
                        c1 = (r >>> 16) & 0xff;
                        t4[h] = ((r << 8) & 0xffff00) | c1 | 0x01000000;
                    } else {
                        // match second
                        c1 = (r >>> 8) & 0xff;
                        t4[h] = (r & 0xff0000) | ((r << 8) & 0xff00) | c1 | 0x01000000;
                    }
                } else {
                    // literal
                    c1 = readByte(cxt + 2);
                    if (c1 == (r & 0xff)) {
                        break; // EOF
                    }
                    t4[h] = ((r << 8) & 0xffff00) | c1;
                }
            } else {
                // match first
                c1 = r & 0xff;
                if ((r & 0xffffffffL) < 0x3f000000L) {
                    t4[h] = r + 0x01000000;
                }
            }
            output[outPos++] = (byte) c1;
            h = (h * (5 << di1) + c1 + 1) & 0xffffff;
        }
    }

}
