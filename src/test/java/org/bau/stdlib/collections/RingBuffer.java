package org.bau.stdlib.collections;

import org.bau.stdlib.math.IntMath;

public class RingBuffer<T> {

    private int readPos, writePos;
    private final T[] array;

    @SuppressWarnings("unchecked")
    private RingBuffer(int capacity) {
        array = (T[]) new Object[capacity];
    }

    /**
     * Build a ring buffer with a power-of-two capacity.
     * The next power of two is used.
     * Negative values will result in a capacity 0 buffer.
     *
     * @param <T> the type
     * @param capacity the minimal capacity
     * @return the ring buffer
     */
    public static <T> RingBuffer<T> build(int minCapacity) {
        int capacity;
        if (minCapacity <= 0) {
            capacity = 0;
        } else {
            capacity = (int) IntMath.nextPowerOf2(minCapacity);
        }
        return new RingBuffer<>(capacity);
    }

    public boolean add(T value) {
        if (isFull()) {
            return false;
        }
        array[writePos & (array.length - 1)] = value;
        writePos++;
        return true;
    }

    public T remove() {
        if (isEmpty()) {
            return null;
        }
        T result = array[readPos & (array.length - 1)];
        readPos++;
        return result;
    }

    public boolean isFull() {
        return size() == array.length;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public int size() {
        return writePos - readPos;
    }

    public int capacity() {
        return array.length;
    }

}
