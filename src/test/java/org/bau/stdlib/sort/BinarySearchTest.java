package org.bau.stdlib.sort;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

public class BinarySearchTest {

    @Test
    public void random() {
        Random random = new Random(1);
        for (int i = 0; i < 1_000; i++) {
            int size = random.nextInt(50) + 1;
            Integer[] arr = new Integer[size];
            arr[0] = random.nextInt(10);
            for (int j = 1; j < size; j++) {
                arr[j] = arr[j - 1] + random.nextInt(10) + 1;
            }
            int target;
            if (random.nextBoolean()) {
                target = arr[random.nextInt(size)];
            } else {
                target = random.nextInt(arr[size - 1] + 20);
            }
            int got = BinarySearch.binarySearch(arr, target);
            int expected = Arrays.binarySearch(arr, target, Integer::compare);
            assertEquals(expected, got);
        }
    }
}
