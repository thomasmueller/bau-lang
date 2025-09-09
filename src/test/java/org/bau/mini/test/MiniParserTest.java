package org.bau.mini.test;

import org.bau.mini.Interpreter;
import org.bau.mini.Lexer;
import org.bau.mini.Parser;
import org.bau.mini.Token;
import org.bau.mini.ast.Stmt;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class MiniParserTest {
    private Stmt.Program parse(String src) {
        Lexer lx = new Lexer(src);
        List<Token> toks = lx.tokenize();
        // Debug: uncomment to inspect tokens
        // for (Token tk : toks) System.out.println(tk);
        Parser p = new Parser(toks);
        return p.parseProgram();
    }

    @Test
    public void testFactorial() {
        String src = """
            fun fact(x int) int
                if x <= 1
                    ret 1
                ret x * fact(x - 1)

            fun main()
                i: 0
                loop i < 5
                    println(fact(i))
                    i +: 1
            """;
        Stmt.Program prog = parse(src);
        assertNotNull(prog);
        Interpreter it = new Interpreter();
        it.execute(prog);
    }

    @Test
    public void testSumArrayAndFor() {
        String src = """
            fun sum(x int[]) int
                s: 0
                for i < x.len
                    s +: x[i]
                ret s

            fun main()
                a: int[3]
                a[0]: 1
                a[1]: 2
                a[2]: 3
                println(sum(a))
            """;
        Stmt.Program prog = parse(src);
        assertNotNull(prog);
        Interpreter it = new Interpreter();
        it.execute(prog);
    }

    @Test
    public void testParserHandlesComparisonsAndOperations() {
        String src = """
            fun main()
                _: 1 = 1
                _: 1 <> 2
                _: 1 < 2
                _: 1 <= 2
                _: 2 > 1
                _: 2 >= 1
                _: 1 + 2 * 3
                _: 7 % 2
                _: (1 + 2) * 3
                _: 1 or 0 and 1
            """;
        Stmt.Program prog = parse(src);
        assertNotNull(prog);
    }
}


