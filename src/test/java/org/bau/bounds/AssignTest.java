package org.bau.bounds;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.bau.parser.Parser;
import org.junit.Test;

public class AssignTest {

    @Test
    public void reassignThisNotOk() {
        try {
            assertEquals("""
                    test()
                    """,
                    new Parser("""
                            type Value
                                x int
                            fun Value test(other Value)
                                this = other
                            """).parse().toString());
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage(), e.getMessage().startsWith("Can not modify constant 'this'"));
        }
    }

}
