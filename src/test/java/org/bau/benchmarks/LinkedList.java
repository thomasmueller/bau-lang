// https://lemire.me/blog/2026/02/15/how-bad-can-python-stop-the-world-pauses-get/

package org.bau.benchmarks;

public class LinkedList {

    static class Node {
        int value;
        Node next;
        Node(int value) {
            this.value = value;
            this.next = null;
        }
        void addNext(Node node) {
            this.next = node;
        }
    }

    public static Node createLinkedList(int limit) {
        Node head = new Node(0);
        Node current = head;
        for (int i = 1; i < limit; i++) {
            Node newNode = new Node(i);
            current.addNext(newNode);
            current = newNode;
        }
        return head;
    }

    public static void main(String[] args) {
        System.out.println("Starting stress test...");
        Node x = createLinkedList(50_000_000);
        int count = 1_000_000;
        int len = 1000;
        long worst = 0;
        long last = System.nanoTime();
        for (int i = 0; i < count; i++) {
            createLinkedList(len);
            long now = System.nanoTime();
            worst = Math.max(worst, now - last);
            last = now;
        }
        System.out.println("Max delay: " + (worst / 1_000_000.0) + " ms; dummy=" + x.value);
    }

}
