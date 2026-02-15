package org.bau.stdlib.qrcode;

import static org.junit.Assert.assertEquals;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.BitSet;
import java.util.Random;

import javax.imageio.ImageIO;

import org.junit.Test;

public class QrCodeTest {

    public static void main(String... args) throws IOException {
        for (int i = 20; i <= 2000; i *= 1.4) {
            String s = "the quick brown fox jumps over the lazy dog. ".repeat(1000).substring(0, i);
            for (int m = 2; m <= 2; m++) {
                QrCode qr = QrCode.encodeText(s, m);
                ImageIO.write(toImage(qr, 13, 1), "png", new File("qr" + i + "-mask" + m + ".png"));
            }
        }
    }

    @Test
    public void allSizes() {
        int errorCount = 0;
        // 1: 0; 5: 0; 8: 9/23320; 10: 37/23320; 100: 3959/23320; 1000: 23320/23320

        String all = "the quick brown fox jumps over the lazy dog. ".repeat(1000);
        for (int len = 0; len < 2332; len += 60) {
            // System.out.println("len " + len);
            for (int test = 0; test < 1; test++) {
                Random r = new Random(test);
                String s = all.substring(0, len);
                QrCode code = QrCode.encodeText(s, len & 7);
                BitSet set = new BitSet(code.getSize() * code.getSize());
                for (int i = 0; i < code.getSize() * code.getSize(); i++) {
                    set.set(i, code.getModule(i % code.getSize(), i / code.getSize()));
                }
                for (int i = 0; i < 20; i++) {
                    int index = r.nextInt(set.length());
                    set.flip(index);
                }
                String got;
                try {
                    got = QrCode.decodeText((BitSet) set.clone(), code.getSize());
                    assertEquals(s, got);
                } catch (IllegalArgumentException e) {
                    errorCount++;
                }
            }
        }
        assertEquals(1, errorCount);
        // System.out.println("error count " + errorCount);
    }

	private static BufferedImage toImage(QrCode qr, int scale, int border) {
		BufferedImage result = new BufferedImage((qr.getSize() + 2 * border) * scale, (qr.getSize() + 2 * border) * scale, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < result.getWidth(); x++) {
            for (int y = 0; y < result.getHeight(); y++) {
				boolean color = qr.getModule(x / scale - border, y / scale - border);
				result.setRGB(x, y, color ? 0 : 0xffffff);
			}
		}
		return result;
	}

}
