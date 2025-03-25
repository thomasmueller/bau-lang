package org.bau.stdlib.sort.number;

public class CountingSort {

    public static void sort(long[] a) {
        sort(a, 0, a.length);
    }

    public static void sort(long[] a, int from, int to) {
        if (from >= to) {
            return;
        }
        long last = a[from];
        long min = last;
        long max = last;
        boolean sorted = true;
        for (int i = from + 1; i < to; i++) {
            long x = a[i];
            if (x < last) {
                sorted = false;
            }
            min = Math.min(min, x);
            max = Math.max(max, x);
            last = x;
        }
        if (sorted) {
            return;
        }
        int bitsPerLevel = 16;
        int shift = 64 - bitsPerLevel;
        long add = 0;
        if (Math.signum(min) == Math.signum(max)) {
            while (shift > 0 && (1L << shift) > (max ^ min)) {
                shift -= bitsPerLevel;
            }
        } else {
            add = Long.MIN_VALUE;
        }
        sortUnsigned(a, shift, add, bitsPerLevel, from, to);
    }

    public static void sortUnsigned(long[] data, int shift, long add, int bitsPerLevel, int from, int to) {
        int buckets = 1 << bitsPerLevel;
        int mask = buckets - 1;
        if (to - from < buckets / 2 || shift < 0) {
            NumberIntroSort.sort(data, from, to);
            return;
        }
        int[] histogram = new int[buckets];
        int len = to - from;
        long[] buffer = new long[len];
        for (int i = from; i < to; i++) {
            int b = (int)(((data[i] + add) >>> shift) & mask);
            histogram[b]++;
        }
        int start = 0;
        for (int i = 0; i < histogram.length; i++) {
            int x = histogram[i];
            histogram[i] = start;
            start += x;
        }
        for (int i = from; i < to; i++) {
            int b = (int)(((data[i] + add) >>> shift) & mask);
            int index = histogram[b]++;
            buffer[index] = data[i];
        }
        System.arraycopy(buffer, 0, data, from, len);
        if (shift <= 0) {
            return;
        }
        int begin = 0;
        for (int b = 0; b < histogram.length; b++) {
            int end = histogram[b];
            if (end - begin > 1) {
                sortUnsigned(data, shift - bitsPerLevel, 0, bitsPerLevel, from + begin, from + end);
            }
            begin = end;
        }
    }

}
