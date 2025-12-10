package org.bau.stdlib.graphics;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.junit.Test;

public class ImageTest {

    @Test
    public void test() throws IOException {
        int w = 360 * 2;
        int h = 288 * 2;
        Image img = new Image(w, h);
        String fox = "the quick brown fox jumps over the lazy dog äöüéàè";
//        img.drawText3x5(0, 10, 5, "Hello world 123!", 2);
        img.drawText5x6(0, 10, 12, "Hello world 123!", 2);
//        img.drawText3x5(0, 10, 20, fox, 2);
        img.drawText5x6(0, 10, 26, fox, 2);
//        img.drawText3x5(0, 10, 40, fox.toUpperCase(Locale.ROOT), 2);
        img.drawText5x6(0, 10, 46, fox.toUpperCase(Locale.ROOT), 2);
//        img.drawText3x5(0, 10, 70, "0123456789 +\"'*ç%&/()=?_:;-.,", 2);
        img.drawText5x6(0, 10, 80, "0123456789 +\"'*ç%&/()=?_:;-.,", 2);
        StringBuilder buff = new StringBuilder();
        int rh = 16 * img.getTextHeight(fox, 2);
        img.fillRectangle(128, 10, 100, 10 + img.getTextLength(fox, 2), 100 + rh);
        img.drawLine(0, 10 + img.getTextLength(fox, 2), 100, 10, 100 + rh);
        img.drawLineAntialiased(0, 128, 10, 100, 10 + img.getTextLength(fox, 2), 100 + rh);
        img.drawCircle(190, 50, 140, 30, true);
        img.drawCircle(0, 50, 140, 30, false);
        img.drawCircle(190, 350, 340, 31, true);
        img.drawCircle(0, 350, 340, 30, false);
        for (int i = 0; i < 256; i++) {
            buff.append((char) i);
        }
        String s = buff.toString();
        int y = 320;
        for (int i = 32; i < 256; i += 32) {
            img.drawText5x6(0, 10, y, s.substring(i, i + 32), 2);
            y += 20;
        }
        File out = new File("target/test.tiff");
        Tiff.writeGrayscaleTiff(img.imageData, w, h, out);
    }
}
