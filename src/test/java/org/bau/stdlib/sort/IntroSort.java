package org.bau.stdlib.sort;

import java.util.Comparator;

/**
 * Intro-sort: quicksort, but if recursion depth is larger than 100, then
 * heap sort. For small sets, insertion sort.
 *
 * Not stable.
 * Worst case: O(n * log(n))
 * Memory: O(log(n))
 */
public class IntroSort {

    public static <T> void sort(T[] a, Comparator<T> c) {
        sort(a, c, 0, a.length);
    }

    public static <T> void sort(T[] a, Comparator<T> c, int from, int to) {
        quicksort(a, c, from, to - 1, 0);
    }

    private static <T> void quicksort(T a[], Comparator<T> c, int low, int high, int depth) {
        if (high - low < 30) {
            InsertionSort.insertionSort(a, low, high, c);
            return;
        }
        if (depth > 100) {
            HeapSort.sort(a, c, low, high + 1);
            return;
        }
        int swapCount = 0;
        int pi = low + (high - low) / 2;
        T pivot = a[pi];
        int l = low, h = high;
        while (true) {
            while (c.compare(pivot, a[l]) > 0) {
                l++;
            }
            while (c.compare(pivot, a[h]) < 0) {
                h--;
            }
            if (l >= h) {
                pi = h;
                break;
            }
            swapCount++;
            T temp = a[l];
            a[l] = a[h];
            a[h] = temp;
            l++;
            h--;
        }
        if (swapCount < 3 && InsertionSort.isSorted(a, low, high, c)) {
            return;
        }
        quicksort(a, c, low, pi, depth + 1);
        quicksort(a, c, pi + 1, high, depth + 1);
    }
}
