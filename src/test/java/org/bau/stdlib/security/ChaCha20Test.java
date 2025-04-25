package org.bau.stdlib.security;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.bau.stdlib.string.HexEncode;
import org.junit.Test;

public class ChaCha20Test {

    @Test
    public void test() {
        // https://datatracker.ietf.org/doc/html/rfc7539
        byte[] key = HexEncode.convertHexToBytes("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f");
        byte[] nonce = HexEncode.convertHexToBytes("000000000000004a00000000");
        String plain = "Ladies and Gentlemen of the class of '99: If I could offer you only one tip for the future, sunscreen would be it.";
        byte[] test = HexEncode.convertHexToBytes("6e2e359a2568f98041ba0728dd0d6981"
                + "e97e7aec1d4360c20a27afccfd9fae0b"
                + "f91b65c5524733ab8f593dabcd62b357"
                + "1639d624e65152ab8f530c359f0861d8"
                + "07ca0dbf500d6a6156a38e088a22b65e"
                + "52bc514d16ccf806818ce91ab7793736"
                + "5af90bbf74a35be6b40b8eedf2785e42"
                + "874d"
            );
        ChaCha20 cha = new ChaCha20(key, nonce);
        byte[] result = cha.encrypt(plain.getBytes());
        assertTrue(Arrays.equals(test, result));
    }

}
