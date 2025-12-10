package org.bau.stdlib.graphics;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

public class TiffTest {

    @Test
    public void simple() throws IOException {
        int w = 8;
        int h = 8;
        byte[] imageData = new byte[w * h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                imageData[x + y * w] = (byte) (100-y*4);
            }
        }
        File out = new File("target/test.tiff");
        Tiff.writeGrayscaleTiff(imageData, w, h, out);
        assertEquals("byte order 4d4d\n"
                + "magic 2a\n"
                + "offset 8\n"
                + "tags 11\n"
                + "[a]: tag 100 ImageWidth 4 1 8\n"
                + "[16]: tag 101 ImageLength 4 1 8\n"
                + "[22]: tag 102 BitsPerSample 3 1 80000\n"
                + "[2e]: tag 103 Compression 3 1 ffffffff80050000\n"
                + "[3a]: tag 106 PhotometricInterpretation 3 1 10000\n"
                + "[46]: tag 111 StripOffsets 4 1 9a\n"
                + "[52]: tag 116 RowsPerStrip 4 1 8\n"
                + "[5e]: tag 117 StripByteCounts 4 1 10\n"
                + "[6a]: tag 11a XResolution 5 1 92\n"
                + "[76]: tag 11b YResolution 5 1 92\n"
                + "[82]: tag 128 ResolutionUnit 3 1 20000\n"
                + "\n"
                + "[8e]:  00 00\n"
                + "[90]:  00 00 00 00 00 48 00 00 00 01 f9 64 f9 60 f9 5c\n"
                + "[a0]:  f9 58", Tiff.readTiff(out));
    }
}
