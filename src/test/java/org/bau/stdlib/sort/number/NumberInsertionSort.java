package org.bau.stdlib.sort.number;

public class NumberInsertionSort {

    public static <T> boolean isSorted(long[] d, int left, int right) {
        for(int i = right - 1; i > left; i--) {
            if (d[i] > d[i + 1]) {
                return false;
            }
        }
        return true;
    }

    public static <T> void insertionSort(long[] d, int left, int right) {
        for (int i = left + 1, j; i <= right; i++) {
            long t = d[i];
            for (j = i - 1; j >= left && d[j] > t; j--) {
                d[j + 1] = d[j];
            }
            d[j + 1] = t;
        }
    }

    public static <T> void binaryInsertionSort(long[] d, int left, int right) {
        for (int i = left + 1; i <= right; i++) {
            long x = d[i];
            int ins = binarySearch(d, x, left, i - 1);
            for (int j = i - 1; j >= ins; j--) {
                d[j + 1] = d[j];
            }
            d[ins] = x;
        }
    }

    public static <T> int binarySearch(long[] d, long x, int from, int to) {
        while (from <= to) {
            int m = (from + to) >>> 1;
            if (x >= d[m]) {
                from = m + 1;
            } else {
                to = m - 1;
            }
        }
        return from;
    }

}
