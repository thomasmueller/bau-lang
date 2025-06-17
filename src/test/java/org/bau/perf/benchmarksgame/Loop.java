package org.bau.perf.benchmarksgame;

import java.lang.reflect.Method;
import java.util.Arrays;

public class Loop {

    public static void main(String... args) throws Exception {
        if (args.length < 1) {
            throw new IllegalArgumentException();
        }
        Method main = Class.forName(args[0]).getMethod("main", String[].class);
        String[] a2 = Arrays.copyOfRange(args, 1, args.length);
        for (int i = 0; i < 3; i++) {
            // Just an attempt to collect garbage
            System.gc();
            long start = System.currentTimeMillis();
            main.invoke(null, (Object) a2);
            long time = System.currentTimeMillis() - start;
            System.out.printf("Run #%d: %3.3f s\n", i, time / 1000.0);
            System.gc();
        }
    }
}