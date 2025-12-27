package org.bau.stdlib.pdf;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;

public class MeasureCharacterWidths {

    public static void main(String[] args) {

        String fontName = "Helvetica";
        int fontSize = 1000;

        String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        System.out.println("Available fonts");
        for (String f : fonts) {
            System.out.print(f + ";");
        }
        System.out.println();

        Font font = new Font(fontName, Font.PLAIN, fontSize);
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setFont(font);

        FontMetrics metrics = g2d.getFontMetrics();

        System.out.println("Font: " + font.getFontName() + " size " + fontSize);
        System.out.print("private static final int[] WIDTHS = { ");
        for (int codePoint = 32; codePoint <= 255; codePoint++) {
            if (codePoint > 32) {
                System.out.print(", ");
            }
            int width = metrics.charWidth((char) codePoint);
            System.out.print(width);
        }
        System.out.println(" };");
        g2d.dispose();
    }

}
