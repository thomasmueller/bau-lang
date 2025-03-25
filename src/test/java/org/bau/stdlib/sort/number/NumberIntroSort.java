package org.bau.stdlib.sort.number;

public class NumberIntroSort {

    public static void sort(long[] a) {
        sort(a, 0, a.length);
    }

    public static void sort(long[] a, int from, int to) {
        quicksort(a, from, to - 1, 0);
    }

    private static void quicksort(long a[], int low, int high, int depth) {
        if (high - low < 30) {
            NumberInsertionSort.insertionSort(a, low, high);
            return;
        }
        if (depth > 100) {
            NumberShellSort.sort(a, low, high + 1);
            return;
        }
        int swapCount = 0;
        int pi = low + (high - low) / 2;
        long pivot = a[pi];
        int l = low, h = high;
        while (true) {
            while (pivot > a[l]) {
                l++;
            }
            while (pivot < a[h]) {
                h--;
            }
            if (l >= h) {
                pi = h;
                break;
            }
            swapCount++;
            long temp = a[l];
            a[l] = a[h];
            a[h] = temp;
            l++;
            h--;
        }
        if (swapCount == 0 && NumberInsertionSort.isSorted(a, low, high)) {
            return;
        }
        quicksort(a, low, pi, depth + 1);
        quicksort(a, pi + 1, high, depth + 1);
    }
}
