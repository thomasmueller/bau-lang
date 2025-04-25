package org.bau.stdlib.security;

import static org.junit.Assert.assertEquals;

import org.bau.stdlib.string.HexEncode;
import org.junit.Test;

public class ARC4Test {
    @Test
    public void test() {
        ARC4 arc4 = new ARC4("Key".getBytes());
        byte[] ciphertext = arc4.encrypt("Plaintext".getBytes());
        assertEquals("bbf316e8d940af0ad3", HexEncode.convertBytesToHex(ciphertext));

        arc4 = new ARC4("Wiki".getBytes());
        ciphertext = arc4.encrypt("pedia".getBytes());
        assertEquals("1021bf0420", HexEncode.convertBytesToHex(ciphertext));

        arc4 = new ARC4("Secret".getBytes());
        ciphertext = arc4.encrypt("Attack at dawn".getBytes());
        assertEquals("45a01f645fc35b383552544b9bf5", HexEncode.convertBytesToHex(ciphertext));

        arc4 = new ARC4("Secret".getBytes());
        assertEquals("Attack at dawn", new String(arc4.encrypt(HexEncode.convertHexToBytes("45a01f645fc35b383552544b9bf5"))));
    }
}
