package org.bau.stdlib.graphics;

import static org.junit.Assert.fail;

import java.util.Arrays;

public class Font5x6 {
    private static final String FONT =
                  "20      21  x   22 x x  23 x x  24  x   25xx    26 x    27  x  \n"
               + "        !   x   \"  x x  # xxxxx $  xxxx % xx  x & x x   '   x  \n"
                + "            x              x x    x x        x     x           \n"
                + "            x              x x     xxxx     x     x x x        \n"
                + "                          xxxxx     x x    x xx   x  x         \n"
                + "            x              x x    xxxx    x  xx    xx x        \n"
                + "\n"
                + "\n"
                + "28  xx  29 xx   2a      2b      2c      2d      2e      2f    \n"
                + "(  xx   )   xx  *   x   +   x   ,       -       .       /     x\n"
                + "   x         x    x x x     x                                x \n"
                + "   x         x     xxx    xxxxx           xxxxx             x\n"
                + "   xx       xx    x x x     x       x              x       x\n"
                + "    xx     xx       x       x      x                      x\n"
                + "\n"
                + "\n"
                + "30 xxx  31  x   32xxxx  33xxxx  34x  x  35xxxxx 36 xxxx 37xxxxx\n"
                + "0 x   x 1  xx   2     x 3     x 4 x  x  5 x     6 x     7     x\n"
                + "  x   x     x      xxx     xxx    x  x    xxxx    xxxx       x\n"
                + "  x   x     x     x           x   xxxxx       x   x   x     x\n"
                + "  x   x     x     x           x      x        x   x   x     x\n"
                + "   xxx     xxx    xxxxx   xxxx       x    xxxx     xxx      x\n"
                + "\n"
                + "\n"
                + "38 xxx  39 xxx  3a      3b      3c      3d      3e      3f xxx \n"
                + "8 x   x 9 x   x :       ;       <   xx  =       >  xx   ? x   x\n"
                + "   xxx     xxxx     x       x      xx     xxxxx     xx       x \n"
                + "  x   x       x                   xx                 xx     x  \n"
                + "  x   x       x     x       x      xx     xxxxx     xx         \n"
                + "   xxx     xxx             x        xx             xx       x  \n"
                + "\n"
                + "\n"
                + "40 xxx  41 xxx  42xxxx  43 xxx  44xxxx  45xxxxx 46xxxxx 47 xxxx\n"
                + "@ x   x A x   x B x   x C x   x D x   x E x     F x     G x    \n"
                + "  x x x   xxxxx   xxxx    x       x   x   xxxx    xxxx    x xxx\n"
                + "  x xxx   x   x   x   x   x       x   x   x       x       x   x\n"
                + "  x       x   x   x   x   x   x   x   x   x       x       x   x\n"
                + "   xxx    x   x   xxxx     xxx    xxxx    xxxxx   x        xxxx\n"
                + "\n"
                + "\n"
                + "48x  x  49 xxx  4a  xxx 4bx  x  4cx     4dx   x 4ex   x 4f xxx   \n"
                + "H x  x  I   x   J     x K x x   L x     M xx xx N xx  x O x   x  \n"
                + "  xxxx      x         x   xx      x       x x x   x x x   x   x  \n"
                + "  x  x      x         x   x x     x       x   x   x  xx   x   x  \n"
                + "  x  x      x     x   x   x  x    x       x   x   x   x   x   x  \n"
                + "  x  x     xxx     xxx    x   x   xxxxx   x   x   x   x    xxx   \n"
                + "\n"
                + "\n"
                + "50xxxx  51 xxx  52xxxx  53 xxxx 54xxxxx 55x   x 56x   x 57x   x  \n"
                + "P x   x Q x   x R x   x S x     T   x   U x   x V x   x W x   x  \n"
                + "  xxxx    x   x   xxxx     xxx      x     x   x    x x    x   x  \n"
                + "  x       x x x   x x         x     x     x   x    x x    x x x  \n"
                + "  x       x  x    x  x        x     x     x   x     x      x x   \n"
                + "  x        xx x   x   x   xxxx      x      xxx      x      x x   \n"
                + "\n"
                + "\n"
                + "58x   x 59x   x 5axxxxx 5b  xx  5cx     5d xx   5e  x   5f       \n"
                + "X  x x  Y  x x  Z    x  [   x   \\  x    ]   x   ^  x x  _       \n"
                + "    x       x       x       x       x       x     x   x          \n"
                + "   x x      x      x        x       x       x                    \n"
                + "  x   x     x     x         x        x      x                    \n"
                + "  x   x     x     xxxxx     xx        x    xx             xxxxx  \n"
                + "\n"
                + "\n"
                + "60  x   61      62x     63      64    x 65      66  xx  67       \n"
                + "`    x  a  xxx  b x xx  c  xxxx d  xxxx e  xxx  f  x    g  xx x  \n"
                + "              x   xx  x   x       x   x   x   x    x      x  xx  \n"
                + "           xxxx   x   x   x       x   x   xxxxx   xxxx     xxxx  \n"
                + "          x   x   x   x   x       x   x   x        x          x  \n"
                + "           xxxx   xxxx     xxxx    xxxx    xxxx    x       xxx   \n"
                + "\n"
                + "\n"
                + "68x     69  x   6a  x   6bx     6c xx   6d      6e      6f       \n"
                + "h x xx  i       j       k x   x l   x   m xx x  n xxxx  o  xxx   \n"
                + "  xx  x    xx       x     x  x      x     x x x   x   x   x   x  \n"
                + "  x   x     x       x     x x       x     x x x   x   x   x   x  \n"
                + "  x   x     x       x     xx x      x     x x x   x   x   x   x  \n"
                + "  x   x    xxx    xxx     x   x     xx    x   x   x   x    xxx   \n"
                + "\n"
                + "\n"
                + "70      71      72      73      74 x    75      76      77       \n"
                + "p xxxx  q  xxxx r x xxx s  xxx  t xxxx  u x   x v x   x w x   x  \n"
                + "  x   x   x   x   xx      x        x      x   x   x   x   x x x  \n"
                + "  xx  x   x  xx   x        xxx     x      x   x    x x    x x x  \n"
                + "  x xx     xx x   x           x    x  x   x   x    x x    xx xx  \n"
                + "  x           x   x       xxxx      xx     xxxx     x      x x   \n"
                + "\n"
                + "\n"
                + "78      79      7a      7b   xx 7c  x   7dxx    7e      7f       \n"
                + "x x   x y x   x z xxxxx {   x   |   x   }   x   ~  x  x          \n"
                + "   x x    x   x      x      x       x       x     x x x          \n"
                + "    x      xxxx     x     xx        x        xx   x  x           \n"
                + "   x x        x    x        x       x       x                    \n"
                + "  x   x    xxx    xxxxx      xx     x     xx                     \n"
                + "\n"
                + "\n"
                + "80      81      82      83      84      85      86      87\n"
                + "\n"
                + "\n"
                + "\n"
                + "\n"
                + "\n"
                + "\n"
                + "\n"
                + "88      89      8a      8b      8c      8d      8e      8f\n"
                + "\n"
                + "\n"
                + "\n"
                + "\n"
                + "\n"
                + "\n"
                + "\n"
                + "90      91      92      93      94      95      96      97\n"
                + "\n"
                + "\n"
                + "\n"
                + "\n"
                + "\n"
                + "\n"
                + "\n"
                + "98      99      9a      9b      9c      9d      9e      9f\n"
                + "\n"
                + "\n"
                + "\n"
                + "\n"
                + "\n"
                + "\n"
                + "\n"
                + "a0      a1  x   a2  x   a3  xx  a4      a5x   x a6  x   a7  xxx  \n"
                + "        ¡       ¢  xxxx £  x    ¤ x   x ¥  x x  ¦   x   §   x    \n"
                + "            x     x x      x       xxx      x              x x   \n"
                + "            x     x x      xxx     x x    xxxxx     x      x x   \n"
                + "            x      xxxx    x       xxx      x       x       x    \n"
                + "            x       x     xxxxx   x   x     x       x      xx    \n"
                + "\n"
                + "\n"
                + "a8 x  x a9 xxx  aa xxxx ab      ac      ad      ae xxx  afxxxxx  \n"
                + "¨       © x x x ª x   x «   x x ¬               ® xxx x ¯         \n"
                + "          xx  x   x   x    x x    xxxxx           xx  x          \n"
                + "          x x x    xxxx   x x         x           xx  x          \n"
                + "           xxx             x x                     xxx           \n"
                + "                 xxxxxx     x x                                  \n"
                + "\n"
                + "\n"
                + "b0 xx   b1  x   b2  xx  b3 xxx  b4  x   b5      b6  xxx b7       \n"
                + "° x  x  ± xxxxx ²    x  ³   xx  ´  x    µ x  x  ¶  xxxx ·        \n"
                + "  x  x      x       x        x            x  x    xxx x     x    \n"
                + "   xx               xx     xxx            xxxx     xx x          \n"
                + "          xxxxx                           x         x x          \n"
                + "                                          x         x x          \n"
                + "\n"
                + "\n"
                + "b8      b9 xx   ba xx   bb      bcx   x bdx  x  bexx  x bf  x    \n"
                + "¸       ¹   x   º x  x  » x x   ¼ x  x  ½ x x   ¾ xx x  ¿        \n"
                + "            x      xx      x x      x      x      xxx       x    \n"
                + "                            x x    x xx   x xx     x xx    x     \n"
                + "                  xxxx     x x    x  xx  x  xx    x  xx   x   x  \n"
                + "                          x x         x    xxx        x    xxx   \n"
                + "\n"
                + "\n"
                + "c0xxx   c1  xxx c2  x   c3xx xx c4 x x  c5 xxx  c6  xxx c7 xxxx  \n"
                + "À       Á       Â  x x  Ã       Ä       Å  x x  Æ  xxx  Ç x      \n"
                + "   xxx     xxx     xxx     xxx     xxx     xxx     x x    x      \n"
                + "  x   x   x   x   x   x   x   x   x   x   x   x   xxxxx   x      \n"
                + "  xxxxx   xxxxx   xxxxx   xxxxx   xxxxx   xxxxx   x  x     xxxx  \n"
                + "  x   x   x   x   x   x   x   x   x   x   x   x   x  xx     xxx  \n"
                + "\n"
                + "\n"
                + "c8xxx   c9  xxx ca  x   cb x x  cc xx   cd  xx  ce  x   cf x x   \n"
                + "È       É       Ê  x x  Ë       Ì       Í       Î  x x  Ï        \n"
                + "  xxxxx   xxxxx   xxxxx   xxxxx    xxx     xxx     xxx     xxx   \n"
                + "  xxx     xxx     xxx     xxx       x       x       x       x    \n"
                + "  x       x       x       x         x       x       x       x    \n"
                + "  xxxxx   xxxxx   xxxxx   xxxxx    xxx     xxx     xxx     xxx   \n"
                + "\n"
                + "\n"
                + "d0 xxx  d1 xxx  d2xxx   d3   xxxd4  x   d5 xxx  d6 x x  d7       \n"
                + "Ð  x  x Ñ       Ò       Ó       Ô  x x  Õ       Ö       ×        \n"
                + "  xxx x   xxxx     xxx      xxx    xxx     xxx     xxx     x x   \n"
                + "   x  x   x   x   x   x    x   x  x   x   x   x   x   x     x    \n"
                + "   x  x   x   x   x   x    x   x  x   x   x   x   x   x    x x   \n"
                + "   xxx    x   x    xxx      xxx    xxx     xxx     xxx           \n"
                + "\n"
                + "\n"
                + "d8 xxxx d9xxx   da  xxx db xxx  dc x x  dd  xxx dex     df xx    \n"
                + "Ø x  xx Ù       Ú       Û       Ü       Ý       Þ x     ß x  x   \n"
                + "  x x x   x   x   x   x   x   x   x   x   x   x   xxx     x x    \n"
                + "  x x x   x   x   x   x   x   x   x   x    x x    x  x    x  x   \n"
                + "  xx  x   x   x   x   x   x   x   x   x     x     xxx     x  x   \n"
                + "  xxxx     xxx     xxx     xxx     xxx      x     x       x x    \n"
                + "\n"
                + "\n"
                + "e0xxx   e1  xxx e2  x   e3 xxxx e4 x x  e5  xx  e6      e7       \n"
                + "à       á       â  x x  ã       ä       å   xx  æ xxxx  ç  xxxx  \n"
                + "   xxxx    xxxx    xxxx    xxxx    xxxx    xxxx     x x   x      \n"
                + "  x   x   x   x   x   x   x   x   x   x   x   x    xxx    x      \n"
                + "  x   x   x   x   x   x   x   x   x   x   x   x   x x      xxxx  \n"
                + "   xxxx    xxxx    xxxx    xxxx    xxxx    xxxx    xxxx     xx   \n"
                + "\n"
                + "\n"
                + "e8xxx   e9  xxx ea  x   eb  x x ecxxx   ed  xxx ee  x   ef x x   \n"
                + "è       é       ê  x x  ë       ì       í       î  x x  ï        \n"
                + "   xxxx    xxxx    xxxx    xxxx    xxx     xxx     xxx     xxx   \n"
                + "  xx  x   xx  x   xx  x   xx  x     x       x       x       x    \n"
                + "  xxxxx   xxxxx   xxxxx   xxxxx     x       x       x       x    \n"
                + "   xxxx    xxxx    xxxx    xxxx    xxx     xxx     xxx     xxx   \n"
                + "\n"
                + "\n"
                + "f0xxxxx f1xxxxx f2xxx   f3  xxx f4  x   f5xxxxx f6 x x  f7       \n"
                + "ð       ñ       ò       ó       ô  x x  õ       ö       ÷   x    \n"
                + "   xxx    xxxx     xxx     xxx     xxx     xxx     xxx           \n"
                + "  x   x   x   x   x   x   x   x   x   x   x   x   x   x   xxxxx  \n"
                + "  x   x   x   x   x   x   x   x   x   x   x   x   x   x          \n"
                + "   xxx    x   x    xxx     xxx     xxx     xxx     xxx      x    \n"
                + "\n"
                + "\n"
                + "f8      f9xxx   fa  xxx fb xxx  fc x x  fd xxx  fex     ff x x   \n"
                + "ø     x ù       ú       û       ü       ý       þ x     ÿ        \n"
                + "   xxx    x   x   x   x   x   x   x   x   x   x   xxx     x   x  \n"
                + "  x x x   x   x   x   x   x   x   x   x    xxxx   x  x     xxxx  \n"
                + "   xxx    x   x   x   x   x   x   x   x       x   xxx         x  \n"
                + "  x        xxx     xxx     xxx     xxx     xxx    x        xxx   \n"
                + "\n"
                + "\n"
                + "\n"
              + "    \n"
              ;

