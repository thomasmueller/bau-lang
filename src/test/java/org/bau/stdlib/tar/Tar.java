package org.bau.stdlib.tar;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

import org.bau.stdlib.datetime.DateTime;
import org.bau.stdlib.datetime.DateTime.DateTimeInfo;

public class Tar {

    public static void main(String... args) throws IOException {
        String fileName = args[0];
        RandomAccessFile f = new RandomAccessFile(fileName, "r");
        long pos = 0;
        while (pos < f.length()) {
            FileEntry e = readEntry(f, pos);
            pos += 512;
            if (e == null) {
                continue;
            }
            pos += e.size;
            pos = (pos + 511) / 512 * 512;
            if (e.type != '0') {
                continue;
            } else if (e.name.startsWith(".")) {
                // Mac OS resource forks
                continue;
            } else if (e.name.indexOf("/._") >= 0) {
                // Mac OS resource forks
                continue;
            }
            DateTimeInfo info = DateTime.getDateTimeInfo(e.lastModified * 1000);
            String ts = DateTime.formatDateTime(info, "YYYY-MM-DD hh:mm:ss");
            System.out.printf("%10d %s %-50s \n", e.size, ts, e.name);
        }
        f.close();
        int test;

    }

    static FileEntry readEntry(RandomAccessFile r, long pos) throws IOException {
        FileEntry e = new FileEntry();
        r.seek(pos);
        byte[] header = new byte[512];
        r.readFully(header);
        String ustarFlag = readString(header, 257, 6);
        if ("ustar".equals(ustarFlag)) {
            String ustarVersion = readString(header, 263, 2);
            if (!ustarVersion.equals("00")) {
                return null;
            }
            e.ownerName = readString(header, 265, 32);
            e.groupName = readString(header, 297, 32);
            e.deviceMajor = readNumber(header, 329, 8);
            e.deviceMinor = readNumber(header, 337, 8);
            e.prefix = readString(header, 345, 155);
        }
        e.name = readString(header, 0, 100);
        e.mode = readNumber(header, 100, 8);
        e.ownerId = readNumber(header, 108, 8);
        e.groupId = readNumber(header, 116, 8);
        e.size = readNumber(header, 124, 12);
        e.lastModified = readNumber(header, 136, 12);
        e.checksum = readNumber(header, 148, 8);
        long calculatedChecksum = calculateChecksum(header);
        if (e.checksum != calculatedChecksum) {
            return null;
        }
        e.type = (char) (header[156] & 0xff);
        e.linkName = readString(header, 157, 100);
        return e;
    }

    static long calculateChecksum(byte[] header) {
        long sum = 0;
        for (int i = 0; i < header.length; i++) {
            if (i >= 148 && i < 148 + 8) {
                sum += 32;
            } else {
                sum += header[i] & 0xff;
            }
        }
        return sum;
    }

    static String readString(byte[] data, int pos, int len) {
        int todoReuseBuffer;
        // re-use?
        byte[] buff = new byte[len + 1];
        int i = 0;
        for (; i < len; i++) {
            int x = data[i + pos] & 0xff;
            if (x == 0) {
                break;
            }
            buff[i] = (byte) x;
        }
        return new String(buff, 0, i, StandardCharsets.UTF_8);
    }

    static long readNumber(byte[] data, int pos, int len) {
        long result = 0;
        for (int i = 0; i < len; i++) {
            int x = data[i + pos] & 0xff;
            if (x == 0 || x == ' ') {
                break;
            }
            result = (result << 3) | (x - '0');
        }
        return result;
    }

    static class FileEntry {
        String name;
        long mode;
        long ownerId;
        String ownerName;
        long groupId;
        String groupName;
        long size;
        long lastModified;
        long checksum;
        char type; // 0 normal, 1 hard link, 2 soft link;
        String linkName;
        long deviceMajor;
        long deviceMinor;
        String prefix;

        public String toString() {
            StringBuilder buff = new StringBuilder();
            buff.append("name: " + name + "\n");
            buff.append("    mode: " + mode + "\n");
            buff.append("    ownerId: " + ownerId + "\n");
            return buff.toString();
        }
    }

}
