package org.bau.stdlib.sort;

import java.util.Comparator;

/**
 * Insertion sort.
 *
 * Not stable.
 * Worst case: O(n^2)
 * Memory: O(1)
 */
public class InsertionSort {

    public static <T> boolean isSorted(T[] d, int left, int right, Comparator<T> comp) {
        for(int i = right - 1; i > left; i--) {
            if (comp.compare(d[i], d[i + 1]) > 0) {
                return false;
            }
        }
        return true;
    }

    public static <T> void insertionSort(T[] d, int left, int right, Comparator<T> comp) {
        for (int i = left + 1, j; i <= right; i++) {
            T t = d[i];
            for (j = i - 1; j >= left && comp.compare(d[j], t) > 0; j--) {
                d[j + 1] = d[j];
            }
            d[j + 1] = t;
        }
    }

    public static <T> void binaryInsertionSort(T[] d, int left, int right, Comparator<T> comp) {
        for (int i = left + 1; i <= right; i++) {
            T x = d[i];
            int ins = binarySearch(d, x, left, i - 1, comp);
            for (int j = i - 1; j >= ins; j--) {
                d[j + 1] = d[j];
            }
            d[ins] = x;
        }
    }

    public static <T> int binarySearch(T[] d, T x, int from, int to, Comparator<T> comp) {
        while (from <= to) {
            int m = (from + to) >>> 1;
            if (comp.compare(x, d[m]) >= 0) {
                from = m + 1;
            } else {
                to = m - 1;
            }
        }
        return from;
    }

}
