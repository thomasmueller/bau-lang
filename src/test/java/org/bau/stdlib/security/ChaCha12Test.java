package org.bau.stdlib.security;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.bau.stdlib.string.HexEncode;
import org.junit.Test;

public class ChaCha12Test {

    @Test
    public void test() {
        byte[] key = HexEncode.convertHexToBytes("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f");
        byte[] nonce = HexEncode.convertHexToBytes("000000000000004a00000000");
        String plain = "Ladies and Gentlemen of the class of '99: If I could offer you only one tip for the future, sunscreen would be it.";
        byte[] test = HexEncode.convertHexToBytes("8d47e256f00475f2661d4fbf7f2a1137"
                + "b63f066215d22dccbfc52e4fbe1701fc"
                + "f8885f7a1a39b63f797754d801111d3c"
                + "0d5f0c9012717425ddf867ef5f1ab14d"
                + "7f01852a87965ee3d8727d8c7f09d5bf"
                + "68a8fa8dc0cac74e88cf26b1729099d7"
                + "37b4ecabba683522483ff77e62b65e39"
                + "ca58");
        ChaCha12 cha = new ChaCha12(key, nonce);
        byte[] result = cha.encrypt(plain.getBytes());
        System.out.println(HexEncode.convertBytesToHex(result));
        assertTrue(Arrays.equals(test, result));
    }

}
