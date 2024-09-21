package org.bau.sort.number;

public class NumberShellSort {

    public static void sort(long[] a) {
        sort(a, 0, a.length);
    }

    public static void sort(long[] a, int from, int to) {
        int h = 16, gap = 1;
        while (to - from > h / 16) {
            h = h + h + h / 4 + 16;
        }
        do {
            h = (h - 16) * 4 / 9;
            gap = (int) ((h + 15) / 16);
            for (int i = gap + from; i < to; i++) {
                long t = a[i];
                int j = i;
                for (; j >= gap + from && a[j - gap] > t; j -= gap) {
                    a[j] = a[j - gap];
                }
                a[j] = t;
            }
        } while (gap > 1);
    }

}
