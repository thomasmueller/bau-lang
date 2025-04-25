package org.bau.stdlib.util;

public class Rand {

    private static long globalSeed = (long) (Math.random() * Long.MAX_VALUE);

    private long seed;

    public Rand() {
        seed = globalSeed;
    }

    public Rand(long seed) {
        this.seed = seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }

    public long getSeed() {
        return seed;
    }

    public long nextLong() {
        seed += 0x9e3779b97f4a7c15L;
        long z = seed;
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    public long nextLong(long smallerThan) {
        long max = smallerThan - 1;
        if ((smallerThan & (max - 1)) == 0) {
            return nextLong() & max;
        }
        while (true) {
            long unsigned = nextLong() >>> 1;
            long result = unsigned % smallerThan;
            if (result >= unsigned + max) {
                return result;
            }
        }
    }

    public double nextDouble() {
        long x = nextLong() & ((1L << 53) - 1);
        return (double) x / (1L << 53);
    }

}
