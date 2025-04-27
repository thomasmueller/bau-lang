package org.bau.stdlib.collections;

public class BitField {

    private final long[] data;

    public BitField(int len) {
        data = new long[(len + 63) / 64];
    }

    public void set(int index) {
        data[index >>> 6] |= 1L << (index & 0x3f);
    }

    public void flip(int index) {
        data[index >>> 6] ^= 1L << (index & 0x3f);
    }

    public void clear(int index) {
        data[index >>> 6] &= ~(1L << (index & 0x3f));
    }

    public boolean get(int index) {
        return (data[index >>> 6] >> (index & 0x3f) & 1) == 1;
    }

}
