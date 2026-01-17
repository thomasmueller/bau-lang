package org.bau.stdlib.compress;

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

public class RLETest {

    public static void main(String... args) {

    }

    @Test
    public void test() {
        Random r = new Random(1);
        for (int i = 0; i < 100; i++) {
            int len = r.nextInt(100);
            byte[] data = new byte[len];
            int minRun = 3 + r.nextInt(10);
            for (int j = 0; j < len; j++) {
                data[j] = (byte) r.nextInt(3);
            }
            byte[] compressed = RLE.encode(data, minRun);
            byte[] verify = RLE.decode(compressed);
            if (!Arrays.equals(data, verify)) {
                fail("\n" + Arrays.toString(data) + " <-> \n" + Arrays.toString(verify));
            }
        }
    }

}
