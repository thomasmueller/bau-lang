package org.bau.stdlib.mphf;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class Consensus {

    public static void main(String[] args) {
        int n = 1_000_000 / 128 * 128;
        Random random = new Random();
        long[] keys = new long[n];
        for (int i = 0; i < n; i++)
            keys[i] = random.nextLong();
        long[][] chunks = partitionIntoChunks(keys, 128);
        Node[] chunkMPHFs = new Node[chunks.length];
        AtomicInteger sequence = new AtomicInteger();
        for (int i = 0; i < chunks.length; i++) {
            chunkMPHFs[i] = build(chunks[i], 4, 0, sequence);
            if (chunkMPHFs[i] == null) {
                throw new IllegalStateException();
            }
        }
        BitSet seen = new BitSet(n);
        int i = 0;
        for (long k : keys) {
            if (i % 100000 == 0) {
                System.out.println("i " + i);
            }
            int chunkId = findChunkId(k, chunks);
            int localIdx = hash(k, chunkMPHFs[chunkId]);
            int globalIdx = chunkId * 128 + localIdx;
            if (globalIdx < 0 || globalIdx >= n || seen.get(globalIdx)) {
                throw new IllegalStateException();
            }
            seen.set(globalIdx);
            i++;
        }
    }

    public static void mainSmall(String[] args) {
        Random rand = new Random();
        long[] keys = new long[128];
        for (int i = 0; i < 128; i++)
            keys[i] = rand.nextLong();
        Node root = build(keys, 4, 0, new AtomicInteger());
        if (root == null) {
            throw new IllegalStateException();
        }
        Set<Integer> hashes = new HashSet<>();
        root.printSeeds(0);
        for (long k : keys) {
            int h = hash(k, root);
            if (h < 0 || h >= 128 || !hashes.add(h)) {
                throw new IllegalStateException();
            }
        }
    }

    public static long[][] partitionIntoChunks(long[] keys, int chunkSize) {
        int n = keys.length;
        int numChunks = (n + chunkSize - 1) / chunkSize;
        long[][] chunks = new long[numChunks][];
        long[] hashes = new long[n];
        long seed = 42;
        for (int i = 0; i < n; i++) {
            hashes[i] = hash(keys[i], seed);
        }
        Integer[] indices = new Integer[n];
        for (int i = 0; i < n; i++)
            indices[i] = i;
        Arrays.sort(indices, (a, b) -> Long.compare(hashes[a], hashes[b]));
        int offset = 0;
        for (int i = 0; i < numChunks; i++) {
            int size = Math.min(chunkSize, n - offset);
            chunks[i] = new long[size];
            for (int j = 0; j < size; j++) {
                chunks[i][j] = keys[indices[offset + j]];
            }
            offset += size;
        }
        return chunks;
    }

    private static int findChunkId(long key, long[][] chunks) {
        for (int i = 0; i < chunks.length; i++) {
            for (long k : chunks[i]) {
                if (k == key)
                    return i;
            }
        }
        return -1;
    }

    public static long hash(long x, long seed) {
        x += seed;
        x = (x ^ (x >>> 33)) * 0xff51afd7ed558ccdL;
        x = (x ^ (x >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return x ^ (x >>> 33);
    }

    static Node build(long[] keys, int numBuckets, int offset, AtomicInteger sequence) {
        int n = keys.length;
        if (n <= 16) {
            for (int attempt = 0; attempt < 50000; attempt++) {
                long seed = sequence.getAndIncrement();
                boolean[] used = new boolean[n];
                boolean ok = true;
                for (long k : keys) {
                    int idx = (int) (hash(k, seed) & 0x7FFFFFFFL) % n;
                    if (used[idx]) {
                        ok = false;
                        break;
                    }
                    used[idx] = true;
                }
                if (ok) {
                    Node leaf = new Node();
                    leaf.seed = seed;
                    leaf.numBuckets = n;
                    leaf.bucketSize = 1;
                    leaf.offset = offset;
                    leaf.isLeaf = true;
                    return leaf;
                }
            }
            // backtrack
            return null;
        }
        int targetSize = n / numBuckets;
        int maxTries = 10000;
        if (n == 128) {
            maxTries = 1000000;
        }
        for (int attempt = 0; attempt < maxTries; attempt++) {
            long seed = sequence.getAndIncrement();
            long[][] buckets = new long[numBuckets][];
            int[] counts = new int[numBuckets];
            boolean valid = true;
            for (long k : keys) {
                int b = (int) (hash(k, seed) & 0x7FFFFFFFL) % numBuckets;
                if (counts[b] == targetSize) {
                    valid = false;
                    break;
                } // Bucket full
                if (buckets[b] == null)
                    buckets[b] = new long[targetSize];
                buckets[b][counts[b]++] = k;
            }
            if (valid) {
                Node node = new Node();
                node.seed = seed;
                node.numBuckets = numBuckets;
                node.bucketSize = targetSize;
                node.offset = offset;
                node.children = new Node[numBuckets];
                boolean allChildrenOk = true;
                for (int i = 0; i < numBuckets; i++) {
                    node.children[i] = build(buckets[i], numBuckets, offset + i * targetSize, sequence);
                    if (node.children[i] == null) {
                        allChildrenOk = false;
                        break;
                    }
                }
                if (allChildrenOk)
                    return node;
            }
        }
        return null;
    }

    static int hash(long key, Node root) {
        Node curr = root;
        while (!curr.isLeaf) {
            int b = (int) (hash(key, curr.seed) & 0x7FFFFFFFL) % curr.numBuckets;
            curr = curr.children[b];
        }
        int finalIdx = (int) (hash(key, curr.seed) & 0x7FFFFFFFL) % curr.numBuckets;
        return curr.offset + finalIdx;
    }

    static class Node {
        long seed;
        int numBuckets;
        int bucketSize;
        Node[] children;
        int offset;
        boolean isLeaf;

        public long printSeeds(long last) {
            System.out.println("+ " + (seed - last) + " = " + seed + ";");
            long x = seed;
            if (children != null) {
                for (Node n : children) {
                    x = n.printSeeds(x);
                }
            }
            return x;
        }
    }
}