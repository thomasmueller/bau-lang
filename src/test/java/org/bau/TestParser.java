package org.bau;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import org.bau.parser.Parser;

public class TestParser {

    @Test
    public void tabs() {
        try {
            new Parser("\ta:1").parse();
            fail();
        } catch (Exception e) {
            // ok
        }
    }

    @Test
    public void longLines() {
        assertEquals("a : 2\n"
                + "println('Hello ', 'world')\n"
                + "",
                new Parser("""
                        a : 1 +
                            1
                        println('Hello ',
                              'world')
                        """).parse().toString());
    }

    @Test
    public void test() {

        assertEquals("""
a := 1
while 1
    break a > 10
    println('hello ', a)
println('end')
                """,
                new Parser("a:=1; while 1 { break a>10; println('hello ', a); } println('end');").parse().toString());
        assertEquals("""
a := 1
while 1
    break a > 10
    continue a < 10
    println(a)
    a += 1
println('end')
                """,
                new Parser("a:=1\n" +
                        "while 1\n" +
                        "  break a>10\n" +
                        "  continue a<10\n" +
                        "  println(a)\n" +
                        "  a+=1\n" +
                        "println('end')\n").parse().toString());
        assertEquals("""
a := 1
if a = 0
    println('zero')
elif a = 1
    println('one')
else
    println('other')
println('done')
                """,
                new Parser("a:= 1\n" +
                        "if a=0\n" +
                        "  println('zero')\n" +
                        "elif a=1\n" +
                        "  println('one')\n" +
                        "else\n" +
                        "  println('other')\n" +
                        "println('done')").parse().toString());
        assertEquals("a : 5\n",
                new Parser("a : 1 + 2 * 3 & 5").parse().toString());
    }

    @Test
    public void function() {
        assertEquals("""
a := 1
while 1
    break a > 10
    println(a)
    a += 1
println('end')
                """,
                new Parser("a:=1\n" +
                        "while 1\n" +
                        "  break a>10\n" +
                        "  println(a)\n" +
                        "  a+=1\n" +
                        "println('end')\n").parse().toString());
    }
}
