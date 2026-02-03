package org.bau.ena.test;

import org.bau.ena.Parser;
import org.bau.ena.ast.Stmt;
import org.bau.ena.tools.CGenerator;
import org.junit.Test;

import static org.junit.Assert.*;

public class ToCTest {
    @Test
    public void testFibonacciToC() {
        String src = String.join("\n",
            "fun fib(n int) int",
            "    if n <= 1",
            "        ret n",
            "    ret fib(n - 1) + fib(n - 2)",
            "",
            "fun main()",
            "    println(fib(10))");
        Stmt.Program prog = Parser.parse(src);
        CGenerator gen = new CGenerator();
        String c = gen.generate(prog);
        // System.out.println("Generated C:\n" + c);
        assertTrue(c.contains("#include <gc.h>"));
        assertTrue(c.contains("int64_t fib"));
        assertTrue(c.contains("void main_fn("));
        assertTrue(c.contains("fib((n - 1LL))"));
        assertTrue(c.contains("fib((n - 2LL))"));
        assertTrue(c.contains("int main() { GC_init();"));
        assertEquals("#include <gc.h>\n"
                + "#include <stdio.h>\n"
                + "#include <stdint.h>\n"
                + "#include <string.h>\n"
                + "\n"
                + "int64_t fib(int64_t n);\n"
                + "void main_fn();\n"
                + "\n"
                + "int64_t fib(int64_t n) {\n"
                + "    if ((n <= 1LL)) {\n"
                + "    return n;\n"
                + "    }\n"
                + "    return (fib((n - 1LL)) + fib((n - 2LL)));\n"
                + "}\n"
                + "\n"
                + "void main_fn() {\n"
                + "    (void)println(fib(10LL));\n"
                + "}\n"
                + "\n"
                + "int main() { GC_init(); main_fn(); return 0; }\n"
                + "", c);
    }
}


