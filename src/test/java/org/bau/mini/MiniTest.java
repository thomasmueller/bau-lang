package org.bau.mini;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MiniTest {

    @Test
    public void hello() {
        String s = """
                fun main()
                    print('hello')
                """;
        MiniBau p = new MiniBau();
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
                        print(':')
                        if a = 1
                            print('one')
                        print()
                        a = a + 1
                        break a > 3
                """;
        MiniBau p = new MiniBau();
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
                        print(':')
                        if a = 1
                            print('one')
                        else
                            print('not one')
                        print()
                        a = a + 1
                        break a > 5
                """;
        MiniBau p = new MiniBau();
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
                        print(':')
                        if a = 1
                            print('one')
                        elif a = 2
                            print('two')
                        else
                            print('something else')
                        print()
                        a = a + 1
                        break a > 5
                """;
        MiniBau p = new MiniBau();
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
        MiniBau p = new MiniBau();
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
        MiniBau p = new MiniBau();
        p.parse(s);
        assertEquals("62830", p.run());
    }

    @Test
    public void operatorPrecedence() {
        String s = """
                fun main()
                    print(1 + 2 * 3)
                """;
        MiniBau p = new MiniBau();
        p.parse(s);
        assertEquals("7", p.run());
    }

    @Test
    public void comment() {
        String s = """

                # this is a single line comment
                fun main()

                    # another comment
                    print('hello')

                """;
        MiniBau p = new MiniBau();
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
        MiniBau p = new MiniBau();
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
        MiniBau p = new MiniBau();
        p.parse(s);
        assertEquals("0123456789", p.run());
    }

}
