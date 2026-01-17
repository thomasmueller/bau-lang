package org.bau.stdlib.qrcode;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.BitSet;

public class QrCode {

    private final static int[] GF_EXP = new int[256];
    private final static int[] GF_LOG = new int[256];
    private final static int[] ZERO_POLY = new int[] { 0 };
    private final static int[] ONE_POLY = new int[] { 1 };

    static {
        int primitive = 285;
        int size = 256;
        int x = 1;
        for (int i = 0; i < size; i++) {
            GF_EXP[i] = x;
            x *= 2;
            if (x >= size) {
                x ^= primitive;
                x &= size - 1;
            }
        }
        for (int i = 0; i < size - 1; i++) {
            GF_LOG[GF_EXP[i]] = i;
        }
    }

    static final byte[] ECC_CODEWORDS_PER_RS_BLOCK = { -1, 10, 16, 26, 18, 24, 16, 18, 22, 22, 26, 30, 22, 22, 24,
            24, 28, 28, 26, 26, 26, 26, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28 };

    static final byte[] ECC_BLOCK_COUNT = { -1, 1, 1, 1, 2, 2, 4, 4, 4, 5, 5, 5, 8, 9, 9, 10, 10, 11, 13, 14, 16, 17,
            17, 18, 20, 21, 23, 25, 26, 28, 29, 31, 33, 35, 37, 38, 40, 43, 45, 47, 49 };

    // version (1..40)
    private final int version;

    // width and height (21..177); 4 * version + 17
    private final int size;

    // the module bits
    private BitSet modules;

    // which of the modules bits are functional (part of the finder pattern etc).
    private BitSet functional;

    public static QrCode encodeText(String text) {
        return encodeText(text, 2);
    }

    public static QrCode encodeText(String text, int mask) {
        byte[] utf8 = text.toString().getBytes(StandardCharsets.UTF_8);
        int len = utf8.length;
        int version = 1;
        int dataUsedBits;
        int lenBits = 8;
        int dataCapacityBits;
        // version
        for (;;version++) {
            dataCapacityBits = 8 * (getDataModuleCount(version) / 8 - ECC_CODEWORDS_PER_RS_BLOCK[version] * ECC_BLOCK_COUNT[version]);
            if (version >= 10) {
                lenBits = 16;
            }
            if (len >= (1 << lenBits)) {
                continue;
            }
            dataUsedBits = 4 + lenBits + len * 8;
            if (dataUsedBits <= dataCapacityBits) {
                break;
            }
            if (version >= 40) {
                throw new IllegalArgumentException("Text too long");
            }
        }
        BitSet set = new BitSet();
        int bitPos = 0;
        bitPos = writeBits(set, bitPos, 4, 4);
        bitPos = writeBits(set, bitPos, lenBits, len);
        for (byte b : utf8) {
            bitPos = writeBits(set, bitPos, 8, b & 0xff);
        }
        // add terminator and padding
        bitPos = writeBits(set, bitPos, Math.min(4, dataCapacityBits - bitPos), 0);
        bitPos = writeBits(set, bitPos, (8 - bitPos % 8) % 8, 0);
        for (int padByte = 0xec; bitPos < dataCapacityBits; padByte ^= 0xec ^ 0x11) {
            bitPos = writeBits(set, bitPos, 8, padByte);
        }
        byte[] data = new byte[bitPos / 8];
        for (int i = 0; i < bitPos; i++) {
            data[i >>> 3] |= (set.get(i) ? 1 : 0) << (7 - (i & 7));
        }
        return new QrCode(version, data, mask);
    }

    public int getSize() {
        return size;
    }

    public boolean getModule(int x, int y) {
        return 0 <= x && x < size && 0 <= y && y < size && modules.get(x + y * size);
    }

    private static int writeBits(BitSet bits, int bitPos, int len, int value) {
        for (int i = len - 1; i >= 0; i--, bitPos++) {
            bits.set(bitPos, getBit(value, i));
        }
        return bitPos;
    }

