package org.bau.perf;

public class LinearProbing {

    long[] keys;
    long[] values;
    int size;
    
    public LinearProbing() {
        this(64);
    }
    
    LinearProbing(int size) {
        resize(size);
    }
    
    void resize(int size) {
        if(Integer.bitCount(size) != 1) {
            throw new IllegalArgumentException("not a power of 2");
        }
        keys = new long[size];
        values = new long[size];
    }
    
    void put(long key, long value) {
        int p = pos(key);
        while (true) {
            long k = keys[p];
            if (k == key) {
                keys[p] = key;
                values[p] = value;
                return;
            } else if (k == 0) {
                size++;
                keys[p] = key;
                values[p] = value;
                return;
            }
            p = (p + 1) & (keys.length - 1);
        }
    }

    long get(long key) {
        int p = pos(key);
        while (true) {
            long k = keys[p];
            if (k == key || k == 0) {
                return values[p];
            }
            p = (p + 1) & (keys.length - 1);
        }
    }
    
    int size() {
        return size;
    }

    boolean remove(long key) {
        int p = pos(key);
        while (true) {
            long k = keys[p];
            if (k == 0) {
                // not found
                return false;
            } else if (k == key) {
                keys[p] = 0;
                values[p] = 0;
                size--;
                break;
            }
            p = (p + 1) & (keys.length - 1);
        }
        int p2 = (p + 1) & (keys.length - 1);
        while (true) {
            long k = keys[p2];
            if (k == 0) {
                break;
            }
            int pb = pos(k);
            boolean canMove;
            if (pb <= p2) {
                // normal case (no wrap)
                //      pb.....p2
                canMove = (pb <= p) & (p2 >= p); 
            } else {
                // wrapped around
                // ....p2       pb.....
                canMove = pb <= p || p2 > p;
            }
            if (canMove) {
                keys[p] = keys[p2];
                values[p] = values[p2];
                keys[p2] = 0;
                values[p2] = 0;
                p = p2;
            }
            p2 = (p2 + 1) & (keys.length - 1);
        }
        return true;
    }

    int pos(long key) {
        return (int) (hash64(key) & (keys.length - 1));
    }
    
    public static long hash64(long x) {
        x = (x ^ (x >>> 33)) * 0xff51afd7ed558ccdL;
        x = (x ^ (x >>> 33)) * 0xc4ceb9fe1a85ec53L;
        x = x ^ (x >>> 33);
        return x;
    }
    
}
