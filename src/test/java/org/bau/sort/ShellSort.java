package org.bau.sort;

import java.util.Comparator;

/**
 * Shell sort.
 *
 * Not stable.
 * Worst case: O(n ^ ~1.34)
 * Memory: O(log(n))
 */
public class ShellSort<T> {

    public static <T> void sort(T[] a, Comparator<T> c) {
        sort(a, c, 0, a.length);
    }

    public static <T> void sort(T[] a, Comparator<T> c, int from, int to) {
        int h = 16, gap = 1;
        while (to - from > h / 16) {
            h = h + h + h / 4 + 16;
        }
        do {
            h = (h - 16) * 4 / 9;
            gap = (int) ((h + 15) / 16);
            for (int i = gap + from; i < to; i++) {
                T t = a[i];
                int j = i - gap;
                for (; j >= from && c.compare(a[j], t) > 0; j -= gap) {
                    a[j + gap] = a[j];
                }
                a[j + gap] = t;
            }
        } while (gap > 1);

    }

}
