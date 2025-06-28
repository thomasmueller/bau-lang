// https://github.com/jabbalaci/SpeedTests

package org.bau.benchmarks;

public class Munchausen {
    static int LIMIT = 440_000_000;
    static int[] cache = getCache();

    public static void main(String[] args) {
        for (int i = 0; i < LIMIT; ++i)
            if (isMunchausen(i))
                System.out.println(i);
    }

    static boolean isMunchausen(int number) {
        int n = number;
        int total = 0;
        while (n > 0) {
            int digit = n % 10;
            total += cache[digit];
            if (total > number)
                return false;
            n = n / 10;
        }
        return total == number;
    }

    static int[] getCache() {
        int[] cache = new int[10];
        cache[0] = 0;
        for (int i = 1; i <= 9; ++i)
            cache[i] = (int) Math.pow(i, i);
        return cache;
    }

}