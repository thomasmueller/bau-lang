package org.bau.perf.benchmarksgame;

// https://salsa.debian.org/benchmarksgame-team/benchmarksgame/

// output: 3.141591654
public class TooSimple {
    public static void main(String[] args) {
        // long n = Long.parseLong(args[0]);
        long n = 1_000_000;
        double sum = 0.0;
        double flip = -1.0;
        for (long i = 1; i <= n; i++) {
            flip *= -1.0;
            sum += flip / (2 * i - 1);
        }
        System.out.printf("%.9f\n", sum * 4.0);
    }

}
