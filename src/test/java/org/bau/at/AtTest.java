package org.bau.at;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import org.junit.Test;

public class AtTest {

    @Test
    public void shortTest() {
        assertEquals("Hello world\n", new At().run(">('Hello world')"));
        assertEquals("result=7\ndone\n", new At().run(">('result=', 1+2*3),>('done')"));
        assertEquals("a=3\n", new At().run("a:1+2,>('a='a)"));
        assertEquals("a=10\n", new At().run("a:0, *10(a:a+1), >('a='a)"));
        assertEquals("a=5\n", new At().run("a:0, *10(?a<5(a:a+1)), >('a='a)"));
        assertEquals("a=5\n", new At().run("a:0, *10(?a<5(a:a+1):(a:a+0)), >('a='a)"));
        assertEquals("a=5\n", new At().run("a:0, *10(a:?a<5(a+1):a), >('a='a)"));
        assertEquals("+--+--+--+\n", new At().run("fun%(a,b)(50,!a-((a/b)|0)*b); *10( o._: ? _%3=0 ('+'.0) : ('-'.0) ), >(o)"));
        assertEquals("+--+--+--+\n", new At().run("fun%(a,b)(50,!a-((a/b)|0)*b); *10o._:?_%3(45):43,>(o)"));
        assertEquals("+--+--+--+\n", new At().run("fun%(a,b)(50,!a-((a/b)|0)*b); i:0, @i<10( o.i: ? i%3=0 ('+'.0) : ('-'.0), i:i+1 ), >(o)"));
    }

    @Test
    public void infinityAndNaN() {
        assertEquals("Infinity\n", new At().run(">(1/0)"));
        assertEquals("-Infinity\n", new At().run(">(-1/0)"));
        assertEquals("NaN\n", new At().run(">(0/0)"));
    }

    @Test
    public void array() {
        assertEquals("6\n", new At().run("a:'hello'; >(a.-1)"));
        assertEquals("ABC\n", new At().run("a.0:65,a.1:66,a.2:67,>(a)"));
        assertEquals("same\n", new At().run("a.0:65,a.1:66,a.2:67, >(? 'ABC'=a {'same'}:{'different'})"));
        assertEquals("different\n", new At().run(">(? 'abc'=(65,66,67) {'same'}:{'different'})"));
    }

    @Test
    public void functions() {
        assertEquals("2 3 4\n", new At().run("+max(a,b){?a<b{b}:a}, >(max(1, 2)' 'max(3, 1)' 'max(-1 4))"));
        assertEquals("1 1\n", new At().run("+abs(x){?0<x{x}:-x}, >(abs(-1)' 'abs(1))"));
        assertEquals("10! 3628800\n", new At().run("+fact(x){?1<x{x*fact(x-1)}:x}, >('10! 'fact(10))"));
        assertEquals("10! 3628800\n", new At().run("+fact(x){i:1; *x{i:i*{_+1}}; !i}; >('10! 'fact(10))"));
    }

    @Test
    public void global() {
        assertEquals("1, 2\n", new At().run("SEED:0; +rnd(){SEED:SEED+1; !SEED}; >(rnd()', 'rnd())"));
    }

    @Test
    public void longForm() {
        assertEquals("1 1\n", new At().run("fun abs(x) { if 0<x { x } else { -x } }; print(abs(-1), ' ', abs(1))"));
        assertEquals("10! 3628800\n", new At().run("fun fact(x) { if x<2 { 1 } else { x * fact(x-1) } } print('10! ', fact(10))"));
        assertEquals("10! 3628800\n", new At().run("fun fact(x) { i: 1; repeat x { i: i * (_ + 1); } return i; } print('10! ', fact(10))"));
    }

    @Test
    public void escapedForm() {
        assertEquals("Joe's taxi\n", new At().run("print('Joe''s taxi')"));
    }

    @Test
    public void comments() {
        assertEquals("Joe's taxi\n", new At().run("'this is a comment', print('Joe''s taxi')"));
    }

    @Test
    public void replMode() {
        At at = new At();
        assertEquals("7", at.run("1+2*3"));
        assertEquals("3.1415", at.run("PI:3.1415"));
        assertEquals("Ok", at.run("? 3 < PI & PI < 4 {'Ok'} : {'Fail'}"));
        assertEquals("3", at.run("{PI|0}"));
    }

    @Test
    public void operatorOverloading() {
        assertEquals("6\n", new At().run("fun ^(a,b) { 20, (a|b) - (a&b) }; print(5 ^ 3)"));
        assertEquals("6\n", new At().run("fun ^(a,b) { 20, !(a|b)-(a&b)}; print(5 ^ 3)"));
        assertEquals("135\n", new At().run("fun **(a, b) { r:a; repeat b{r:r*b}; return r }; print(5 ** 3)"));
        assertEquals("135\n", new At().run("fun **(a,b){r:a;*b{r:r*b};!r}; print(5 ** 3)"));
        assertEquals(-1, -1 % 3);
        assertEquals("1\n-1\n", new At().run("fun%(a,b)(a-((a/b)|0)*b); print(10 % 3),print(-1%3)"));
        assertEquals("110\n", new At().run("fun>=(a,b)((a=b)|(b<a)); print(3>=1,3>=3,0>=2)"));
        assertEquals("10\n", new At().run("fun<>(a,b)(1-(a=b)); print(1<>2,1<>1)"));
    }

    @Test
    public void math() throws IOException {
        String source = readResource("math.at");
        String result = new At().run(source);
        assertEquals("pow(2, 10) 1024.0000000000005\n"
                + "sqrt(2) 1.4142135623730954\n"
                + "sin(0.5) 0.479425538604203\n"
                + "cos(0.5) 0.8775825618903724\n"
                + "tan(0.5) 0.5463024898437907\n"
                + "asin(0.5) 0.5237263053335843\n"
                + "acos(0.5) 1.047070021461312\n"
                + "atan(0.5) 0.4637164938940982\n"
                + "", result);
    }

    private String readResource(String fileName) throws IOException {
        InputStream in = getClass().getResourceAsStream(fileName);
        if (in == null) {
            return "";
        }
        LineNumberReader r = new LineNumberReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder buff = new StringBuilder();
        while (true) {
            String line = r.readLine();
            if (line == null) {
                break;
            }
            buff.append(line).append("\n");
        }
        return buff.toString();
    }

    @Test
    public void random() {
        Random r = new Random(1);
        for (int i = 0; i < 200; i++) {
            if (i % 2000 == 1000) {
                System.out.println(i);
            }
            StringBuilder buff = new StringBuilder();
            int length = r.nextInt(100);
            for (int j = 0; j < length; j++) {
                String n = "+'*%&/(),.ABcd?$_;{}=<>!_ +,;:.&|<>=+-*/.@*?=-(){}[]0139azAZ_,;'>_\\n\n";
                buff.append(n.charAt(r.nextInt(n.length())));
            }
            try {
                new At().run(buff.toString());
            } catch (IllegalStateException e) {

            }
        }
    }

}
