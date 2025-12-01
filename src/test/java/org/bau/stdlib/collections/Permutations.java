package org.bau.stdlib.collections;

public class Permutations<T> {

    private final T[] in;
    private final T[] out;
    private final int[] index;
    private boolean hasNext = true;

    public Permutations(T[] in, T[] out) {
        this.in = in;
        this.out = out;
        index = new int[in.length];
        for (int i = 0; i < in.length; i++) {
            index[i] = i;
        }
        reverseAfter(out.length - 1);
    }

    private void reverseAfter(int i) {
        int start = i + 1;
        int end = in.length - 1;
        while (start < end) {
            int t = index[start];
            index[start] = index[end];
            index[end] = t;
            start++;
            end--;
        }
    }

    public boolean next() {
        if (!hasNext) {
            return false;
        }
        for (int i = 0; i < out.length; i++) {
            out[i] = in[index[i]];
        }
        int i = in.length - 2;
        while (i >= 0 && index[i] >= index[i + 1]) {
            i--;
        }
        if (i < 0) {
            hasNext = false;
        } else {
            int leastToRightIndex = i + 1;
            for (int j = i + 2; j < in.length; j++) {
                if (index[j] < index[leastToRightIndex] && index[j] > index[i]) {
                    leastToRightIndex = j;
                }
            }
            int t = index[i];
            index[i] = index[leastToRightIndex];
            index[leastToRightIndex] = t;
            if (out.length - 1 > i) {
                reverseAfter(i);
                reverseAfter(out.length - 1);
            }
        }
        return true;
    }

}
