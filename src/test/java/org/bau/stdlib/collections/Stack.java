package org.bau.stdlib.collections;

public class Stack<T> {
    private T[] array;
    private int size;

    @SuppressWarnings("unchecked")
    public Stack(int capacity) {
        array = (T[]) new Object[capacity];
    }

    public boolean isFull() {
        return size == array.length;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean  push(T value) {
        if (isFull()) {
            return false;
        } else {
            array[size++] = value;
            return true;
        }
    }

    public T pop() {
        if (isEmpty()) {
            return null;
        } else {
            return array[--size];
        }
    }

    public T peek() {
        if (isEmpty()) {
            return null;
        } else {
            return array[size - 1];
        }
    }

}
