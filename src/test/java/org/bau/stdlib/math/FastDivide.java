package org.bau.stdlib.math;

public class FastDivide {

    private final long multiply;
    private final int shift;

    private FastDivide(long multiply, int shift) {
        this.multiply = multiply;
        this.shift = shift;
    }

    public long divide(long x) {
        return (x * multiply) >>> shift;
    }

    public static FastDivide build(long divisor) {
        if (divisor < 0) {
            throw new IllegalArgumentException("negative divisor");
        } else if (divisor >= Integer.MAX_VALUE / 2) {
            throw new IllegalArgumentException("divisor too large");
        }
        int shift = 32 + (64 - Long.numberOfLeadingZeros(divisor - 1));
        long multiply = (1L << shift) / divisor;

        if ((1L << 32) - multiply * divisor <= multiply - 1) {
            multiply++;
        }
        return new FastDivide(multiply, shift);
    }

}
