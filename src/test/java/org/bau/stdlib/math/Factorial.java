package org.bau.stdlib.math;

import java.math.BigInteger;

import org.junit.Test;

public class Factorial {

    // TODO see https://github.com/thomasmueller/minperf/blob/master/src/test/java/org/minperf/Probability.java

    @Test
    public void test() {
        for (int i = 0; i < 500; i += 10) {
            long f1 = factorial(i);
            double f2 = factorial2(i);
            double f3 = factorial3(i);
            // System.out.println(i + " " + f1 + " " + f2 + " " + f3);

            // 52 8.065817517×10^67
            // 70 1.197857167×10^100
            // 100 9.332621544×10^157
            // 100 9223372036854775807 9.332621538103648E157 9.332621544394415E157
        }
    }

    long factorial(long n) {
        long result = 1;
        for (int i = 1; i <= n && result > 0; i++) {
            result *= i;
        }
        if (result < 0) {
            return Long.MAX_VALUE;
        }
        return result;
    }

    double factorial2(double n) {
        double z = n + 1;
        return Math.sqrt(2 * Math.PI / z) * Math.pow((z + 1 / (12 * z - (1 / 10 / z))) / Math.E, z);
    }

    double factorial3(long n) {
        BigInteger x = BigInteger.ONE;
        for (int i = 1; i <= n; i++) {
            x = x.multiply(BigInteger.valueOf(i));
        }
        return x.doubleValue();
    }

}