    private QrCode(int version, byte[] data, int mask) {
        this.version = version;
        size = version * 4 + 17;
        modules = new BitSet(size * size);
        functional = new BitSet(size * size);
        // timing patterns
        for (int i = 0; i < size; i++) {
            setFunctionModule(6 + size * i, i % 2 == 0);
            setFunctionModule(i + size * 6, i % 2 == 0);
        }
        // 3 finder patterns
        for (int i = 0; i < 3; i++) {
            for (int xo = -4; xo <= 4; xo++) {
                for (int yo = -4; yo <= 4; yo++) {
                    int x = xo + 3 + (i & 1) * (size - 7);
                    int y = yo + 3 + (i >> 1) * (size - 7);
                    if (x >= 0 && x < size && y >= 0 && x < size) {
                        int dist = Math.max(Math.abs(xo), Math.abs(yo));
                        setFunctionModule(x + size * y, dist != 2 && dist != 4);
                    }
                }
            }
        }
        // alignment patterns
        if (version > 1) {
            int align = version / 7 + 2;
            int step = (version * 8 + align * 3 + 5) / (align * 4 - 4) * 2;
            int[] alignPos = new int[align];
            alignPos[0] = 6;
            for (int i = alignPos.length - 1, pos = size - 7; i >= 1; i--, pos -= step) {
                alignPos[i] = pos;
            }
            for (int i = 0; i < align; i++) {
                for (int j = 0; j < align; j++) {
                    if ((i == 0 && j == 0) || (i == 0 && j == align - 1) || (j == 0 && i == align - 1)) {
                        continue;
                    }
                    int x = alignPos[i];
                    int y = alignPos[j];
                    for (int dy = -2; dy <= 2; dy++) {
                        for (int dx = -2; dx <= 2; dx++) {
                            setFunctionModule(x + dx + size * (y + dy), Math.max(Math.abs(dx), Math.abs(dy)) != 1);
                        }
                    }
                }
            }
        }
        // format
        int bits = getFormatBits(mask);
        for (int i = 0; i < 15; i++) {
            int p = 8 + size * i;
            if (i >= 9) {
                p = 14 - i + size * 8;
            } else if (i == 8) {
                p--;
            } else if (i >= 6) {
                p += size;
            }
            setFunctionModule(p, getBit(bits, i));
            p = size - 1 - i + size * 8;
            if (i >= 8) {
                p = 8 + size * (size - 15 + i);
            }
            setFunctionModule(p, getBit(bits, i));
        }
        // dark
        setFunctionModule(8 + size * (size - 8), true);
        if (version >= 7) {
            // version
            bits = getVersionBits(version);
            for (int i = 0; i < 18; i++) {
                boolean bit = getBit(bits, i);
                int a = size - 11 + i % 3;
                int b = i / 3;
                setFunctionModule(a + size * b, bit);
                setFunctionModule(b + size * a, bit);
            }
        }
        byte[] codewords = encodeCodewords(data);
        int di = 0;
        // zigzag
        for (int right = size - 1; right >= 1; right -= 2) {
            if (right == 6) {
                right = 5;
            }
            for (int y0 = 0; y0 < size; y0++) {
                for (int j = 0; j < 2; j++) {
                    int x = right - j;
                    boolean upward = ((right + 1) & 2) == 0;
                    int y = upward ? size - 1 - y0 : y0;
                    if (!functional.get(x + y * size) && di < codewords.length * 8) {
                        modules.set(x + y * size, getBit(codewords[di >>> 3], 7 - (di & 7)));
                        di++;
                    }
                }
            }
        }
        applyMask(modules, functional, size, mask);
        functional = null;
    }

