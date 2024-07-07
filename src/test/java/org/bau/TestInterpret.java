package org.bau;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import org.bau.parser.Parser;
import org.bau.parser.Program;

public class TestInterpret {

    @Test
    public void helloWorld() {
        Program p = new Parser("println('Hello World')").parse();
        assertEquals("Hello World\n", p.run());
    }
    
    @Test
    public void calc() {
        assertEquals("1 million + 1 = 1000001\n", new Parser(
                "println('1 million + 1 = ', 1_000_000 + 1)"
        ).parse().run());
    }
    
    @Test
    public void calc2() {
        assertEquals("1 million + 1 = 1000001\n", new Parser(
                "println('1 million + 1 = ', 1_000_000 + 1,)"
        ).parse().run());
    }    
    
}
