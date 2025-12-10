package org.bau.stdlib.graphics;

import static org.junit.Assert.fail;

import java.util.Arrays;

// https://alasseearfalas.itch.io/another-tiny-pixel-font-mono-3x5
// https://www.lexaloffle.com/pico-8.php?page=faq

public class Font3x5 {
    private static final String FONT =
                "20    21 x  22x x 23x x 24xxx 25x x 26xx  27 x \n"
             + "      !  x  \" x x # xxx $ xx  %   x & xx  '  x\n"
              + "         x          x x    xx    x     xx      \n"
              + "                    xxx   xxx   x     x x      \n"
              + "         x          x x    x    x x   xxx      \n"
              + "\n"
              + "28 x  29 x  2a    2b    2c    2d    2e    2f  x\n"
              + "( x   )   x * x x +  x  ,     -     .     /  x \n"
              + "  x       x    x    xxx         xxx          x \n"
              + "  x       x   x x    x     x                 x \n"
              + "   x     x                x            x    x  \n"
              + "\n"
              + "30xxx 31 x  32xxx 33xxx 34x x 35xxx 36x   37xxx\n"
              + "0 x x 1 xx  2   x 3   x 4 x x 5 x   6 x   7   x\n"
              + "  x x    x    xxx    xx   xxx   xxx   xxx     x\n"
              + "  x x    x    x       x     x     x   x x     x\n"
              + "  xxx    x    xxx   xxx     x   xxx   xxx     x\n"
              + "\n"
              + "38xxx 39xxx 3a    3b    3c  x 3d    3ex   3fxxx\n"
              + "8 x x 9 x x :  x  ;  x  <  x  = xxx >  x  ?   x\n"
              + "  xxx   xxx               x             x    xx\n"
              + "  x x     x    x     x     x    xxx    x       \n"
              + "  xxx   xxx         x       x         x      x \n"
              + "\n"
              + "40 x  41 xx 42xx  43 xx 44xx  45xxx 46xxx 47 xx\n"
              + "@ x x A x x B x x C x   D x x E x   F x   G x  \n"
              + "  x x   xxx   xx    x     x x   xx    xx    x x\n"
              + "  x     x x   x x   x     x x   x     x     x x\n"
              + "   xx   x x   xxx    xx   xx    xxx   x     xxx\n"
              + "\n"
              + "48x x 49xxx 4a  x 4bx x 4cx   4dx x 4exx  4f xx\n"
              + "H x x I  x  J   x K x x L x   M xxx N x x O x x\n"
              + "  xxx    x      x   xx    x     xxx   x x   x x\n"
              + "  x x    x    x x   x x   x     x x   x x   x x\n"
              + "  x x   xxx   xxx   x x   xxx   x x   x x   xx\n"
              + "\n"
              + "50xxx 51 x  52xxx 53 xx 54xxx 55x x 56x x 57x x\n"
              + "P x x Q x x R x x S x   T  x  U x x V x x W x x\n"
              + "  xxx   x x   xx    xxx    x    x x   x x   xxx\n"
              + "  x     xx    x x     x    x    x x    x    xxx\n"
              + "  x      xx   x x   xx     x     xx    x    xxx\n"
              + "\n"
              + "58x x 59x x 5axxx 5b xx 5cx   5dxx  5e x  5f   \n"
              + "X  x  Y x x Z   x [  x  \\ x   ]  x  ^ x x _   \n"
              + "   x    xxx    x     x     x     x             \n"
              + "   x      x   x      x      x    x             \n"
              + "  x x   xxx   xxx    xx     x   xx          xxx\n"
              + "\n"
              + "60 x  61    62    63    64    65    66    67   \n"
              + "`   x a  xx b xx  c  xx d xx  e xxx f xxx g  xx\n"
              + "        x x   xx    x     x x   xx    xx    x  \n"
              + "        xxx   x x   x     x x   x     x     x x\n"
              + "        x x   xx     xx   xx     xx   x     xxx\n"
              + "\n"
              + "68    69    6a    6b    6c    6d    6e    6f   \n"
              + "h x x i xxx j  x  k x x l x   m xxx n xx  o  xx\n"
              + "  x x    x     x    xx    x     xxx   x x   x x\n"
              + "  xxx    x   x x    x x   x     x x   x x   x x\n"
              + "  x x   xxx  xxx    x x    xx   x x   x x   xx \n"
              + "\n"
              + "70    71    72    73    74    75    76    77\n"
              + "p  xx q  x  r xx  s  xx t xxx u x x v x x w x x\n"
              + "  x x   x x   x x   x      x    x x   x x   xxx\n"
              + "  xxx   xx    xx      x    x    x x    x    xxx\n"
              + "  x      xx   x x   xx     x     xx    x    xxx\n"
              + "\n"
              + "78    79    7a    7b xx 7c x  7dxx  7e    7f \n"
              + "x x x y x x z xxx {  x  |  x  }  x  ~ xx     \n"
              + "   x    xxx     x   x      x      x    xx    \n"
              + "   x      x   x      x     x     x           \n"
              + "  x x    x    xxx    xx    x    xx           \n"
              + "    \n"
              ;

