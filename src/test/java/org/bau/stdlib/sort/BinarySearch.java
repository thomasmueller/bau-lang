package org.bau.stdlib.sort;

public class BinarySearch {

    public static <T extends Comparable<T>> int binarySearch(T[] d, T x) {
        return binarySearch(d, x, 0, d.length);
    }

    public static <T extends Comparable<T>> int binarySearch(T[] d, T x, int from, int to) {
        while (from < to) {
            int m = (from + to) >>> 1;
            int cmp = x.compareTo(d[m]);
            if (cmp > 0) {
                from = m + 1;
            } else if (cmp < 0) {
                to = m;
            } else {
                return m;
            }
        }
        return -from - 1;
    }
}
