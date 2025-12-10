package org.bau.stdlib.graphics;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Tiff {

    public static void writeGrayscaleTiff(byte[] imageData, int width, int height, File outFile) throws IOException {
        // byte[] pixelBytes = imageData;
        byte[] pixelBytes = packBitsEncode(imageData);
        ByteBuffer buff = ByteBuffer.allocate(1024);
        // Magic: 'MM 00 2a'
        buff.putInt(0x4d4d002a);
        // Header position: 8
        buff.putInt(0x00000008);
        // Tag count: 11
        buff.putShort((short) 11);
        // ImageWidth: width
        buff.putShort((short) 0x0100).putShort((short) 4).putInt(1).putInt(width);
        // ImageLength: height
        buff.putShort((short) 0x0101).putShort((short) 4).putInt(1).putInt(height);
        // BitsPerSample: 8
        buff.putShort((short) 0x0102).putShort((short) 3).putInt(1).putShort((short) 8).putShort((short) 0);
        // Compression: 32773 (PackBits)
        // buff.putShort((short) 0x0103).putShort((short) 3).putInt(1).putShort((short) 1).putShort((short) 0);
        buff.putShort((short) 0x0103).putShort((short) 3).putInt(1).putShort((short) 32773).putShort((short) 0);
        // PhotometricInterpretation: 1 (BlackIsZero)
        buff.putShort((short) 0x0106).putShort((short) 3).putInt(1).putShort((short) 1).putShort((short) 0);
        // StripOffsets: <unknown>
        int posStripOffsets = buff.position() + 8;
        buff.putShort((short) 0x0111).putShort((short) 4).putInt(1).putInt(0);
        // RowsPerStrip: height
        buff.putShort((short) 0x0116).putShort((short) 4).putInt(1).putInt(height);
        // StripByteCounts: <bytes>
        buff.putShort((short) 0x0117).putShort((short) 4).putInt(1).putInt(pixelBytes.length);
        // XResolution:
        int posXResolution = buff.position() + 8;
        buff.putShort((short) 0x011a).putShort((short) 5).putInt(1).putInt(0);
        // YResolution:
        int posYResolution = buff.position() + 8;
        buff.putShort((short) 0x011b).putShort((short) 5).putInt(1).putInt(0);
        // ResolutionUnit:
        buff.putShort((short) 0x0128).putShort((short) 3).putInt(1).putShort((short) 2).putShort((short) 0);
        // end
        buff.putInt(0);
        // XResolution, YResolution patch
        int p = buff.position();
        buff.putInt(posXResolution, p);
        buff.putInt(posYResolution, p);
        buff.putInt(72).putInt(1);
        // StripOffsets patch
        int end = buff.position();
        buff.putInt(posStripOffsets, end);
        RandomAccessFile f = new RandomAccessFile(outFile, "rw");
        f.write(buff.array(), 0, buff.position());
        f.write(pixelBytes);
        f.setLength(f.getFilePointer());
        f.close();
    }

    public static String readTiff(File inFile) throws IOException {
        StringBuilder buff = new StringBuilder();
        byte[] data = new byte[0];
        RandomAccessFile f = new RandomAccessFile(inFile, "r");
        data = new byte[(int) f.length()];
        f.readFully(data);
        f.close();
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
        buff.append("byte order " + Long.toHexString(in.readShort()) + "\n");
        buff.append("magic " + Long.toHexString(in.readShort()) + "\n");
        int offset = in.readInt();
        buff.append("offset " + offset + "\n");
        byte[] d2 = Arrays.copyOfRange(data, offset, data.length - offset);
        data = d2;
        in = new DataInputStream(new ByteArrayInputStream(data));
        int tagCount = in.readShort();
        int pos = 10;
        buff.append("tags " + tagCount + "\n");
        int i = 0;
        for (; i < tagCount; i++) {
            int tag = in.readShort();
            buff.append("[" + Long.toHexString(pos) + "]: tag " + Long.toHexString(tag) + " ");
            pos += 2;
            switch (tag) {
            case 0x0100:
                buff.append("ImageWidth");
                break;
            case 0x0101:
                buff.append("ImageLength");
                break;
            case 0x0102:
                buff.append("BitsPerSample");
                break;
            case 0x0103:
                buff.append("Compression");
                break;
            case 0x0106:
                buff.append("PhotometricInterpretation");
                break;
            case 0x0111:
                buff.append("StripOffsets");
                break;
            case 0x0116:
                buff.append("RowsPerStrip");
                break;
            case 0x0117:
                buff.append("StripByteCounts");
                break;
            case 0x011a:
                buff.append("XResolution");
                break;
            case 0x011b:
                buff.append("YResolution");
                break;
            case 0x0128:
                buff.append("ResolutionUnit");
                break;
            case 0:
                buff.append("end");
                break;
            default:
                buff.append("unkown tag " + tag);
                break;
            }
            if (tag == 0) {
                break;
            }
            int length = in.readShort();
            int v1 = in.readInt();
            int v2 = in.readInt();
            pos += 10;
            buff.append(
                    " " + Long.toHexString(length) + " " + Long.toHexString(v1) +
                    " " + Long.toHexString(v2) + "\n");

        }
        boolean nextPos = true;
        while (true) {
            if (nextPos) {
                buff.append("\n");
                buff.append("[" + Long.toHexString(pos) + "]: ");
                nextPos = false;
            }
            int val = in.read();
            if (val < 0) {
                break;
            }
            buff.append(" " + Long.toHexString(0x100 | (val & 0xff)).substring(1));
            pos++;
            if (pos % 16 == 0) {
                nextPos = true;
            }
        }
        return buff.toString();
    }

    private static byte[] packBitsEncode(byte[] data) {
        ByteBuffer out = ByteBuffer.allocate(data.length * 2);
        for (int i = 0; i < data.length;) {
            int runLength = 1;
            while (runLength < 128 && i + runLength < data.length && data[i] == data[i + runLength]) {
                runLength++;
            }
            if (runLength >= 3) {
                out.put((byte) (1 - runLength));
                out.put(data[i]);
                i += runLength;
            } else {
                int litStart = i;
                int litLen = 0;
                while (litLen < 128 && i < data.length) {
                    if (i + 2 < data.length && data[i] == data[i + 1] && data[i] == data[i + 2]) {
                        break;
                    }
                    i++;
                    litLen++;
                }
                out.put((byte) (litLen - 1));
                out.put(data, litStart, litLen);
            }
        }
        byte[] result = new byte[out.position()];
        out.rewind();
        out.get(result);
        return result;
    }

}