    public static void mainGrid(String... args) {
        for (int i0 = 32; i0 < 256; i0 += 0x8) {
            for (int a = 0; a < 8; a++) {
                for (int i1 = 0; i1< 8; i1++) {
                    int i = i0 + i1;
                    if (a == 0) {
                        System.out.print(Integer.toHexString(0x100 | i).substring(1) + "      ");
                    } else if (a == 1) {
                        if (i < 0x7f || i > 0xa0) {
                            System.out.print((char) i + "       ");
                        } else {
                            System.out.print("        ");
                        }
                    } else {
                        System.out.print("        ");
                    }
                }
                System.out.println();
            }
        }
    }

    public static void main(String... args) {
        String[] lines = FONT.split("\n");
        int[] chars = new int[256-33];
        int li = 0;
        for (int i0 = 32; i0 < 256; i0 += 8) {
            System.out.println("char " + (char) i0 + " line " + li + "/" + lines.length);
            for (int a = 0; a < 8; a++) {
                String line = lines[li++];
                for (int i1 = 0; i1 < 8; i1++) {
                    int i = i0 + i1;
                    if (i > 255 || (i & 127) < 32) {
                        continue;
                    }
                    int slot = i - 33;
                    if (slot < 0) {
                        continue;
                    }
                    for(int p = 0; p < 8; p++) {
                        if (i1 * 8 + p >= line.length()) {
                            continue;
                        }
                        char c = line.charAt(i1 * 8 + p);
                        if (p == 0 && a == 1) {
                            if (i != 127 && i != 0xad && (i & 127) >= 33 && c != (char) i) {
                                fail("expected char code " + Integer.toHexString(i) + ": " + (char) i + " got " + c + ": " + Integer.toHexString(c));
                            }
                        }
                        if (p < 2) {
                            continue;
                        }
                        if (c != ' ' && c != 'x') {
                            fail("got " + (char) c);
                        }
                        if (c == 'x') {
                            chars[slot] |= 1 << (a * 5 + p - 2);
                        }
                    }
                }
            }
        }
        for(int c = 33; c < 256; c++) {
            int slot = c - 33;
            if (chars[slot] == 0) {
                continue;
            }
            int ch = chars[slot];
            for (int y = 0; y < 6; y++) {
                for (int x = 0; x < 5; x++) {
                    int b = 1 & (ch >>> (x + 5 * y));
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
