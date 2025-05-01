package org.bau.borrow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.bau.parser.Parser;
import org.junit.Assert;
import org.junit.Test;

public class BorrowTest {

    @Test
    public void movedOk() {
        assertEquals("""
                x : Entry_owned()
                x.key = 1
                x.value = 100
                print(x)
                y : x
                print(y)
                println('end')
                """,
                new Parser("""
                        fun main()
                            x : Entry+()
                            x.key = 1
                            x.value = 100
                            x.print()
                            y : x
                            y.print()
                            println('end')

                        type Entry
                            key int
                            value int

                        fun clear(n Entry+)
                            println('clear ' n.key)

                        fun Entry+ print()
                            println('key: ' key ' value: ' value)
                        """).parse().toString());
    }

    @Test
    public void moveFail() {
        try {
            new Parser("""
                    fun main()
                        x : Entry+()
                        x.key = 1
                        x.value = 100
                        x.print()
                        y : x
                        x.print()
                        println('end')

                    type Entry
                        key int
                        value int

                    fun clear(n Entry+)
                        println('clear ' n.key)

                    fun Entry+ print()
                        println('key: ' key ' value: ' value)
                    """).parse().toC();
            Assert.fail();
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().startsWith("The expression 'x' could be null here."));
        }
    }

    @Test
    public void moveViaCallFail() {
        try {
            new Parser("""
                    fun main()
                        x : Entry+()
                        x.key = 1
                        x.value = 100
                        x.print()
                        clear(x)
                        x.print()
                        println('end')

                    type Entry
                        key int
                        value int

                    fun clear(n Entry+)
                        println('clear ' n.key)

                    fun Entry+ print()
                        println('key: ' key ' value: ' value)
                    """).parse().toC();
            Assert.fail();
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().startsWith("The expression 'x' could be null here."));
        }
    }

    @Test
    public void borrowFailViaMove() {
        try {
            new Parser("""
                    fun main()
                        x : Entry+()
                        x.key = 1
                        x.value = 100
                        y : &x
                        z : x
                        z.print()
                        clear(z)
                        y.print()
                        println('end')

                    type Entry
                        key int
                        value int

                    fun clear(n Entry+)
                        println('clear ' n.key)

                    fun Entry+ print()
                        println('key: ' key ' value: ' value)
                    """).parse().toC();
            Assert.fail();
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().startsWith("Function main 0: borrowing Entry which is freed"));
        }
    }

}