    private static void applyMask(BitSet modules, BitSet functional, int size, int mask) {
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int invert;
                switch (mask) {
                case 0:
                    invert = (x + y) % 2;
                    break;
                case 1:
                    invert = y % 2;
                    break;
                case 2:
                    invert = x % 3;
                    break;
                case 3:
                    invert = (x + y) % 3;
                    break;
                case 4:
                    invert = (x / 3 + y / 2) % 2;
                    break;
                case 5:
                    invert = x * y % 2 + x * y % 3;
                    break;
                case 6:
                    invert = (x * y % 2 + x * y % 3) % 2;
                    break;
                case 7:
                    invert = ((x + y) % 2 + x * y % 3) % 2;
                    break;
                default:
                    throw new IllegalArgumentException();
                }
                if (invert == 0 && !functional.get(x + y * size)) {
                    modules.flip(x + y * size);
                }
            }
        }
    }

    private byte[] encodeCodewords(byte[] data) {
        int blockCount = ECC_BLOCK_COUNT[version];
        int blockEccLen = ECC_CODEWORDS_PER_RS_BLOCK[version];
        int rawCodewords = getDataModuleCount(version) / 8;
        int shortBlocks = blockCount - rawCodewords % blockCount;
        int shortBlockLen = rawCodewords / blockCount;
        byte[][] blocks = new byte[blockCount][];
        // Reed-Solomon polynomial coefficients (highest to lowest),
        // excluding the leading term, 1.
        // eg. x^3 + 255x^2 + 8x + 93 => {255, 8, 93}.
        byte[] coefficients = new byte[blockEccLen];
        coefficients[blockEccLen - 1] = 1;
        int root = 1;
        for (int i = 0; i < blockEccLen; i++) {
            // multiply by (x - r^i)
            for (int j = 0; j < coefficients.length; j++) {
                coefficients[j] = (byte) gfMultiply(coefficients[j] & 0xff, root);
                if (j + 1 < coefficients.length) {
                    coefficients[j] ^= coefficients[j + 1];
                }
            }
            root = gfMultiply(root, 2);
        }
        for (int i = 0, k = 0; i < blockCount; i++) {
            byte[] buff = Arrays.copyOfRange(data, k, k + shortBlockLen - blockEccLen + (i < shortBlocks ? 0 : 1));
            k += buff.length;
            byte[] block = Arrays.copyOf(buff, buff.length + coefficients.length);
            // Reed-Solomon remainder
            byte[] ecc = new byte[coefficients.length];
            for (byte b : buff) {
                // polynomial division
                int factor = (b ^ ecc[0]) & 0xff;
                System.arraycopy(ecc, 1, ecc, 0, ecc.length - 1);
                ecc[ecc.length - 1] = 0;
                for (int j = 0; j < ecc.length; j++) {
                    ecc[j] ^= gfMultiply(coefficients[j] & 0xff, factor);
                }
            }
            System.arraycopy(ecc, 0, block, block.length - blockEccLen, ecc.length);
            blocks[i] = block;
        }
        // interleave
        byte[] result = new byte[rawCodewords];
        for (int i = 0, k = 0; i < blocks[blocks.length - 1].length; i++) {
            for (int j = 0; j < blocks.length; j++) {
                if (i >= blocks[j].length) {
                    continue;
                }
                result[k] = blocks[j][i];
                k++;
            }
        }
        return result;
    }

    private static boolean getBit(int x, int i) {
        return ((x >>> i) & 1) != 0;
    }

    private void setFunctionModule(int p, boolean value) {
        modules.set(p, value);
        functional.set(p, true);
    }

    private static int getDataModuleCount(int version) {
        int size = version * 4 + 17;
        // 3 finder patterns, format, dark, timing
        int result = size * size - 223 - 2 * (size - 16);
        if (version >= 2) {
            int align = version / 7 + 1;
            // alignment patterns
            result -= 25 * align * align + 40 * (align - 1);
            if (version >= 7)
                // version
                result -= 36;
        }
        return result;
    }

    // decoding logic -------------------------------------------------------------------------

    public static String decodeText(BitSet modules, int size) {
        BitSet functional = new BitSet(size * size);
        int version = (size - 17) / 4;
        if (version > 6) {
            int versionBits = 0, versionBits2 = 0;
            for (int i = 17; i >= 0; i--) {
                int a = size - 11 + i % 3;
                int b = i / 3;
                functional.set(a + b * size, true);
                versionBits = (versionBits << 1) | (modules.get(a + b * size) ? 1 : 0);
                functional.set(b + a * size, true);
                versionBits2 = (versionBits2 << 1) | (modules.get(b + a * size) ? 1 : 0);
            }
            for (version = 7;; version++) {
                int test = getVersionBits(version);
                if (Integer.bitCount(versionBits ^ test) < 4 || Integer.bitCount(versionBits2 ^ test) < 4) {
                    break;
                }
                if (version > 40) {
                    throw new IllegalArgumentException("Can not parse version");
                }
            }
        }
        int formatBits = 0, formatBits2 = 0;
        for (int i = 14; i >= 0; i--) {
            int p = 8 + size * i;
            if (i >= 9) {
                p = 14 - i + size * 8;
            } else if (i == 8) {
                p--;
            } else if (i >= 6) {
                p += size;
            }
            functional.set(p, true);
            formatBits = (formatBits << 1) | (modules.get(p) ? 1 : 0);
            p = size - 1 - i + size * 8;
            if (i >= 8) {
                p = 8 + size * (size - 15 + i);
            }
            functional.set(p, true);
            formatBits2 = (formatBits2 << 1) | (modules.get(p) ? 1 : 0);
        }
        functional.set(8 + size * (size - 8), true);
        int format = 0;
        for(;; format++) {
            int test = getFormatBits(format);
            if (Integer.bitCount(formatBits ^ test) < 4 || Integer.bitCount(formatBits2 ^ test) < 4) {
                break;
            }
            if (format > 31) {
                throw new IllegalArgumentException("Can not parse format");
            }
        }
        // timing patterns
        for (int i = 0; i < size; i++) {
            functional.set(6 + size * i, true);
            functional.set(i + size * 6, true);
        }
        // 3 finder patterns
        for (int i = 0; i < 3; i++) {
            for (int xo = -4; xo <= 4; xo++) {
                for (int yo = -4; yo <= 4; yo++) {
                    int x = xo + 3 + (i & 1) * (size - 7);
                    int y = yo + 3 + (i >> 1) * (size - 7);
                    if (x >= 0 && x < size && y >= 0 && x < size) {
                        functional.set(x + size * y, true);
                    }
                }
            }
        }
        // alignment patterns
        if (version > 1) {
            int align = version / 7 + 2;
            int step = (version * 8 + align * 3 + 5) / (align * 4 - 4) * 2;
            int[] alignPos = new int[align];
            alignPos[0] = 6;
            for (int i = alignPos.length - 1, pos = size - 7; i >= 1; i--, pos -= step) {
                alignPos[i] = pos;
            }
            for (int i = 0; i < align; i++) {
                for (int j = 0; j < align; j++) {
                    if ((i == 0 && j == 0) || (i == 0 && j == align - 1) || (j == 0 && i == align - 1)) {
                        continue;
                    }
                    int x = alignPos[i];
                    int y = alignPos[j];
                    for (int dy = -2; dy <= 2; dy++) {
                        for (int dx = -2; dx <= 2; dx++) {
                            functional.set(x + dx + size * (y + dy), true);
                        }
                    }
                }
            }
        }
        int mask = format & 7;
        applyMask(modules, functional, size, mask);
        int rawCodewords = getDataModuleCount(version) / 8;
        byte[] codewords = new byte[rawCodewords];
        int di = 0;
        // zigzag
        for (int right = size - 1; right >= 1; right -= 2) {
            if (right == 6) {
                right = 5;
            }
            for (int y0 = 0; y0 < size; y0++) {
                for (int j = 0; j < 2; j++) {
                    int x = right - j;
                    boolean upward = ((right + 1) & 2) == 0;
                    int y = upward ? size - 1 - y0 : y0;
                    if (!functional.get(x + y * size) && di < codewords.length * 8) {
                        if (modules.get(x + y * size)) {
                            codewords[di >>> 3] |= 1 << (7 - (di & 7));
                        }
                        di++;
                    }
                }
            }
        }
        byte[] data = decodeCodewords(codewords, version);
        int len = ((data[0] & 0xf) << 4) + ((data[1] & 0xf0) >> 4);
        int in = 1;
        if (version >= 10) {
            in++;
            len = (len << 8) + ((data[1] & 0xf) << 4) + ((data[2] & 0xf0) >> 4);
        }
        byte[] utf8 = Arrays.copyOf(data, len);
        for (int i = 0; i < utf8.length; i++) {
            utf8[i] = (byte) (((data[i + in] & 0xf) << 4) + ((data[i + in + 1] & 0xf0) >> 4));
        }
        return new String(utf8, StandardCharsets.UTF_8);

    }

    private static byte[] decodeCodewords(byte[] codewords, int version) {
        int blockCount = ECC_BLOCK_COUNT[version];
        int blockEccLen = ECC_CODEWORDS_PER_RS_BLOCK[version];
        int rawCodewords = getDataModuleCount(version) / 8;
        int shortBlocks = blockCount - rawCodewords % blockCount;
        int shortBlockLen = rawCodewords / blockCount;
        byte[][] blocks = new byte[blockCount][];
        for (int i = 0; i < blockCount; i++) {
            blocks[i] = new byte[shortBlockLen + (i < shortBlocks ? 0 : 1)];
        }
        int k = 0;
        for (int i = 0; i < blocks[blocks.length - 1].length; i++) {
            for (int j = 0; j < blocks.length; j++) {
                if (i >= blocks[j].length) {
                    continue;
                }
                blocks[j][i] = codewords[k++];
            }
        }
        byte[] result = new byte[blocks.length * blocks[0].length - blockEccLen];
        int p = 0;
        for (int i = 0; i < blockCount; i++) {
            byte[] block = blocks[i];
            int[] data = new int[block.length];
            for (int j = 0; j < data.length; j++) {
                data[j] = block[j] & 0xff;
            }
            data = correctErrors(data, blockEccLen);
            if (data == null) {
                throw new IllegalArgumentException("Can not correct errors");
            }
            for (int j = 0; j < data.length; j++) {
                block[j] = (byte) data[j];
            }
            for (int j = 0; j < blocks[i].length - blockEccLen; j++) {
                result[p++] = blocks[i][j];
            }
        }
        int len = ((result[0] & 0xf) << 4) + ((result[1] & 0xf0) >> 4);
        if(version >= 10) {
            len = (len << 8) + ((result[1] & 0xf) << 4) + ((result[2] & 0xf0) >> 4);
        }
        return Arrays.copyOf(result, len + 4);
    }

    private static int getVersionBits(int version) {
        int rem = version;
        for (int i = 0; i < 12; i++) {
            rem = (rem << 1) ^ ((rem >>> 11) * 0x1f25);
        }
        return version << 12 | rem;
    }

    private static int getFormatBits(int format) {
        int rem = format;
        for (int i = 0; i < 10; i++) {
            rem = (rem << 1) ^ ((rem >>> 9) * 0x537);
        }
        return (format << 10 | rem) ^ 0x5412;
    }

    private static int[] correctErrors(int[] received, int eccLen) {
        int[] poly = stripZeros(received);
        int[] syndrome = new int[eccLen];
        int errorsSum = 0;
        for (int i = 0; i < eccLen; i++) {
            int eval = evaluatePoly(poly, GF_EXP[i]);
            errorsSum += eval;
            syndrome[syndrome.length - 1 - i] = eval;
        }
        if (errorsSum == 0) {
            return received;
        }
        syndrome = stripZeros(syndrome);
        // Euclidean algorithm
        int[] rLast = buildMonomial(eccLen, 1);
        int[] r = syndrome;
        int[] tLast = ZERO_POLY;
        int[] t = ONE_POLY;
        while (2 * r.length - 1 >= eccLen) {
            int[] rLast2 = rLast;
            int[] tLast2 = tLast;
            rLast = r;
            tLast = t;
            if (rLast[0] == 0) {
                return null;
            }
            r = rLast2;
            int[] q = ZERO_POLY;
            int denomLeadingTerm = rLast[0];
            int dltInverse = gfInverse(denomLeadingTerm);
            while (r.length >= rLast.length && r[0] != 0) {
                int degreeDiff = r.length - rLast.length;
                int scale = gfMultiply(r[0], dltInverse);
                q = addPoly(q, buildMonomial(degreeDiff, scale));
                r = addPoly(r, multiplyByMonomial(rLast, degreeDiff, scale));
            }
            t = addPoly(multiplyPoly(q, tLast), tLast2);
            if (r.length >= rLast.length) {
                return null;
            }
        }
        if (r.length >= t.length) {
            return null;
        }
        int sigmaTildeAtZero = t[t.length - 1];
        int inverse = gfInverse(sigmaTildeAtZero);
        int[] sigma = multiplyPoly(t, inverse);
        int[] omega = multiplyPoly(r, inverse);
        // Chien's search
        int len = sigma.length - 1;
        int[] errorLocations = new int[len];
        int e = 0;
        for (int i = 1; i < 256 && e < len; i++) {
            if (evaluatePoly(sigma, i) == 0) {
                errorLocations[e] = gfInverse(i);
                e++;
            }
        }
        if (e != len) {
            return null;
        }
        // Forney's formula
        int[] errorMagnitudes = new int[len];
        for (int i = 0; i < len; i++) {
            int xiInv = gfInverse(errorLocations[i]);
            int denom = 1;
            for (int j = 0; j < len; j++) {
                if (i != j) {
                    denom = gfMultiply(denom, 1 ^ gfMultiply(errorLocations[j], xiInv));
                }
            }
            errorMagnitudes[i] = gfMultiply(evaluatePoly(omega, xiInv), gfInverse(denom));
        }
        for (int i = 0; i < len; i++) {
            int position = received.length - 1 - GF_LOG[errorLocations[i]];
            if (position < 0 || position >= received.length || errorMagnitudes[i] == 0) {
                return null;
            }
            received[position] = received[position] ^ errorMagnitudes[i];
        }
        return received;
    }

    private static int[] buildMonomial(int degree, int coefficient) {
        int[] coefficients = new int[degree + 1];
        coefficients[0] = coefficient;
        return coefficients;
    }

    private static int gfInverse(int a) {
        return GF_EXP[256 - GF_LOG[a] - 1];
    }

    private static int gfMultiply(int x, int y) {
        // int z = 0;
        // for (int i = 7; i >= 0; i--) {
        //     z = (z << 1) ^ ((z >>> 7) * 0x11d) ^ ((y >>> i) & 1) * x;
        // }
        // return z;
        if (x == 0 || y == 0) {
            return 0;
        }
        return GF_EXP[(GF_LOG[x] + GF_LOG[y]) % 255];
    }

    private static int[] stripZeros(int[] coefficients) {
        int len = coefficients.length;
        if (len < 2 || coefficients[0] != 0) {
            return coefficients;
        }
        int skip = 1;
        while (skip < len && coefficients[skip] == 0) {
            skip++;
        }
        if (skip == len) {
            return ZERO_POLY;
        }
        return Arrays.copyOfRange(coefficients, skip, len);
    }

    private static int evaluatePoly(int[] coefficients, int at) {
        int result = coefficients[0];
        for (int i = 1; i < coefficients.length; i++) {
            result = gfMultiply(at, result) ^ coefficients[i];
        }
        return result;
    }

    private static int[] addPoly(int[] coefficients, int[] other) {
        if (coefficients.length > other.length) {
            int[] temp = coefficients;
            coefficients = other;
            other = temp;
        }
        int[] sumDiff = new int[other.length];
        int lengthDiff = other.length - coefficients.length;
        System.arraycopy(other, 0, sumDiff, 0, lengthDiff);
        for (int i = lengthDiff; i < other.length; i++) {
            sumDiff[i] = coefficients[i - lengthDiff] ^ other[i];
        }
        return stripZeros(sumDiff);
    }

    private static int[] multiplyPoly(int[] coefficients, int[] other) {
        int cLen = coefficients.length;
        int oLen = other.length;
        int[] product = new int[cLen + oLen - 1];
        for (int i = 0; i < cLen; i++) {
            for (int j = 0; j < oLen; j++) {
                product[i + j] = product[i + j] ^ gfMultiply(coefficients[i], other[j]);
            }
        }
        return stripZeros(product);
    }

    private static int[] multiplyPoly(int[] coefficients, int scalar) {
        int size = coefficients.length;
        int[] product = new int[size];
        for (int i = 0; i < size; i++) {
            product[i] = gfMultiply(coefficients[i], scalar);
        }
        return stripZeros(product);
    }

    private static int[] multiplyByMonomial(int[] coefficients, int degree, int coefficient) {
        int size = coefficients.length;
        int[] product = new int[size + degree];
        for (int i = 0; i < size; i++) {
            product[i] = gfMultiply(coefficients[i], coefficient);
        }
        return stripZeros(product);
    }

}
