package org.bau.perf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Random;

import org.junit.Test;

public class HashTest {
    
    private static Random random = new Random(1);
    
    @Test
    public void primeTest() {
        // https://github.com/OpenHFT/Zero-Allocation-Hashing/blob/ea/src/main/java/net/openhft/hashing/XXH3.java
        // XXH_PRIME64_1 = 0x9E3779B185EBCA87L
        assertTrue(new BigInteger("11400714785074694791").isProbablePrime(100));
    }

    static long javaStringHash(byte[] data) {
        long h = 1;
        for (int i = 0; i < data.length; i++) {
            h = 31 * h + (data[i] & 0xff);
        }
        return h;
    }
    
    static long javaStringHash4(byte[] data) {
        // https://lemire.me/blog/2015/10/22/faster-hashing-without-effort/
        long h = 1;
        int i = 0;
        for (; i < data.length - 3; i += 4) {
            h = 31 * 31 * 31 * 31 * h + 
                    31 * 31 * 31 * (data[i] & 0xff) + 
                    31 * 31 * (data[i + 1] & 0xff) + 
                    31 * (data[i + 2] & 0xff) + 
                    (data[i + 3] & 0xff);
        }
        for (; i < data.length; i++) {
            h = 31 * h + (data[i] & 0xff);
        }
        return h;
    }
        
    static int murmur3_32(byte[] data) {
        // MurmurHash3_x86_32
        // https://github.com/aappleby/smhasher/blob/master/src/MurmurHash3.cpp
        // https://murmurhash.shorelabs.com/
        if (data.length <= 0)
            return 0;
        int c1 = 0xcc9e2d51;
        int c2 = 0x1b873593;
        int h = 0;
        int i = 0;
        if (i < data.length - 3) {
            while (true) {
                int x = ((data[i] & 0xff) | ((data[i + 1] & 0xff) << 8) | ((data[i + 2] & 0xff) << 16)
                        | ((data[i + 3] & 0xff) << 24));
                x *= c1;
                x = Integer.rotateLeft(x, 15);
                x *= c2;
                h ^= x;
                h = Integer.rotateLeft(h, 13);
                h = h * 5 + 0xe6546b64;
                int n = i + 4;
                if (n > data.length - 4)
                    break;
                i = n;
            }
        }
        int x = 0;
        if (i < data.length - 4) {
            i += 4;
        }
        if (i < data.length && i != data.length - 4) {
            int s = 0;
            while (true) {
                x ^= (data[i] & 0xff) << s;
                int n = i + 1;
                if (n >= data.length)
                    break;
                i = n;
                s += 8;
            }
            x *= c1;
            x = Integer.rotateLeft(x, 15);
            x *= c2;
            h ^= x;
        }
        h ^= data.length;
        h ^= h >>> 16;
        h *= 0x85ebca6b;
        h ^= h >>> 13;
        h *= 0xc2b2ae35;
        h ^= h >>> 16;
        return h;
    }
    
    @Test
    public void testCollisions() {
        Random r = new Random(1);
        for (int shift = 0; shift < 64 - 8; shift += 4) {
            for (int len = 2; len <= 1 * 1024; len *= 2) {
                // System.out.println("shift " + shift + " len " + len);
                byte[] data = new byte[len];
                long seed = r.nextLong();
                int expected = 0;
                for (int type = 1; type <= 1; type++) {
                    HashSet<Long> set = new HashSet<>();
                    Random r2 = new Random(seed);
                    r2.nextBytes(data);
                    long x = hash(type, data);
                    set.add((x >>> shift) & (len - 1));
                    int count = 0;
                    int flipMin = 128;
                    int flipMax = 0;
                    long flipSum = 0;
                    for (int j = 1; j < 256; j *= 2) {
                        for (int i = 0; i < data.length; i++) {
                            data[i] ^= j;
                            long y = hash(type, data);
                            set.add((y >>> shift) & (len - 1));
                            int diff = Long.bitCount(x ^ y);
                            flipMin = Math.min(flipMin, diff);
                            flipMax = Math.max(flipMax, diff);
                            flipSum += diff;
                            count++;
                            data[i] ^= j;
                        }
                    }
                    int collisions = (count - set.size() - 7 * len);
                    if (type == 0) {
                        expected = collisions;
                    } else {
                        if (collisions > expected + 1) {
                            System.out
                                    .println("shift " + shift + " type " + type + " len " + len + " collisions " + collisions + " expected " + expected);
                        }
                    }
                    long flipAvg = flipSum / count;
                    if (flipMin < 10 || flipMax > 54 || Math.abs(flipAvg - 32) > 4) {
                        System.out
                        .println("shift " + shift + " type " + type + " len " + len + " flip min " + flipMin + " max " + flipMax + " avg " + flipAvg);
                    }
                }
            }
        }
    }
    
