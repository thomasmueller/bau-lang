package org.bau.stdlib.security;

public class ARC4 {

    private byte[] state = new byte[256];
    private int i = 0, j = 0;

    public ARC4(byte[] key) {
        for (int i = 0; i < 256; i++) {
            state[i] = (byte) i;
        }
        for (int i = 0, j = 0; i < 256; i++) {
            j = (j + state[i] + key[i % key.length]) & 0xff;
            swap(i, j);
        }
    }

    public byte[] encrypt(byte[] input) {
        byte[] output = new byte[input.length];
        for (int k = 0; k < input.length; k++) {
            output[k] = (byte) (nextByte() ^ input[k]);
        }
        return output;
    }

    public byte nextByte() {
        i = (i + 1) & 0xff;
        j = (j + state[i]) & 0xff;
        swap(i, j);
        int t = (state[i] + state[j]) & 0xff;
        return state[t];
    }

    private void swap(int i, int j) {
        byte temp = state[i];
        state[i] = state[j];
        state[j] = temp;
    }

}
