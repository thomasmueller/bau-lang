package org.bau.stdlib.util;

public class BloomFilter {

    private final int k;
    private final int arraySize;
    private final long[] data;

    private BloomFilter(long[] data, int k) {
        this.data = data;
        this.k = k;
        this.arraySize = data.length;
    }

    public static BloomFilter construct(long n, double fpp) {
        long m = calculateBits(n, fpp);
        int k = calculateK((double) m / n);
        return new BloomFilter(new long[(int) ((m + 63) / 64)], k);
    }

    // See also https://hur.st/bloomfilter

    public static int calculateK(double bitsPerKey) {
        return Math.max(1, (int) Math.round(bitsPerKey * Math.log(2)));
    }

    public static long calculateBits(long n, double fpp) {
        return (long) Math.ceil((n * Math.log(fpp)) / Math.log(1 / Math.pow(2, Math.log(2))));
    }

    public static long calculateN(long bits, double fpp) {
        return (long) Math.ceil((bits * Math.log(Math.pow(0.5, Math.log(2))) / Math.log(fpp)));
    }

    public static double calculateFpp(long n, long bits, int k) {
        // p = pow(1 - exp(-k / (m / n)), k)
        return Math.pow(1 - Math.exp(-k / ((double) bits / n)), k);
    }

    public void add(long hash) {
        long a = (hash >>> 32) | (hash << 32);
        long b = hash;
        for (int i = 0; i < k; i++) {
            data[Hash.reduce((int) (a >>> 32), arraySize)] |= 1L << a;
            a += b;
        }
    }

    public boolean mayContain(long hash) {
        long a = (hash >>> 32) | (hash << 32);
        long b = hash;
        for (int i = 0; i < k; i++) {
            if ((data[Hash.reduce((int) (a >>> 32), arraySize)] & 1L << a) == 0) {
                return false;
            }
            a += b;
        }
        return true;
    }

    public long getBitCount() {
        return data.length * 64L;
    }

    public int getK() {
        return k;
    }

    public long getEstimatedEntryCount() {
        long x = 0;
        for (long d : data) {
            x += Long.bitCount(d);
        }
        double m = getBitCount();
        return (long) (-(m / k) * Math.log(1 - (x / m)));
    }

}
