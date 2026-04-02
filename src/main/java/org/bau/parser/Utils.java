package org.bau.parser;

public class Utils {

    public static void assertTrue(boolean b) {
        if (!b) {
            throw new IllegalStateException();
        }
    }

}
