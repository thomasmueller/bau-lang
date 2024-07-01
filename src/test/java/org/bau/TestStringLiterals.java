package org.bau;

import static org.junit.Assert.assertEquals;

import org.bau.parser.Parser;
import org.bau.parser.StringLiteral;
import org.junit.Test;

public class TestStringLiterals {

    @Test
    public void escape() {
        assertEquals("println('\\n\\t\\'\\\\\\xc3\\xb6')\n"
                + "",
                new Parser("""
                        println('\\n\\t\\'\\\\\\xC3\\xb6')
                        """).parse().toString());
    }

    @Test
    public void rawString() {
        assertEquals("println('Backslash n: \\\\n')\n"
                + "println('Backtick: ` ')\n"
                + "println('Two Backticks: `` ')\n"
                + "",
                new Parser("""
                        println(`Backslash n: \\n`)
                        println(``Backtick: ` ``)
                        println(```Two Backticks: `` ```)
                        
                        """).parse().toString());
    }
    
    @Test
    public void multiLineRawString() {
        assertEquals("println('for i := range(1, 10)\\n  println(i)\\nprintln(``backtick ` ``)')\n"
                + "",
                new Parser("""
                        println(```
                          for i := range(1, 10)
                            println(i)
                          println(``backtick ` ``)
                          ```)
                        """).parse().toString());
    }    
    
    @Test
    public void testUnindentRawMultiLineString() {
        assertEquals("", StringLiteral.unindentRawMultiLineString(""));
        assertEquals("abc", StringLiteral.unindentRawMultiLineString("abc"));
        assertEquals("abc", StringLiteral.unindentRawMultiLineString("\n  abc\n  "));
        assertEquals("0\n 1\n-", StringLiteral.unindentRawMultiLineString("\n  0\n   1\n-\n  "));
    }

}
