package org.bau.stdlib.collections;

import java.util.ArrayList;

public class PriorityHeap<T extends Comparable<T>> {
    private ArrayList<T> list = new ArrayList<>();

    public void add(T element) {
        list.add(element);
        int index = list.size() - 1;
        while (index > 0) {
            int parent = (index - 1) / 2;
            if (list.get(index).compareTo(list.get(parent)) >= 0) {
                break;
            }
            swap(index, parent);
            index = parent;
        }
    }

    public T poll() {
        if (list.isEmpty()) {
            return null;
        }
        T min = list.get(0);
        T last = list.remove(list.size() - 1);
        if (list.isEmpty()) {
            return min;
        }
        list.set(0, last);
        int index = 0;
        while (true) {
            int left = 2 * index + 1;
            int right = 2 * index + 2;
            int smallest = index;
            if (left < list.size() && list.get(left).compareTo(list.get(smallest)) < 0) {
                smallest = left;
            }
            if (right < list.size() && list.get(right).compareTo(list.get(smallest)) < 0) {
                smallest = right;
            }
            if (smallest == index) {
                break;
            }
            swap(index, smallest);
            index = smallest;
        }
        return min;
    }

    public T peek() {
        return isEmpty() ? null : list.get(0);
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    private void swap(int i, int j) {
        T temp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, temp);
    }

}
