package org.bau.stdlib.io;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Random;
import java.util.zip.Adler32;
import java.util.zip.CRC32;

import org.junit.Test;

public class ChecksumTest {

    @Test
    public void random() {
        Random r = new Random(1);
        for (int i = 0; i < 10_000; i++) {
            byte[] data = new byte[r.nextInt(100)];
            long got = Checksum.crc32(data);
            CRC32 crc = new CRC32();
            crc.update(data);
            long value = crc.getValue();
            assertEquals(value, got);

            Adler32 adler = new Adler32();
            adler.update(data);
            value = adler.getValue();
            got = Checksum.adler32(data);
            assertEquals(value, got);
        }
    }

    @Test
    public void test() {
        byte[] ones = new byte[1_000_000];
        Arrays.fill(ones, (byte) 1);
        assertEquals(233063202, Checksum.adler32(ones));
        assertEquals(1823800358, Checksum.crc32(ones));
        assertEquals(-757937976, Checksum.fletcher32(ones));
        byte[] zeros = new byte[1_000_000];
        assertEquals(1126236161, Checksum.adler32(zeros));
        assertEquals(309971870, Checksum.crc32(zeros));
        assertEquals(-1, Checksum.fletcher32(zeros));
    }

}
