// The Computer Language Benchmarks Game
// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

package org.bau.benchmarks;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Mandelbrot {

    public static void main(String[] args) throws IOException {
        int w, h, bitNum = 0;
        int iter = 50;
        double limit = 2;
        byte byteAcc = 0;
        int n = 200;
        if (args.length > 0)
            n = Integer.parseInt(args[0]);
        w = h = n;
        OutputStream out = new BufferedOutputStream(System.out);
        out.write(("P4\n" + w + " " + h + "\n").getBytes());
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double Zr = 0, Zi = 0, Tr = 0, Ti = 0;
                double Cr = 2.0 * x / w - 1.5;
                double Ci = 2.0 * y / h - 1;
                for (int i = 0; i < iter && Tr + Ti <= limit * limit; i++) {
                    Zi = 2.0 * Zr * Zi + Ci;
                    Zr = Tr - Ti + Cr;
                    Tr = Zr * Zr;
                    Ti = Zi * Zi;
                }
                byteAcc <<= 1;
                if (Tr + Ti <= limit * limit)
                    byteAcc |= 0x01;
                bitNum++;
                if (bitNum == 8) {
                    out.write(byteAcc);
                    byteAcc = 0;
                    bitNum = 0;
                } else if (x == w - 1) {
                    byteAcc <<= (8 - (w % 8));
                    out.write(byteAcc);
                    byteAcc = 0;
                    bitNum = 0;
                }
            }
        }
        out.flush();
    }
}
