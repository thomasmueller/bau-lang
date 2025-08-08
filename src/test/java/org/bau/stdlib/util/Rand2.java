package org.bau.stdlib.util;

public class Rand2 {

    private static long globalSeed = (long) (Math.random() * Long.MAX_VALUE);

    private long s0, s1, s2, s3;

    public Rand2() {
        this(globalSeed);
    }

    public Rand2(long seed) {
        setSeed(seed);
    }

    private static long splitMix(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    public void setSeed(long seed) {
        s0 = seed;
        s1 = splitMix(seed + 1);
        s2 = splitMix(seed + 2);
        s3 = splitMix(seed + 3);
    }

    public long getSeed() {
        return s0;
    }

    public long nextLong() {
        long result = s0 + s3;
        long t = s1 << 17;
        s2 ^= s0;
        s3 ^= s1;
        s1 ^= s2;
        s0 ^= s3;
        s2 ^= t;
        s3 = Long.rotateLeft(s3, 45);
        return result;
    }

    /**
     * A random value below a limit. If the limit is smaller than 2, then 0 is
     * returned.
     *
     * @param smallerThan the result is smaller than this value
     * @return a random number
     */
    public long nextLong(long smallerThan) {
        // https://news.ycombinator.com/item?id=44767508
        // https://www.pcg-random.org/posts/bounded-rands.html
        if (smallerThan <= 1) {
            // instead of throwing an exception
            return 0;
        }
        long mask = ~0L >>> Long.numberOfLeadingZeros(smallerThan - 1);
        while (true) {
            long x = nextLong() & mask;
            if (x < smallerThan) {
                return x;
            }
        }
    }

    public double nextDouble() {
        long x = nextLong() & ((1L << 53) - 1);
        return (double) x / (1L << 53);
    }

}
