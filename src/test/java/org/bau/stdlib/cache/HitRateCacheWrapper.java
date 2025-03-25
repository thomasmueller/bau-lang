package org.bau.stdlib.cache;

import java.util.concurrent.atomic.LongAdder;

public class HitRateCacheWrapper {

    private final Cache<Integer, Long> base;
    private final LongAdder misses = new LongAdder();
    private final LongAdder hits = new LongAdder();
    private final LongAdder gets = new LongAdder();

    HitRateCacheWrapper(Cache<Integer, Long> base) {
        this.base = base;
    }

    public Long get(Integer key) {
        gets.increment();
        Long result;
        result = base.get(key);
        if (result == null) {
            result = key * 10L;
            base.put(key, result);
            misses.increment();
        } else {
            hits.increment();
        }
        return result;
    }

    public void put(Integer key, Long value) {
        base.put(key, value);
    }

    @Override
    public String toString() {
        String result = "hits: " + hits + " misses: " + misses + " gets: " + gets + " " + base.toString();
        if (gets.longValue() == 0) {
            return "0% hit rate: " + result;
        }
        return (100 * hits.longValue() / gets.longValue()) + "% hit rate: " + result;
    }

}
