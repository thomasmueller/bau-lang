package org.bau.ena.test;

import static org.junit.Assert.assertNotNull;

import org.bau.ena.EnaInterpreter;
import org.bau.ena.EnaParser;
import org.bau.ena.ast.Stmt;
import org.junit.Test;

public class ParserTest {
    private Stmt.EnaProgram parse(String src) {
        return EnaParser.parse(src);
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
        Stmt.EnaProgram prog = parse(src);
        assertNotNull(prog);
        EnaInterpreter it = new EnaInterpreter();
        it.setPrintToSystemOut(false);
        it.execute(prog);
    }

    @Test
    public void testSumArrayAndFor() {
        String src = """
            fun sum(x int[]) int
                s: 0
                for i, 0, x.len
                    s +: x[i]
                ret s

            fun main()
                a: int[3]
                a[0]: 1
                a[1]: 2
                a[2]: 3
                println(sum(a))
            """;
        Stmt.EnaProgram prog = parse(src);
        assertNotNull(prog);
        EnaInterpreter it = new EnaInterpreter();
        it.setPrintToSystemOut(false);
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
        Stmt.EnaProgram prog = parse(src);
        assertNotNull(prog);
    }
}


