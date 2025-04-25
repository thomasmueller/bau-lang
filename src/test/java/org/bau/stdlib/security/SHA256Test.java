package org.bau.stdlib.security;

import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;

import org.bau.stdlib.string.HexEncode;
import org.junit.Test;

public class SHA256Test {

    @Test
    public void test() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                HexEncode.convertBytesToHex(SHA256.getHash("abc".getBytes(StandardCharsets.UTF_8))));
        assertEquals("cdc76e5c9914fb9281a1c7e284d73e67f1809a48a497200e046d39ccc7112cd0",
                HexEncode.convertBytesToHex(SHA256.getHash("a".repeat(1000000).getBytes(StandardCharsets.UTF_8))));
    }
}
