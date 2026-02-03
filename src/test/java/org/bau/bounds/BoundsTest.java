package org.bau.bounds;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.bau.parser.Parser;
import org.junit.Test;

public class BoundsTest {

    @Test
    public void boundsOk() {
        assertEquals("""
                test()
                """,
                new Parser("""
                        fun test()
                            x : int[10]
                            i := 0..x.len
                            while
                                println(x[i]!)
                                next : i + 1
                                break next >= x.len
                                i = next
                        test()
                        """).parse().toString());
    }

    @Test
    public void boundsNotOk() {
        try {
            new Parser("""
                    fun test()
                        x : int[10]
                        i := 0..x.len
                        while
                            println(x[i]!)
                            next : i + 1
                            break next > x.len
                            i = next
                    test()
                    """).parse();
            fail();
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage(), e.getMessage().startsWith("Can not verify"));
        }
    }

    @Test
    public void boundsNotOk2() {
        try {
            new Parser("""
                    fun test()
                        x : int[10]
                        i := 0..x.len
                        while
                            println(x[i]!)
                            next : i - 1
                            break next >= x.len
                            i = next
                    test()
                    """).parse();
            fail();
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage(), e.getMessage().startsWith("Can not verify"));
        }
    }

}
