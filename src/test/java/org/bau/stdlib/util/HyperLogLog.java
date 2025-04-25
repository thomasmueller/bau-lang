package org.bau.stdlib.util;

import java.util.HashSet;

public class HyperLogLog {

    private final double amm2;
    private final int m;
    private final byte[] counters;
    private final int maxSmallSetSize;
    private HashSet<Long> smallSet;

    public HyperLogLog(int m, int maxSmallSetSize) {
        this.maxSmallSetSize = maxSmallSetSize;
        if (maxSmallSetSize > 0) {
            smallSet = new HashSet<>();
        } else {
            smallSet = null;
        }
        if (m < 16) {
            throw new IllegalArgumentException("Must be >= 16, is " + m);
        }
        if (Integer.bitCount(m) != 1) {
            throw new IllegalArgumentException("Must be a power of 2, is " + m);
        }
        this.m = m;
        double am;
        switch (m) {
        case 32:
            am = 0.697;
            break;
        case 64:
            am = 0.709;
            break;
        default:
            am = 0.7213 / (1.0 + 1.079 / m);
        }
        amm2 = am * m * m;
        this.counters = new byte[m];
    }

    public void add(long hash) {
        if (smallSet != null) {
            smallSet.add(hash);
            if (smallSet.size() > maxSmallSetSize) {
                smallSet = null;
            }
        }
        int i = (int) (hash & (m - 1));
        counters[i] = (byte) Math.max(counters[i], 1 + Long.numberOfLeadingZeros(hash));
    }

    public long estimate() {
        if (smallSet != null) {
            return smallSet.size();
        }
        double sum = 0;
        int countZero = 0;
        for(int c : counters) {
            countZero += c == 0 ? 1 : 0;
            sum += 1. / (1L << (c & 0xff));
        }
        if (sum == 0) {
            sum = 1;
        }
        long est = (long) (1. / sum * amm2);
        if (est <= 5 * m && countZero > 0) {
            // linear counting
            est = (long) (m * Math.log((double) m / countZero));
        }
        return est;
    }

}
