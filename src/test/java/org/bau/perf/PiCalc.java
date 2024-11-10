package org.bau.perf;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PiCalc {

    public static void main(String[] args) {
        for (int x = 1; x < 20; x++) {
            if (x < 10) {
                double y = 0.0;
                for (double i = 0; i <= x; i++) {
                    y = y + (1.0 / Math.pow(16.0, i)) *
                            ((4.0 / (8.0 * i + 1.0)) - (2.0 / (8.0 * i + 4.0))
                            - (1.0 / (8.0 * i + 5.0)) - (1.0 / (8.0 * i + 6.0)));
                }
                System.out.println(y);
            } else {
                BigDecimal pi = BigDecimal.ZERO;
                for (int i = 0; i <= x; i++) {
                    BigDecimal ii = BigDecimal.valueOf(i);
                    BigDecimal a = BigDecimal.valueOf(1).divide(BigDecimal.valueOf(16).pow(i), x, RoundingMode.HALF_UP);
                    BigDecimal b1 = BigDecimal.valueOf(4).divide(
                            BigDecimal.valueOf(8).multiply(ii).add(BigDecimal.valueOf(1)), x, RoundingMode.HALF_UP);
                    BigDecimal b2 = BigDecimal.valueOf(2).divide(
                            BigDecimal.valueOf(8).multiply(ii).add(BigDecimal.valueOf(4)), x, RoundingMode.HALF_UP);
                    BigDecimal b3 = BigDecimal.valueOf(1).divide(
                            BigDecimal.valueOf(8).multiply(ii).add(BigDecimal.valueOf(5)), x, RoundingMode.HALF_UP);
                    BigDecimal b4 = BigDecimal.valueOf(1).divide(
                            BigDecimal.valueOf(8).multiply(ii).add(BigDecimal.valueOf(6)), x, RoundingMode.HALF_UP);
                    BigDecimal b = b1.subtract(b2).subtract(b3).subtract(b4);
                    pi = pi.add(a.multiply(b));
                }
                System.out.println(pi);
            }
        }
    }
}
