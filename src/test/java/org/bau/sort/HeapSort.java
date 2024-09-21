package org.bau.sort;

import java.util.Comparator;

/**
 * Non-recursive heap sort.
 *
 * Not stable.
 * Worst case: O(n * log(n))
 * Memory: O(1)
 */
public class HeapSort {

    // https://en.oi-wiki.org/basic/heap-sort/

    public static <T> void sort(T[] a, Comparator<T> c) {
        sort(a, c, 0, a.length);
    }

    public static <T> void sort(T[] a, Comparator<T> c, int from, int to) {
        int len = to - from;
        for (int k = 1; k >= 0; k--) {
            for (int i = len - 1; i >= 0; i--) {
                T tmp = a[from];
                a[from] = a[from + i];
                a[from + i] = tmp;
                int right = i + k * (len - i) - 1;
                int parent = i * k;
                int child = parent * 2 + 1;
                while (child <= right) {
                    if (child + 1 <= right && c.compare(a[from + child], a[from + child + 1]) < 0) {
                        child++;
                    }
                    if (c.compare(a[from + parent], a[from + child]) >= 0) {
                        break;
                    }
                    tmp = a[from + parent];
                    a[from + parent] = a[from + child];
                    a[from + child] = tmp;
                    parent = child;
                    child = parent * 2 + 1;
                }
            }
        }
    }

}
