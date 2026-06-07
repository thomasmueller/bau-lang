package org.bau.benchmarks;

public class Fibonacci {

    public static void main(String[] args) {
        System.out.println("Java");
        for (int test = 0; test < 5; test++) {
            long start = System.currentTimeMillis();
            for (int i = 0; i < 10; i++) {
                long result = fibonacci(35);
                System.out.println("fib(35) = " + result);
            }
            long time = System.currentTimeMillis() - start;
            System.out.println(time + " ms");
        }
    }

    private static long fibonacci(int n) {
        if (n <= 1) {
            return n;
        }
        return fibonacci(n - 1) + fibonacci(n - 2);
    }
}
