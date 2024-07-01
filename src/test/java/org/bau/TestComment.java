package org.bau;

import static org.junit.Assert.assertEquals;

import org.bau.parser.Parser;
import org.junit.Test;

public class TestComment {
    
    @Test
    public void comments() {
        assertEquals("a := 1\n"
                + "a = 2\n"
                + "a = 3\n"
                + "a = 4\n"
                + "a = 5\n"
                + "",
                new Parser("""
                        a := 1
                        # commented line
                        a = 2
                        ## block comment 1
                        comment
                        ##
                        a = 3
                        ### block comment 2
                        ##  more
                        #   more
                        ##  more
                        ###
                        a = 4
                        ## short block comment ## a = 5
                        """).parse().toString());
    }

}
