package org.bau.stdlib.security;

public class ChaCha12 {

    private int[] state = new int[16];
    private int[] work = new int[16];
    private int counter;

    ChaCha12(byte[] key, byte[] nonce) {
        if (key.length != 32 || nonce.length != 12) {
            throw new IllegalArgumentException();
        }
        state[0] = 0x61707865;
        state[1] = 0x3320646e;
        state[2] = 0x79622d32;
        state[3] = 0x6b206574;
        for (int i = 0; i < 8; i++) {
            state[i + 4] = (key[i * 4] & 0xff)
                    | ((key[i * 4 + 1] & 0xff) << 8)
                    | ((key[i * 4 + 2] & 0xff) << 16)
                    | ((key[i * 4 + 3] & 0xff) << 24);
        }
        for (int i = 0; i < 3; i++) {
            state[i + 13] = (nonce[i * 4] & 0xff)
                    | ((nonce[i * 4 + 1] & 0xff) << 8)
                    | ((nonce[i * 4 + 2] & 0xff) << 16)
                    | ((nonce[i * 4 + 3] & 0xff) << 24);
        }
    }

    public byte[] encrypt(byte[] plain) {
        byte[] result = new byte[plain.length];
        byte[] target = new byte[64];
        for (int i = 0; i < plain.length; i += 64) {
            next(target);
            for (int j = 0; j < 64 && i + j < plain.length; j++) {
                result[i + j] = (byte) (plain[i + j] ^ target[j]);
            }
        }
        return result;
    }

    public void next(byte[] target) {
        counter++;
        state[12] = counter;
        System.arraycopy(state, 0, work, 0, 16);
        int[] x = work;
        block(x);
        for (int i = 0; i < 16; i++) {
            x[i] += state[i];
        }
        for (int i = 0; i < 64; i++) {
            target[i] = (byte) (x[i / 4] >>> ((i % 4) * 8));
        }
    }

    private void block(int[] x) {
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
