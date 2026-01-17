package org.bau.stdlib.compress;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class SR3 {

    private static int[] DT = new int[128];

    static {
        for (int i = 0; i < 128; i++) {
            DT[i] = 512 / (i + 2);
        }
    }

    final InputStream in;
    final OutputStream out;
    int di1 = 5;
    int h;
    final int[] state;

    long x1 = 0, x2 = 0xFFFFFFFFL, x = 0;

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.out.println("Usage: sr3 c|d input output");
            return;
        }
        long start = System.currentTimeMillis();
        SR3 sr = new SR3(new BufferedInputStream(new FileInputStream(args[1])),
                new BufferedOutputStream(new FileOutputStream(args[2])));
        if (args[0].charAt(0) == 'c') {
            sr.out.write(new byte[] { 's', 'R', 2 });
            sr.out.write(sr.di1);
            sr.compress();
        } else {
            if (sr.in.read() != 's' || sr.in.read() != 'R' || sr.in.read() != 2) {
                throw new IOException("Not SR3");
            }
            sr.di1 = sr.in.read();
            sr.decompress();
        }
        System.out.printf("Done in %.2f sec%n", (System.currentTimeMillis() - start) / 1000.0);
    }

    SR3(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
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

    void compress() throws IOException {
        int c1 = 0; // previous byte
        int[] t4 = new int[0x1000000]; // context -> last 3 bytes + count
        while (true) {
            int r = t4[h];
            int cxt;
            if ((r & 0xFFFFFFFFL) >= 0x04000000L) {
                cxt = 1024 + (r >>> 20);
            } else {
                cxt = c1 | ((r >>> 16) & 0x3F00);
            }
            cxt *= 258;
            int c = in.read();
            if (c == -1) {
                // EOF marker
                writeBit(cxt, 1);
                writeBit(cxt + 1, 0);
                writeByte(cxt + 2, r & 0xFF);
                break;
            }
            int comp3 = (c * 0x010101) ^ r;
            if ((comp3 & 0xFF) == 0) {
                // match first
                writeBit(cxt, 0);
                if ((r & 0xFFFFFFFFL) < 0x3F000000L) {
                    t4[h] += 0x01000000;
                }
            } else if ((comp3 & 0xFF00) == 0) {
                // match second
                writeBit(cxt, 1);
                writeBit(cxt + 1, 1);
                writeBit(cxt + 2, 0);
                t4[h] = (r & 0xFF0000) | ((r << 8) & 0xFF00) | c | 0x01000000;
            } else if ((comp3 & 0xFF0000) == 0) {
                // match third
                writeBit(cxt, 1);
                writeBit(cxt + 1, 1);
                writeBit(cxt + 2, 1);
                t4[h] = ((r << 8) & 0xFFFF00) | c | 0x01000000;
            } else {
                // literal
                writeBit(cxt, 1);
                writeBit(cxt + 1, 0);
                writeByte(cxt + 2, c);
                t4[h] = ((r << 8) & 0xFFFF00) | c;
            }
            c1 = c;
            h = (h * (5 << di1) + c + 1) & 0xFFFFFF;
        }
        flush();
        out.flush();
    }

    void decompress() throws IOException {
        initRead();
        int c1 = 0; // previous byte
        int[] t4 = new int[0x1000000]; // context -> last 3 bytes + count
        while (true) {
            int r = t4[h];
            int cxt;
            if ((r & 0xFFFFFFFFL) >= 0x04000000L) {
                cxt = 1024 + (r >>> 20);
            } else {
                cxt = c1 | ((r >>> 16) & 0x3F00);
            }
            cxt *= 258;
            if (readBit(cxt) == 1) {
                if (readBit(cxt + 1) == 1) {
                    if (readBit(cxt + 2) == 1) {
                        // match third
                        c1 = (r >>> 16) & 0xFF;
                        t4[h] = ((r << 8) & 0xFFFF00) | c1 | 0x01000000;
                    } else {
                        // match second
                        c1 = (r >>> 8) & 0xFF;
                        t4[h] = (r & 0xFF0000) | ((r << 8) & 0xFF00) | c1 | 0x01000000;
                    }
                } else {
                    // literal
                    c1 = readByte(cxt + 2);
                    if (c1 == (r & 0xFF)) {
                        break; // EOF
                    }
                    t4[h] = ((r << 8) & 0xFFFF00) | c1;
                }
            } else {
                // match first
                c1 = r & 0xFF;
                if ((r & 0xFFFFFFFFL) < 0x3F000000L) {
                    t4[h] = r + 0x01000000;
                }
            }
            out.write(c1);
            h = (h * (5 << di1) + c1 + 1) & 0xFFFFFF;
        }
        out.flush();
    }

}
