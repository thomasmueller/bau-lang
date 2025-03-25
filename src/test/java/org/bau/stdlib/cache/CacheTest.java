package org.bau.stdlib.cache;

import java.util.Random;
import java.util.function.Function;

public class CacheTest {

    public static void main(String... args) {
        test((cacheSize) -> new ClockPro<>(cacheSize));
        test((cacheSize) -> new LRU<>(cacheSize));
        test((cacheSize) -> new Sieve<>(cacheSize));
    }

    static void test(Function<Integer, Cache<Integer, Long>> function) {
        int size = 1000;
        long start = System.nanoTime();
        long count = 0;
        String cacheClassName = "";
        for (int cacheSize = size / 10; cacheSize <= size; cacheSize += size / 10) {
            Cache<Integer, Long> baseCache = function.apply(cacheSize);
            cacheClassName = baseCache.getClass().getSimpleName();
            HitRateCacheWrapper cache = new HitRateCacheWrapper(baseCache);
            int[] counts = new int[size];
            // Gaussian
            // see also Zipf 's law: the relative probability of a request
            // for the i'th most popular page is proportional to 1/i.
            // https://courses.cs.washington.edu/courses/cse551/09sp/papers/breslau99.pdf
            for (int i = 0; i < size * 10; i++) {
                Random r = new Random(i);
                for (int j = 0; j < size; j++) {
                    int x = Math.min(size - 1, (int) Math.abs(r.nextGaussian() * size / 4));
                    cache.get(x);
                    counts[x]++;
                    count++;
                }

            }
            System.out.println(cacheClassName + " cacheSize " + cacheSize + " " + cache);
        }
        long time = System.nanoTime() - start;
        System.out.println(cacheClassName + " time " + (time / count) + " ns/call");
    }
}
