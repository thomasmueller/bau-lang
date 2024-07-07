package org.bau.perf;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ExceptionPerformanceTest {

    static long randomSeed = 0;
    
    static MyException ONE_EX = new MyException(1);
    static class MyException extends RuntimeException {
        private static final long serialVersionUID = -8003357823276752569L;
        public final long code;
        MyException(long code) {
            this.code = code;
        }
        @Override
        public Throwable fillInStackTrace() {
            return this;
        }
    };
    
    static long random() {
        randomSeed += 0x9e3779b97f4a7c15L;
        long z = randomSeed;
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }
    
    static long square(long x) throws MyException {
        if (x > 1_000_000_000) {
            // throw ONE_EX;
            throw new MyException(x & 0xff);
        }
      return x * x;
    }
            
    @Test
    public void test() {
        for (int test = 0; test < 10; test++)
            testPerf();
    }
    
    void testPerf() {
        // -2700154805279709700 dummy

        // Java
        // 3 ns/call ONE_EX (without malloc, but no error code)
        // 8 ns/call with empty fillInStackTrace
        // 461 ns/call with stack trace
        
        // bau:
        // 13 ns/call with code + message
        // 0 ns/call code as int
        // 1 ns/call code as i8 or i32
        // 4 ns/call with code + message
        // 4 ns/call with code + message + data
        long time = System.nanoTime();
        randomSeed = 0;
        // int count = 1_000_000_000;
        int count = 1_000;
        long sum = 0;
        for (int i = 0; i < count; i++) {
            try {
                sum += square(random());
            } catch (MyException e) {
                sum += e.code;
            }
        }
        time = System.nanoTime() - time;
        // System.out.println((time / count) + " ns/call");
        // System.out.println(sum + " dummy");
        assertTrue(sum != 0);
    }
}
