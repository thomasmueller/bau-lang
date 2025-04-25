package org.bau.stdlib.security;

import java.security.SecureRandom;

public class ChaCha12Random {

    private final int[] buffer = new int[64];
    private final int[] state = new int[16];
    private final int[] work = new int[16];
    private int counter;
    private int index = buffer.length;

    public static ChaCha12Random createDefault() {
        return new ChaCha12Random(new SecureRandom());
    }

    private ChaCha12Random(SecureRandom seed) {
        state[0] = 0x61707865;
        state[1] = 0x3320646e;
        state[2] = 0x79622d32;
        state[3] = 0x6b206574;
        for (int i = 0; i < 8; i++) {
            state[i + 4] = seed.nextInt();
        }
        for (int i = 0; i < 3; i++) {
            state[i + 13] = seed.nextInt();
        }
    }

    public int nextInt() {
        if (index >= buffer.length) {
            next(buffer);
            index = 0;
        }
        return buffer[index++];
    }

    public long nextLong() {
        return ((long) nextInt() << 32) | nextInt();
    }

    public void next(int[] target) {
        counter++;
        state[12] = counter;
        System.arraycopy(state, 0, work, 0, 16);
        int[] x = work;
        for (int i = 0; i < 6; i++) {
            quarterRound(x, 0, 4, 8, 12);
            quarterRound(x, 1, 5, 9, 13);
            quarterRound(x, 2, 6, 10, 14);
            quarterRound(x, 3, 7, 11, 15);
            quarterRound(x, 0, 5, 10, 15);
            quarterRound(x, 1, 6, 11, 12);
            quarterRound(x, 2, 7, 8, 13);
            quarterRound(x, 3, 4, 9, 14);
        }
        for (int i = 0; i < 16; i++) {
            x[i] += state[i];
            target[i] = x[i];
        }
    }

    private void quarterRound(int[] x, int a, int b, int c, int d) {
        x[a] += x[b];
        x[d] ^= x[a];
        x[d] = Integer.rotateLeft(x[d], 16);
        x[c] += x[d];
        x[b] ^= x[c];
        x[b] = Integer.rotateLeft(x[b], 12);
        x[a] += x[b];
        x[d] ^= x[a];
        x[d] = Integer.rotateLeft(x[d], 8);
        x[c] += x[d];
        x[b] ^= x[c];
        x[b] = Integer.rotateLeft(x[b], 7);
    }

}
