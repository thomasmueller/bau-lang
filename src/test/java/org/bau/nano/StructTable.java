package org.bau.nano;

public class StructTable {

    public static void main(String... args) {
        String[] fields = new String[] {
                "id", "name", "firstName", "lastName",
                "street", "phone", "city", "email",
                "middleName", "website",
                };
        int len = fields.length;

        Table table = new Table(len);
        for(int i=0; i<len; i++) {
            table.fieldNames[i] = nameToLong(fields[i]);
        }
        table.mphf = findMinimalPerfectHash(table.fieldNames);
        System.out.println("mphf: " + table.mphf);
        for(String f : fields) {
            int index = index(f, table.mphf, table.len);
            System.out.println("index: " + index + "; field name: " + f);
        }

    }

    static int index(String fieldName, long mphf, int len) {
        long f = nameToLong(fieldName);
        long hash = hash64(f, mphf);
        int index = reduce((int) hash, len);
        return index;
    }

    static class Table {
        // number of records
        int len;
        // minimal perfect hash of the field names
        long mphf;
        long[] fieldNames;
        long[] values;

        Table(int len) {
            this.len = len;
            this.fieldNames = new long[len];
            this.values = new long[len];
        }
    }

    static long nameToLong(String name) {
        long x = 0;
        for(int i=0; i<name.length() && i < 8; i++) {
            x = (x << 8) | (name.charAt(i) & 0xff);
            // System.out.println("    " + (char) name.charAt(i) + " => " + Long.toHexString(x));
        }
        return x;
    }

    static long findMinimalPerfectHash(long[] fieldNames) {
        int len = fieldNames.length;
        if (len > 64) {
            throw new IllegalArgumentException("Up to 64 entries only");
        }
        outer: for (int x = 0;; x++) {
            long bits = 0;
            for (long f : fieldNames) {
                long h = hash64(f, x);
                int index = reduce((int) h, len);
                long bit = 1L << index;
                if ((bits & bit) != 0) {
                    continue outer;
                }
                bits |= bit;
            }
            return x;
        }
    }

    static long hash64(long x, long seed) {
        x += seed;
        x = (x ^ (x >>> 33)) * 0xff51afd7ed558ccdL;
        x = (x ^ (x >>> 33)) * 0xc4ceb9fe1a85ec53L;
        x = x ^ (x >>> 33);
        return x;
    }

    public static int reduce(int hash, int n) {
        // http://lemire.me/blog/2016/06/27/a-fast-alternative-to-the-modulo-reduction/
        return (int) (((hash & 0xffffffffL) * n) >>> 32);
    }

}
