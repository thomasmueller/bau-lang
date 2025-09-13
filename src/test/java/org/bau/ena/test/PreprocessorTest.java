package org.bau.ena.test;

import org.junit.Test;

import static org.junit.Assert.*;

import org.bau.ena.Preprocessor;

public class PreprocessorTest {

    @Test
    public void testPlusAssignMacro() {
        String src = String.join("\n",
                "def +:",
                "    @{left}: (@{left}) + (@{rest})",
                "",
                "x +: 10");
        String got = Preprocessor.convert(src);
        String exp = String.join("\n",
                "x: (x) + (10)");
        assertEquals(exp, got.trim());
    }

    @Test
    public void testLiteralAt() {
        String src = String.join("\n",
                "def debugPrint",
                "    println(@{rest.text}, ' = ', (@{rest}))",
                "",
                "x: 10",
                "debugPrint 2 * x");
        String got = Preprocessor.convert(src);
        String exp = String.join("\n",
                "x: 10",
                "println('2 * x', ' = ', (2 * x))");
        assertEquals(exp, got.trim());
    }

    @Test
    public void testForInc() {
        String src =
                  "def +:\n"
                + "    @{left}: (@{left}) + (@{rest})\n"
                + "\n"
                + "def for var, from, until\n"
                + "    if 1\n"
                + "        var: from\n"
                + "        loop var < (until)\n"
                + "            @{body}\n"
                + "            var : var + 1\n"
                + "\n"
                + "for i, 0, 10\n"
                + "    x +: 1\n";
        String got = Preprocessor.convert(src);
        String exp = "if 1\n"
                + "    i: 0\n"
                + "    loop i < (10)\n"
                + "        x: (x) + (1)\n"
                + "        i : i + 1";
        assertEquals(exp, got.trim());
    }

    @Test
    public void testBreakWithCondition() {
        String src = "def break condition\n"
                + "    if condition\n"
                + "        exit\n"
                + "\n"
                + "i: 0\n"
                + "loop 1\n"
                + "    break i > 10\n"
                + "    i +: 1\n"
                + "";
        String got = Preprocessor.convert(src);
        String exp = "i: 0\n"
                + "loop 1\n"
                + "    if i > 10\n"
                + "        exit\n"
                + "    i +: 1";
        assertEquals(exp, got.trim());
    }

    @Test
    public void testLogGreedyMacro() {
        String src = String.join("\n",
                "def log",
                "    if logEnabled",
                "        println('Log: ', @{rest})",
                "",
                "log 'Hello ', name");
        String got = Preprocessor.convert(src);
        String exp = String.join("\n",
                "if logEnabled",
                "    println('Log: ', 'Hello ', name)");
        assertEquals(exp, got.trim());
    }

    @Test
    public void testForBlockMacro() {
        String src = String.join("\n",
                "def for var, from, until",
                "    if 1",
                "        var: from",
                "        loop (var) < (until)",
                "            @{body}",
                "            var +: 1",
                "",
                "for i, 0, 10",
                "    println('hello ', i)",
                "println('done')");
        String got = Preprocessor.convert(src);
        String exp = String.join("\n",
                "if 1",
                "    i: 0",
                "    loop (i) < (10)",
                "        println('hello ', i)",
                "        i +: 1",
                "println('done')");
        assertEquals(exp, got.trim());
    }

    @Test
    public void testAssertMacroTextAndId() {
        String src = String.join("\n",
                "def assert condition",
                "    if 0 = (condition)",
                "        println(@{condition.text})",
                "        _x@{id}: int[0]",
                "        _x@{id}[1]: 1",
                "",
                "i: -1",
                "assert i < 0",
                "println('done')");
        String got = Preprocessor.convert(src);
        assertEquals("\n"
                + "i: -1\n"
                + "if 0 = (i < 0)\n"
                + "    println('i < 0')\n"
                + "    _x0: int[0]\n"
                + "    _x0[1]: 1\n"
                + "println('done')", got);
    }

    @Test
    public void testAssertMacroTextInCondition() {
        String src = String.join("\n",
                "def assert condition",
                "    if 0 = (condition)",
                "        println(@{condition.text})",
                "        _x@{id}: int[0]",
                "        _x@{id}[1]: 1",
                "",
                "i: -1",
                "assert map.contains('id')",
                "println('done')");
        String got = Preprocessor.convert(src);
        assertEquals("\n"
                + "i: -1\n"
                + "if 0 = (map.contains('id'))\n"
                + "    println('map.contains(''id'')')\n"
                + "    _x0: int[0]\n"
                + "    _x0[1]: 1\n"
                + "println('done')", got);
    }

    @Test
    public void testIfTernaryToStmt() {
        String src = String.join("\n",
                "def when condition, trueExpr, falseExpr",
                "    if condition",
                "        @{left} trueExpr",
                "    else",
                "        @{left} falseExpr",
                "",
                "array: int[10]",
                "x: when array.len < 0, 0, array[0]");
        String got = Preprocessor.convert(src);
        assertEquals(""
                + "array: int[10]\n"
                + "if array.len < 0\n"
                + "    x: 0\n"
                + "else\n"
                + "    x: array[0]", got.trim());
    }

    @Test
    public void testMaxMacroWithIdAndLhs() {
        String src = String.join("\n",
                "def max a, b",
                "    _a@{id}: a",
                "    _b@{id}: b",
                "    if _a@{id} > _b@{id}",
                "        @{left} _a@{id}",
                "    else",
                "        @{left} _b@{id}",
                "",
                "a: 10",
                "b: 20",
                "c: max a, b");
        String got = Preprocessor.convert(src);
        assertEquals("\n"
                + "a: 10\n"
                + "b: 20\n"
                + "_a0: a\n"
                + "_b0: b\n"
                + "if _a0 > _b0\n"
                + "    c: _a0\n"
                + "else\n"
                + "    c: _b0", got);
    }

    @Test
    public void testNoSubstitutionInsideText() {
        String got = Preprocessor.convert("def PI\n"
                + "    @{left} 3.1415\n"
                + "\n"
                + "x: 'Hello PI'\n"
                + "y: PI * 2\n"
                + "");
        assertEquals("\n"
                + "x: 'Hello PI'\n"
                + "y: 3.1415 * 2\n"
                + "", got);
    }

    @Test
    public void testNoSubstitutionInsideComment() {
        String got = Preprocessor.convert("def PI\n"
                + "    @{left} 3.1415\n"
                + "\n"
                + "x: PI # PI is 3.1415\n"
                + "");
        assertEquals("\n"
                + "x: 3.1415 # PI is 3.1415\n"
                + "", got);
    }

    @Test
    public void testTextParamSubstitution() {
        String src = String.join("\n",
                "def show x",
                "    println(@{x.text})",
                "",
                "show hello");
        String got = Preprocessor.convert(src);
        assertEquals("println('hello')", got.trim());
    }

    @Test
    public void testEscapeAtSymbol() {
        String src = String.join("\n",
                "def test x",
                "    println('@') @@ x",
                "",
                "test 42");
        String got = Preprocessor.convert(src);
        assertEquals("println('@') @ 42", got.trim());
    }
}
