package org.bau.stdlib.graphics;

public class VectorFonts {

    public static void main(String... args) {

        StringBuilder buff = new StringBuilder();
        for (int i = 32; i < 256; i++) {
            if (i > 0x7f && i < 0xa0) {
                continue;
            }
            if (i > 32 && i % 16 == 0) {
                buff.append("|\n");
            }
            char c = (char) i;
            if (c == 0x7f || c == 0xad) {
                c = ' ';
            }
            buff.append("|").append(c);
        }
        buff.append("|");
        System.out.println(buff.toString());
    }


}