    static long hash(int type, byte[] data) {
        switch (type) {
        case 0:
            return random.nextLong();
        case 1:
            return polyHash4(data);
        case 2:
            return polyHash8(data);
        case 3:
            return javaStringHash(data);
        case 4:
            return murmur3_32(data);
        case 5:
            return sha256(data);
        }
        throw new IllegalArgumentException();
    }
    
    private static long sha256(byte[] data) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(data);
            BigInteger number = new BigInteger(1, hashBytes);
            return number.longValue();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void murmurTest() {
        assertEquals(0, murmur3_32("".getBytes(StandardCharsets.UTF_8)));
        assertEquals(1009084850, murmur3_32("a".getBytes(StandardCharsets.UTF_8)));
        assertEquals(0xba6bd213, murmur3_32("test".getBytes(StandardCharsets.UTF_8)));
        assertEquals(0xc0363e43, murmur3_32("Hello, world!".getBytes(StandardCharsets.UTF_8)));
    }
    
    // polynomial hash function
    // (similar to Java String hash, but 4 bytes at a time)
    static long polyHash4(byte[] data) {
        long h = ((long) data.length << 32);
        int i = 0;
        for (; i < data.length - 3; i += 4) {
            int x = ((data[i] & 0xff) | 
                    ((data[i + 1] & 0xff) << 8) | 
                    ((data[i + 2] & 0xff) << 16) | 
                    ((data[i + 3] & 0xff) << 24));
            h = (h + x) * 0x9E3779B185EBCA87L;
        }
        if (i < data.length) {
            int x = 0;
            for (int s = 0; i < data.length; i++, s += 8) {
                x ^= (data[i] & 0xff) << s;
            }
            h = (h + x) * 0x9E3779B185EBCA87L;
        }
        return h ^ (h >>> 32);
    }

    // polynomial hash function
    // (similar to Java String hash, but 8 bytes at a time)
    static long polyHash8(byte[] data) {
        long h = data.length;
        int i = 0;
        for (; i < data.length - 7; i += 8) {
            long x = ((data[i] & 0xffL) | 
                    ((data[i + 1] & 0xffL) << 8) | 
                    ((data[i + 2] & 0xffL) << 16) | 
                    ((data[i + 3] & 0xffL) << 24) |
                    ((data[i + 4] & 0xffL) << 32) |
                    ((data[i + 5] & 0xffL) << 40) |
                    ((data[i + 6] & 0xffL) << 48) |
                    ((data[i + 7] & 0xffL) << 56));
            h = (h + x) * 0x9E3779B185EBCA87L;
        }
        if (i < data.length) { 
            long x = 0;
            for (int s = 0; i < data.length; i++, s += 8) {
                x += ((data[i] & 0xffL)) << s;
            }
            h = (h + x) * 0x9E3779B185EBCA87L;
        }
        h = ((h ^ (h >>> 32))) * 0x9E3779B185EBCA87L;
        return h ^ (h >>> 32);
    }      
    
//    @Test
//    public void testPerf10() {
//        testPerf(10, 100_000_000);
//    }
//
//    @Test
//    public void testPerf100() {
//        testPerf(100, 10_000_000);
//    }
//
//    @Test
//    public void testPerfLarge() {
//        testPerf(1024 * 1024, 1000);
//    }
//
//    @Test
//    public void testPerfGrow() {
//        for(int size = 1; size < 32; size++) {
//            testPerf(size, 100_000_000 / size);
//        }
//    }
    
    public void testPerf(int size, int count) {
        byte[] data = new byte[size];
        for (int test = 0; test < 5; test++) {
            long time = System.currentTimeMillis();
            long sum = 0;
            for (int i = 0; i < count; i++) {
                data[0] = (byte) (sum & 0xff);
                sum += polyHash4(data);
            }
            time = System.currentTimeMillis() - time;
            if (test == 4) {
                System.out.println("size " + size + " ms " + time + " dummy " + sum);
            }
        }
    }
    
