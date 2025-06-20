package org.bau.benchmarks;

// https://github.com/jabbalaci/SpeedTests/blob/master/java/Main.java
// 2025-05-01

public class Munchausen
{
    private final static int LIMIT = 440_000_000;
    private final static int[] cache = getCache();

    private static int[] getCache()
    {
        int[] cache = new int[10];
        cache[0] = 0;
        for (int i = 1; i <= 9; ++i) {
            cache[i] = (int)Math.pow(i, i);
        }
        return cache;
    }

    private boolean isMunchausen(final int number)
    {
        int n = number;
        int total = 0;

        while (n > 0)
        {
            final int digit = n % 10;
            total += cache[digit];
            if (total > number) {
                return false;
            }
            n = n / 10;
        }
        return total == number;
    }

    private void start()
    {
        for (int i = 0; i < LIMIT; ++i)
        {
            if (isMunchausen(i)) {
                System.out.println(i);
            }
        }
    }

    public static void main(String[] args)
    {
        Munchausen m = new Munchausen();
        m.start();
    }
}