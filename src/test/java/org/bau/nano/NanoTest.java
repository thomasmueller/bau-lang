package org.bau.nano;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NanoTest {


    public static int fibonacciLoop(int n) {
        int r1, r0 = 0, r = 1;
        int i = 0;
        while(true) {
            if (i >= n) break;
            r1 = r0;
            r0 = r;
            r = r1 + r0;
            i++;
        }
        return r;
    }

    @Test
    public void fibonacci() {
        String s = """
                n := 1000000
                r0 := 0
                r1 := 0
                r := 0
                i := 0
                fun fibonacci()
                    r0 = 0
                    r1 = 0
                    r = 1
                    loop
                        break i >= n
                        r1 = r0
                        r0 = r
                        r = r1 + r0
                        i = i + 1

                fun main()
                    printText('fibonacci(')
                    print(n)
                    printText(') = ')
                    fibonacci()
                    print(r)
                """;
        NanoBau p = new NanoBau();
        p.parse(s);
        long start = System.currentTimeMillis();
        assertEquals("fibonacci(1000000) = 2756670985995446685", p.run());
        long time = System.currentTimeMillis() - start;
        assertTrue(time >= 0);
        // System.out.println("time: " + (System.currentTimeMillis() - time));

/*

n := 0
p2 := 0
p1 := 0

public static int fibonacciLoop(int nthNumber) {
        //use loop
        int previouspreviousNumber, previousNumber = 0, currentNumber = 1;

        for (int i = 1; i < nthNumber ; i++) {

            previouspreviousNumber = previousNumber;

            previousNumber = currentNumber;

            currentNumber = previouspreviousNumber + previousNumber;

        }
        return currentNumber;
    }

 */

    }

    @Test
    public void hello() {
        String s = """
                fun main()
                    printText('hello')
                """;
        NanoBau p = new NanoBau();
        p.parse(s);
        assertEquals("hello", p.run());
    }

    @Test
    public void ifThen() {
        String s = """
                a := 0
                fun main()
                    loop
                        print(a)
                        printText(':')
                        if a = 1
                            printText('one')
                        printEnd()
                        a = a + 1
                        break a > 3
                """;
        NanoBau p = new NanoBau();
        p.parse(s);
        assertEquals("0:\n"
                + "1:one\n"
                + "2:\n"
                + "3:\n"
                + "", p.run());
    }

    @Test
    public void ifElse() {
        String s = """
                a := 0
                fun main()
                    loop
                        print(a)
                        printText(':')
                        if a = 1
                            printText('one')
                        else
                            printText('not one')
                        printEnd()
                        a = a + 1
                        break a > 5
                """;
        NanoBau p = new NanoBau();
        p.parse(s);
        assertEquals("0:not one\n"
                + "1:one\n"
                + "2:not one\n"
                + "3:not one\n"
                + "4:not one\n"
                + "5:not one\n"
                + "", p.run());
    }

    @Test
    public void elif() {
        String s = """
                a := 0
                fun main()
                    loop
                        print(a)
                        printText(':')
                        if a = 1
                            printText('one')
                        elif a = 2
                            printText('two')
                        else
                            printText('something else')
                        printEnd()
                        a = a + 1
                        break a > 5
                """;
        NanoBau p = new NanoBau();
        p.parse(s);
        assertEquals("0:something else\n"
                + "1:one\n"
                + "2:two\n"
                + "3:something else\n"
                + "4:something else\n"
                + "5:something else\n"
                + "", p.run());
    }

    @Test
    public void factorial() {
        String s = """
                x := 0
                result := 0
                fun factorial()
                    result = 1
                    loop
                        break x <= 0
                        result = result * x
                        x = x - 1
                fun main()
                    x = 10
                    factorial()
                    print(result)
                """;
        NanoBau p = new NanoBau();
        p.parse(s);
        assertEquals("3628800", p.run());
    }

    @Test
    public void pi() {
        String s = """
                PI : 31415
                fun main()
                    print(PI * 20000 / 10000)
                """;
        NanoBau p = new NanoBau();
        p.parse(s);
        assertEquals("62830", p.run());
    }

    @Test
    public void operatorPrecedence() {
        String s = """
                fun main()
                    print(1 + 2 * 3)
                """;
        NanoBau p = new NanoBau();
        p.parse(s);
        assertEquals("7", p.run());
    }

    @Test
    public void comment() {
        String s = """

                # this is a single line comment
                fun main()

                    # another comment
                    printText('hello')

                """;
        NanoBau p = new NanoBau();
        p.parse(s);
        assertEquals("hello", p.run());
    }

    @Test
    public void nestedLoop() {
        String s = """
                i := 0
                j := 0
                sum := 0
                fun main()
                    loop
                        break i > 10
                        i = i + 1
                        j = 0
                        loop
                            break j > 10
                            sum = sum + i + j
                            j = j + 1
                    print(sum)
                """;
        NanoBau p = new NanoBau();
        p.parse(s);
        assertEquals("1331", p.run());
    }

    @Test
    public void array() {
        String s = """
                a : array(10)
                i := 0
                fun main()
                    i = 0
                    loop
                        a[i] = i
                        i = i + 1
                        break i = 10
                    i = 0
                    loop
                        print(a[i])
                        i = i + 1
                        break i = 10
                """;
        NanoBau p = new NanoBau();
        p.parse(s);
        assertEquals("0123456789", p.run());
    }

}