    /*

javaStringHash
size 4 ms 151 dummy 190662390437089
size 8 ms 126 dummy -4193367566511493919
size 16 ms 109 dummy -8536805194956420127
size 32 ms 101 dummy 6713922491977298401
size 64 ms 99 dummy 7397971098742043105
size 128 ms 96 dummy 1804911153469238241

javaStringHash4
size 4 ms 137 dummy 190662390437089
size 8 ms 94 dummy -4193367566511493919
size 16 ms 77 dummy -8536805194956420127
size 32 ms 66 dummy 6713922491977298401
size 64 ms 60 dummy 7397971098742043105
size 128 ms 59 dummy 1804911153469238241

polyHash4
size 4 ms 145 dummy 8940020968246236983
size 8 ms 88 dummy -5468316856230457827
size 16 ms 61 dummy -4258304169352728375
size 32 ms 46 dummy 8327214201052883638
size 64 ms 39 dummy 1200268354525726071
size 128 ms 35 dummy -276199946549732800

size 1 ms 579 dummy 5278077792660970462
size 2 ms 290 dummy -6973678418234011378
size 3 ms 210 dummy 3400461022875929242
size 4 ms 144 dummy 8940020968246236983
size 5 ms 143 dummy 1580507791054418273
size 6 ms 117 dummy 4132035839393732011
size 7 ms 102 dummy 2215264861669130565
size 8 ms 89 dummy -5468316856230457827
size 9 ms 94 dummy 6671483284882256325
size 10 ms 84 dummy 45576180758333519
size 11 ms 76 dummy -2076414031555032800
size 12 ms 71 dummy 8590737434032103648
size 13 ms 75 dummy 523703342810576124
size 14 ms 69 dummy -3144920261228924113
size 15 ms 65 dummy 8825475312729752490
size 16 ms 61 dummy -4258304169352728375
size 17 ms 65 dummy 1473503919288143471
size 18 ms 61 dummy -2259723478978496643
size 19 ms 58 dummy 5583636303412347040
size 20 ms 55 dummy -8784936903143399443
size 21 ms 58 dummy 4750625997572211288
size 22 ms 56 dummy -144719238869246484
size 23 ms 55 dummy -9108960555359786981
size 24 ms 53 dummy -6272977115409305497
size 25 ms 55 dummy 4046467366776427285
size 26 ms 53 dummy -231965464459261563
size 27 ms 52 dummy -4089422190855533553
size 28 ms 49 dummy 8598839661217561320
size 29 ms 52 dummy -913685610243521121
size 30 ms 50 dummy 1570509165348456471
size 31 ms 48 dummy -8248876575116730462

polyHash8
size 4 ms 195 dummy 8760260588898338138
size 8 ms 111 dummy 7078634352035461215
size 16 ms 60 dummy 6682700241624697177
size 32 ms 39 dummy 2659572827088986478
size 64 ms 29 dummy -4814808130109833595
size 128 ms 27 dummy -7981684112431862738


size 1 ms 707 dummy -3824498378514461904
size 2 ms 354 dummy -5628078537833818840
size 3 ms 292 dummy -5808192479228009391
size 4 ms 196 dummy 8760260588898338138
size 5 ms 163 dummy 4917408626969370713
size 6 ms 141 dummy 6259455716849264225
size 7 ms 125 dummy 5696745779485193228
size 8 ms 112 dummy 7078634352035461215
size 9 ms 107 dummy -6071743909016257693
size 10 ms 97 dummy -2070092929527275987
size 11 ms 88 dummy 6972054182193414750
size 12 ms 81 dummy 2609072509322791331
size 13 ms 77 dummy 2586107110054191778
size 14 ms 69 dummy -4250117743176510990
size 15 ms 65 dummy 1541262423055942440
size 16 ms 60 dummy 6682700241624697177
size 17 ms 64 dummy -2684666249622847310
size 18 ms 61 dummy -2795971602849012559
size 19 ms 58 dummy 607823850783777982
size 20 ms 54 dummy 8856144388608577076
size 21 ms 52 dummy -257297477415105658
size 22 ms 50 dummy 3927910921997790381
size 23 ms 51 dummy 6684261416981385777
size 24 ms 47 dummy 8556641971644156976
size 25 ms 50 dummy 4935347551101555569
size 26 ms 47 dummy 8804018450155840372
size 27 ms 45 dummy -4351187959501377543
size 28 ms 44 dummy 7469956870713128712
size 29 ms 43 dummy -586512765560239210
size 30 ms 42 dummy -4245690868767741034
size 31 ms 43 dummy -6791926489625541198


murmur3
size 1 ms 941 dummy -48176051578450106
size 2 ms 438 dummy -6503511543650929
size 4 ms 274 dummy 1018664307466009
size 8 ms 169 dummy 416142188670482
size 16 ms 111 dummy -17333195331433
size 32 ms 84 dummy -272972546692114
size 64 ms 71 dummy 384303819542537
size 128 ms 64 dummy 23121730936452

     */

}
