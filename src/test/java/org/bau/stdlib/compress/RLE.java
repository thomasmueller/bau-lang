package org.bau.stdlib.compress;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public class RLE {

    public static byte[] encode(byte[] data, int minRun) {
        byte[] output = new byte[data.length * 2 + 1];
        int outPos = 0;
        for (int i = 0; i < data.length;) {
            int runLength = runLength(data, i, minRun, 128);
            if (runLength >= minRun) {
                output[outPos++] = (byte) (1 - runLength);
                output[outPos++] = data[i];
                i += runLength;
                continue;
            }
            int litStart = i;
            int litLen = 0;
            while (litLen < 128 && i < data.length) {
                if (runLength(data, i, minRun, 128) != 0) {
                    break;
                }
                i++;
                litLen++;
            }
            output[outPos++] = (byte) (litLen - 1);
            System.arraycopy(data, litStart, output, outPos, litLen);
            outPos += litLen;
        }
        // last byte is end marker
        output[outPos++] = -128;
        return Arrays.copyOf(output, outPos);
    }

    public static int runLength(byte[] data, int pos, int min, int max) {
        byte x = data[pos];
        if (pos + min >= data.length || x != data[pos + min]) {
            return 0;
        }
        int i = 1;
        for (; i < max; i++) {
            if (pos + i >= data.length || x != data[pos + i]) {
                break;
            }
        }
        if (i < min) {
            return 0;
        }
        return i;
    }

    public static byte[] decode(byte[] data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < data.length;) {
            int control = data[i++];
            if (control == -128) {
                break;
            }
            if (control >= 0) {
                for (int j = 0; j < control + 1; j++) {
                    out.write(data[i++]);
                }
            } else {
                byte value = data[i++];
                for (int j = 0; j < -control + 1; j++) {
                    out.write(value);
                }
            }
        }
        return out.toByteArray();
    }

}
