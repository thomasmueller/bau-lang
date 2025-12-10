package org.bau.stdlib.util;

import java.util.Random;

/**
 * Reservoir sampling. When adding more than 1 billion entries, new entries
 * replace existing entries slightly more often: with a fixed probability of 1:1
 * billion.
 *
 * @param <T> the type
 */
public class ReservoirSampling<T> {

    private final Random random;
    private final int resultSize;
    private T[] array;
    private int pos;
    private long count;

    @SuppressWarnings("unchecked")
    public ReservoirSampling(int count, Random random) {
        this.resultSize = count;
        this.array = (T[]) new Object[count];
        this.random = random;
    }

    public void add(T obj) {
        if (pos < resultSize) {
            array[pos] = obj;
            pos++;
            return;
        }
        count++;
        long r = random.nextLong();
        int reduce = (int) Math.min(count, 1_000_000_000);
        int index = Hash.reduce((int) r, reduce);
        if (index < resultSize) {
            array[index] = obj;
        }
    }

    public T[] getArray() {
        return array;
    }

    public long getCount() {
        return count;
    }

}
