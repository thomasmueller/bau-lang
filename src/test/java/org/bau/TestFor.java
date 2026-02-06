package org.bau;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.bau.parser.Parser;
import org.junit.Test;

public class TestFor {

    @Test
    public void breakOutsideOfLoop() {
        try {
            new Parser("""
    fun test()
      a := 1
      break
    test()
                    """).parse();
            fail();
        } catch (IllegalStateException e) {
            assertEquals("'break' statement outside of a loop at line 3:\n"
                    + "  break\n"
                    + "       ^", e.getMessage());
        }
    }

    @Test
    public void forLoop() {
        assertEquals("""
loop 1 = 1
    a := 0
    loop a < 10
        println(a)
        a += 1
    break
                """,
                new Parser("""
fun range(from int, to int) int
  _ := from
  loop _ < to
    return _
    _ += 1
for a := range(0, 10)
  println(a)
                        """).parse().toString());
    }

}
