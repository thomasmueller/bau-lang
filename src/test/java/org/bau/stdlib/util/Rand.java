package org.bau.stdlib.util;

import java.util.Random;

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

    // Fisher–Yates shuffle
    static <T> void shuffle(T[] array, Random random) {
        for (int i = array.length - 1; i > 0; i--) {
            int j = random.nextInt(i);
            T temp = array[j];
            array[j] = array[i];
            array[i] = temp;
        }
    }

}
