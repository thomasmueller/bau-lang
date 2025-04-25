package org.bau.stdlib.util;

import java.util.Arrays;

public class Deque<T> {
    private T[] array;
    private int head, tail, size;

    @SuppressWarnings("unchecked")
    public Deque(int capacity) {
        array = (T[]) new Object[capacity];
    }

    public boolean addFirst(T value) {
        if (size == array.length) {
            grow();
        }
        head = (head - 1 + array.length) % array.length;
        array[head] = value;
        size++;
        return true;
    }

    public boolean add(T value) {
        if (size == array.length) {
            grow();
        }
        array[tail] = value;
        tail = (tail + 1) % array.length;
        size++;
        return true;
    }

    public T remove() {
        if (size == 0) {
            return null;
        }
        T value = array[head];
        array[head] = null;
        head = (head + 1) % array.length;
        size--;
        return value;
    }

    public T removeLast() {
        if (size == 0) {
            return null;
        }
        tail = (tail - 1 + array.length) % array.length;
        T value = array[tail];
        array[tail] = null;
        size--;
        return value;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean isFull() {
        return size == array.length;
    }

    public Object size() {
        return size;
    }

    private void grow() {
        int oldSize = array.length;
        int grow;
        if (oldSize < 128) {
            grow = oldSize;
        } else {
            grow = oldSize / 2;
        }
        T[] newArray = Arrays.copyOf(array, oldSize + grow);
        if (tail < head) {
            for(int i=0; i < oldSize - head; i++) {
                newArray[head + grow + i] = newArray[head + i];
                newArray[head + i] = null;
            }
            head += grow;
        }
        array = newArray;
    }

}