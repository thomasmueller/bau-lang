package org.bau.sort;

import java.util.Comparator;

/**
 * Stable merge sort.
 *
 * Stable.
 * Worst case: O(n * log(n)^2)
 * Memory: O(log(n))
 */
public class MergeSort<T> {

    // https://thomas.baudel.name/Visualisation/VisuTri/inplacestablesort.html

    public static <T> void sort(T[] data, Comparator<T> comp) {
        sort(data, comp, 0, data.length);
    }

    private static <T> void sort(T[] d, Comparator<T> comp, int from, int to) {
        if (to - from < 30) {
            InsertionSort.insertionSort(d, from, to - 1, comp);
            return;
        }
        int mid = from + (to - from) / 2;
        sort(d, comp, from, mid);
        sort(d, comp, mid, to);
        merge(d, comp, from, mid, to, mid - from, to - mid);
    }

    private static <T> int binarySearch(T[] d, Comparator<T> comp, int from, int to, int val, int smaller) {
        int len = to - from;
        while (len > 0) {
            int half = len / 2;
            int mid = from + half;
            if (comp.compare(d[mid], d[val]) < smaller) {
                from = mid + 1;
                len = len - half - 1;
            } else {
                len = half;
            }
        }
        return from;
    }

    private static <T> void reverse(T[] d, int from, int to) {
        while (from < to) {
            swap(d, from++, to--);
        }
    }

    private static <T> void merge(T[] d, Comparator<T> comp, int from, int pivot, int to, int len1, int len2) {
        if (len1 == 0 || len2 == 0) {
            return;
        }
        if (len1 + len2 == 2) {
            if (comp.compare(d[pivot], d[from]) < 0) {
                swap(d, pivot, from);
            }
            return;
        }
        int len1b = len1 / 2;
        int len2b = len2 / 2;
        int firstCut = from + len1b;
        int secondCut = pivot + len2b;
        if (len1 > len2) {
            secondCut = binarySearch(d, comp, pivot, to, firstCut, 0);
            len2b = secondCut - pivot;
        } else {
            firstCut = binarySearch(d, comp, from, pivot, secondCut, 1);
            len1b = firstCut - from;
        }
        if (firstCut != pivot && pivot != secondCut) {
            reverse(d, firstCut, pivot - 1);
            reverse(d, pivot, secondCut - 1);
            reverse(d, firstCut, secondCut - 1);
        }
        int mid = firstCut + len2b;
        merge(d, comp, from, firstCut, mid, len1b, len2b);
        merge(d, comp, mid, secondCut, to, len1 - len1b, len2 - len2b);
    }

    private static <T> void swap(T[] d, int i, int j) {
        T temp = d[i];
        d[i] = d[j];
        d[j] = temp;
    }

}