    public static void mainGrid(String... args) {
        for (int i0 = 32; i0 < 128; i0 += 0x8) {
            for (int a = 0; a < 6; a++) {
                for (int i1 = 0; i1< 0x8; i1++) {
                    int i = i0 + i1;
                    if (a == 0) {
                        System.out.print(Integer.toHexString(0x100 | i).substring(1) + "    ");
                    } else if (a == 1) {
                        if (i < 0x7f || i > 0xa0) {
                            System.out.print((char) i + "     ");
                        } else {
                            System.out.print("      ");
                        }
                    } else {
                        System.out.print("      ");
                    }
                }
                System.out.println();
            }
        }
    }

    public static void main(String... args) {
        String[] lines = FONT.split("\n");
        int[] chars = new int[94];
        int li = 0;
        for (int i0 = 32; i0 < 128; i0 += 8) {
            System.out.println("char " + (char) i0 + " line " + li + "/" + lines.length);
            for (int a = 0; a < 6; a++) {
                String line = lines[li++];
                for (int i1 = 0; i1 < 0x8; i1++) {
                    int i = i0 + i1;
                    if (i > 255 || (i & 127) < 32) {
                        continue;
                    }
                    int slot = i - 33;
                    if (slot < 0) {
                        continue;
                    }
                    for(int p = 0; p < 6; p++) {
                        if (i1 * 6 + p >= line.length()) {
                            continue;
                        }
                        char c = line.charAt(i1 * 6 + p);
                        if (p == 0 && a == 1) {
                            if (i != 127 && c != (char) i) {
                                fail("expected char code " + Integer.toHexString(i) + ": " + (char) i + " got " + c + ": " + Integer.toHexString(c));
                            }
                        }
                        if (p < 2) {
                            continue;
                        }
                        if (c != ' ' && c != 'x') {
                            fail();
                        }
                        if (c == 'x') {
                            chars[slot] |= 1 << (a * 3 + p - 2);
                        }
                    }
                }
            }
        }
        for (int c = 33; c < 127; c++) {
            int slot = c - 33;
            if (chars[slot] == 0) {
                continue;
            }
            int ch = chars[slot];
            for (int y = 0; y < 5; y++) {
                for (int x = 0; x < 3; x++) {
                    int b = 1 & (ch >>> (x + 3 * y));
                    System.out.print(b == 1 ? "#" : " ");
                }
                System.out.println();
            }
        }
        for (int i = 0; i < chars.length; i++) {
            for (int j = 0; j < chars.length; j++) {
                if (i == j) {
                    continue;
                }
                if (chars[i] == chars[j]) {
                    System.out.println("?? a=" + (char) (i + 33) + "<->" + (char) (j + 33));
                }
                int xor = chars[i] ^ chars[j];
                int notEqualBitsCount = Integer.bitCount(xor);
                if (notEqualBitsCount < 2) {
                    System.out.println("bitDifference=" +
                            notEqualBitsCount + " for " + (char) (i + 33) + "<->" + (char) (j + 33));
                }
            }
        }
        System.out.println(Arrays.toString(chars));

    }

}
