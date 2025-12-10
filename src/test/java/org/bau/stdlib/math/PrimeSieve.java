package org.bau.stdlib.math;

import org.bau.stdlib.collections.BitField;

public class PrimeSieve {

    private final int limit;
    private final BitField notPrime;

    private PrimeSieve(int limit, BitField notPrime) {
        this.limit = limit;
        this.notPrime = notPrime;
    }

    static PrimeSieve generate(int limit) {
        BitField notPrime = new BitField(limit >>> 1);
        for (int p = 3; p * p <= limit; p += 2) {
            if (!notPrime.get(p >>> 1)) {
                for (int i = p * p; i <= limit; i += p + p) {
                    notPrime.set(i >>> 1);
                }
            }
        }
        return new PrimeSieve(limit, notPrime);
    }

    public boolean isPrime(long n) {
        if (n <= 3) {
            return n >= 2;
        }
        if (n % 2 == 0 || n % 3 == 0) {
            return false;
        }
        if (n < limit) {
            return !notPrime.get((int) (n >>> 1));
        }
        for (long f = 5; f * f <= n; f += 6) {
            if ((n % f) == 0 || (n % (f + 2)) == 0) {
                return false;
            }
        }
        return true;
    }

}