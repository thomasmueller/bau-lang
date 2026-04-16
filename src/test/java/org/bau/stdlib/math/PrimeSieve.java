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

    static int countTrailingZeros(long n) {
        if (n == 0)
            return 64;
        return Long.numberOfTrailingZeros(n);
    }

    // https://www.jeremykun.com/2026/04/07/deterministic-miller-rabin/
    public static boolean isPrime32(long n) {
        long bases[] = { 2, 3, 5, 7 };
        if (n < 2) {
            return false;
        }
        if (n < 4) {
            return true;
        }
        if ((n & 1) == 0) {
            return false;
        }
        long d = n - 1;
        int s = countTrailingZeros(d);
        d = d >> s;
        for (long a : bases) {
            if (n <= a) {
                break;
            }
            long x = modularExponentiation(a, d, n);
            if (x == 1 || x == n - 1) {
                continue;
            }
            boolean composite = true;
            for (long r = 1; r < s; ++r) {
                // Doesn't overflow because it is at most n < 32 bits
                x = (x * x) % n;
                if (x == n - 1) {
                    composite = false;
                    break;
                }
            }
            if (composite)
                return false;
        }
        return true;
    }

    private static long modularExponentiation(long base, long exponent, long modulus) {
        long res = 1;
        long b = base % modulus;
        long e = exponent;
        while (e > 0) {
            if ((e & 1) != 0) {
                // Doesn't overflow because we assume 32-bit integer inputs
                res = (res * b) % modulus;
            }
            b = (b * b) % modulus;
            e >>= 1;
        }
        return res;
    }

}