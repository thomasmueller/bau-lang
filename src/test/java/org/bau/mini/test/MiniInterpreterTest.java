package org.bau.mini.test;

import org.bau.mini.Interpreter;
import org.bau.mini.Lexer;
import org.bau.mini.Parser;
import org.bau.mini.Token;
import org.bau.mini.ast.Stmt;
import org.bau.mini.vm.reg.RegBytecode;
import org.bau.mini.vm.reg.RegCompiler;
import org.bau.mini.vm.reg.RegVM;
import org.bau.mini.vm.stack.StackBytecode;
import org.bau.mini.vm.stack.StackCompiler;
import org.bau.mini.vm.stack.StackVM;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import static org.junit.Assert.*;

public class MiniInterpreterTest {
    private Stmt.Program parse(String src) {
        Lexer lx = new Lexer(src);
        List<Token> toks = lx.tokenize();
        Parser p = new Parser(toks);
        return p.parseProgram();
    }

    private String runInterpreter(String src) {
        Stmt.Program prog = parse(src);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream old = System.out;
        System.setOut(new PrintStream(baos));
        try { new Interpreter().setMaxOps(10_000_000L).setCheckEvery(65536).execute(prog); } finally { System.setOut(old); }
        return baos.toString().replace("\r\n", "\n");
    }

    private String runStackVM(String src) {
        Stmt.Program prog = parse(src);
        StackCompiler comp = new StackCompiler();
        StackBytecode bc = comp.compile(prog);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream old = System.out;
        System.setOut(new PrintStream(baos));
        try { new StackVM().run(bc, "main"); } finally { System.setOut(old); }
        return baos.toString().replace("\r\n", "\n");
    }

    private String runRegVM(String src) {
        Stmt.Program prog = parse(src);
        RegCompiler comp = new RegCompiler();
        RegBytecode bc = comp.compile(prog);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream old = System.out;
        System.setOut(new PrintStream(baos));
        try { new RegVM().run(bc, "main"); } finally { System.setOut(old); }
        return baos.toString().replace("\r\n", "\n");
    }

    private void assertAllBackends(String src, String expected) {
        String got = runInterpreter(src);
        assertEquals("Interpreter error: ", expected, got);
        got = runStackVM(src);
        assertEquals("StackVM error: ", expected, got);
        got = runRegVM(src);
        assertEquals("RegVM error: ", expected, got);
    }

    @Test
    public void testRegVMHeapArrayLarge() {
        String src = """
            fun main()
                a: int[1000]
                for i < a.len
                    a[i]: i
                for i < a.len
                    println(a[i])
            """;
        StringBuilder exp = new StringBuilder();
        for (int i = 0; i < 1000; i++) exp.append(i).append('\n');
        assertEquals(exp.toString(), runRegVM(src));
    }

    private void assertParseError(String src) {
        try {
            parse(src);
            fail("Expected parse error but program parsed successfully");
        } catch (RuntimeException ex) {
            // OK
        }
    }

    @Test
    public void testIfElifElse() {
        String src = """
            fun main()
                a: 2
                if a = 0
                    println('zero')
                elif a = 1
                    println('one')
                else
                    println('many')
            """;
        assertAllBackends(src, "many\n");
    }

    @Test
    public void testForLoop() {
        String src = """
            fun main()
                for i < 3
                    println(i)
            """;
        assertAllBackends(src, "0\n1\n2\n");
    }

    @Test
    public void testLoopExit() {
        String src = """
            fun main()
                i: 0
                loop i < 10
                    println(i)
                    if i = 2
                        exit
                    i +: 1
            """;
        assertAllBackends(src, "0\n1\n2\n");
    }

    @Test
    public void testArraysAndLen() {
        String src = """
            fun main()
                a: int[2]
                a[0]: 10
                a[1]: 20
                println(a.len)
                println(a[0])
                println(a[1])
            """;
        assertAllBackends(src, "2\n10\n20\n");
    }

    @Test
    public void testTypesAndConstructor() {
        String src = """
            type point(x int, y int)
            fun main()
                p: point(3, 4)
                println(p.x)
                println(p.y)
                p.x: 7
                println(p.x)
            """;
        assertAllBackends(src, "3\n4\n7\n");
    }

    @Test
    public void testTypesWithNullFields() {
        String src = """
            type node(left node, right node)
            fun main()
                l: node(0, 0)
                n: node(l, 0)
                println(n.left = 0)
                println(n.left <> 0)
                println(n.right = 0)
                println(n.right <> 0)
                println(n = 0)
                println(n <> 0)
            """;
        assertAllBackends(src, "0\n1\n1\n0\n0\n1\n");
    }

    @Test
    public void testBitwiseAndShifts() {
        String src = """
            fun main()
                println(1 and 0)
                println(1 or 0)
                println(5 & 3)
                println(5 | 2)
                println(5 ^ 1)
                println(1 << 3)
                println(8 >> 2)
            """;
        assertAllBackends(src, "0\n1\n1\n7\n4\n8\n2\n");
    }

    @Test
    public void testArithmeticIntAndReal() {
        String src = """
            fun main()
                println(1 + 2)
                println(5 - 3)
                println(2 * 3)
                println(7 / 2)
                println(7 % 2)
                println(1.5 + 2.0)
                println(5.0 - 3)
                println(2 * 2.5)
                println(7.0 / 2)
            """;
        assertAllBackends(src, "3\n2\n6\n3\n1\n3.5\n2.0\n5.0\n3.5\n");
    }

    @Test
    public void testEqualityAndRelational() {
        String src = """
            fun main()
                println(1 = 1)
                println(1 = 2)
                println(1 <> 2)
                println(1 < 2)
                println(2 > 1)
                println(2 <= 2)
                println(2 >= 3)
            """;
        assertAllBackends(src, "1\n0\n1\n1\n1\n1\n0\n");
    }

    @Test
    public void testComparisonRealAndText() {
        String src = """
            fun main()
                println(1.5 = 1.5)
                println(1.5 < 2.0)
                println(2.0 <= 2.0)
                println('a' = 'a')
                println('a' <> 'b')
                println('abc' < 'abd')
            """;
        assertAllBackends(src, "1\n1\n1\n1\n1\n1\n");
    }

    @Test
    public void testFloatAndOrAreCompileErrors() {
        // real and/or int should be a compile-time error in RegCompiler
        String andSrc = """
            fun main()
                r: 0.1
                println(r and 1.0)
            """;
        try { new org.bau.mini.vm.reg.RegCompiler().compile(parse(andSrc)); fail("Expected compile error for real AND"); } catch (RuntimeException ex) {}

        String orSrc = """
            fun main()
                r: 0.0
                println(r or 1.0)
            """;
        try { new org.bau.mini.vm.reg.RegCompiler().compile(parse(orSrc)); fail("Expected compile error for real OR"); } catch (RuntimeException ex) {}
    }

    @Test
    public void testTruthinessRequiresInt_AndOr() {
        // '' and 1 -> compile-time error (non-int operand)
        String s1 = """
            fun main()
                println('' and 1)
            """;
        try { new org.bau.mini.vm.reg.RegCompiler().compile(parse(s1)); fail("Expected compile error: text in and"); } catch (RuntimeException ex) {}

        // (int[0]) or 0 -> compile-time error (non-int operand)
        String s2 = """
            fun main()
                println((int[0]) or 0)
            """;
        try { new org.bau.mini.vm.reg.RegCompiler().compile(parse(s2)); fail("Expected compile error: array in or"); } catch (RuntimeException ex) {}
    }

    @Test
    public void testTruthinessRequiresInt_If() {
        // if 1.0 -> compile-time error
        String s1 = """
            fun main()
                if 1.0
                    println(1)
            """;
        try { new org.bau.mini.vm.reg.RegCompiler().compile(parse(s1)); fail("Expected compile error: if with real"); } catch (RuntimeException ex) {}

        // if 'hello' -> compile-time error
        String s2 = """
            fun main()
                if 'hello'
                    println(1)
            """;
        try { new org.bau.mini.vm.reg.RegCompiler().compile(parse(s2)); fail("Expected compile error: if with text"); } catch (RuntimeException ex) {}
    }

    @Test
    public void testTruthinessAllTypes() {
        String src = """
            fun main()
                println(0 and 1)
                println(1 and 1)
                println(0 or 0)
                println(1 or 0)
            """;
        assertAllBackends(src, "0\n1\n0\n1\n");
    }

    @Test
    public void testPlusEqualAllTypes() {
        String src = """
            fun main()
                i: 1
                i +: 2
                println(i)

                r: 1.5
                r +: 0.5
                println(r)

                a: int[2]
                a[0]: 10
                a[1]: 20
                println(a.len)
                println(a[0])
                println(a[1])
            """;
        assertAllBackends(src, "3\n2.0\n2\n10\n20\n");
    }

    @Test
    public void testArithmeticAndLogicalSingleType() {
        String src = """
            fun main()
                println(2 * 3)
            """;
        assertAllBackends(src, "6\n");
    }

    @Test
    public void testLocalVariable() {
        String src = """
                fun double(n int) int
                    x : n
                    x +: n
                    ret x

                fun main()
                    x : 10
                    println('double = ', double(x))
                    println('x = ', x)
                """;
            assertAllBackends(src, "double = 20\nx = 10\n");
    }

    @Test
    public void testArithmeticAndLogicalAllTypes() {
        String src = """
            fun main()
                println(2 * 3)
                println(7 / 2)
                println(7 % 2)
                println(1 + 2)
                println(1 and 0)
                println(1 or 0)
                println(2.0 * 0.5)
                println(3.0 / 2.0)
                println(0 and 1)
                println(1 or 0)
            """;
        assertAllBackends(src, "6\n3\n1\n3\n0\n1\n1.0\n1.5\n0\n1\n");
    }

    @Test
    public void testNegationIntAndReal() {
        String src = """
            fun main()
                println(-1)
                println(-1.5)
                r: 1.25
                println(-r)
            """;
        assertAllBackends(src, "-1\n-1.5\n-1.25\n");
    }

    @Test
    public void testLenArrayAndText() {
        String src = """
            fun main()
                a: int[3]
                a[0]: 1
                a[1]: 2
                a[2]: 3
                println(a.len)
                println('abc'.len)
            """;
        assertAllBackends(src, "3\n3\n");
    }

    @Test
    public void testGlobalVariablesAllTypes() {
        String src = """
            x: 1
            y: 1.5
            s: 'hi'
            a: int[1]
            a[0]: 1
            println(x)
            println(y)
            println(s)
            println(a.len)
            """;
        assertAllBackends(src, "1\n1.5\nhi\n1\n");
    }

    @Test
    public void testGlobalVariablesIntArray() {
        String src = """
            a: int[1]
            a[0]: 1
            """;
        assertAllBackends(src, "");
    }

    @Test
    public void testArrayAccessSetGetAllTypes() {
        String src = """
            fun main()
                a: int[2]
                a[0]: 10
                a[1]: 20
                println(a[0])
                println(a[1])

                b: real[1]
                b[0]: 3.5
                println(b[0])

                c: text[1]
                c[0]: 'x'
                println(c[0])
            """;
        assertAllBackends(src, "10\n20\n3.5\nx\n");
    }

    @Test
    public void testRetWithAndWithoutValues() {
        String src = """
            fun f() int
                ret 42
            fun g()
                ret
            fun main()
                println(f())
                println(g())
            """;
        // Using g() as an expression should be a parse-time error (void function in expression)
        assertParseError(src);
    }

    @Test
    public void testFunctionCallWithArgs() {
        String src = """
            fun add(a int, b int) int
                ret a + b
            fun main()
                println(add(2, 3))
            """;
        // Interpreter prints 5, StackVM/RegVM too; ensure all match
        assertAllBackends(src, "5\n");
    }

    @Test
    public void testVarargsSingleTrailingArrayParam() {
        String src = """
            fun printAll(xs text[])
                println(xs[0], xs[1])
            fun main()
                printAll('hello ', 'world')
            """;
        assertAllBackends(src, "hello world\n");
    }

    @Test
    public void testVarargsZeroArgsProducesEmptyArray() {
        String src = """
            fun join(xs text[]) text
                ret ''
            fun main()
                println(join(''))
            """;
        assertAllBackends(src, "\n");
    }

    @Test
    public void testVarargsPassthroughWhenArrayProvided() {
        String src = """
            fun printTwo(xs text[])
                println(xs[0], xs[1])
            fun main()
                a: text[2]
                a[0]: 'A'
                a[1]: 'B'
                printTwo(a)
            """;
        assertAllBackends(src, "AB\n");
    }

    @Test
    public void testDivisionByZeroSemantics() {
        String src = """
            fun main()
                println(7 / 0)
                println(-7 / 0)
                println(0 / 0)
                println(7 % 0)
            """;
        String expected = Long.toString(Long.MAX_VALUE) + "\n" +
                Long.toString(Long.MIN_VALUE) + "\n" +
                "0\n0\n";
        assertAllBackends(src, expected);
    }

    @Test
    public void testTextConcatenationWithNumbers() {
        String src = """
            fun main()
                println('count: ', 3)
                println('pi ', 3.5)
            """;
        assertAllBackends(src, "count: 3\npi 3.5\n");
    }

    @Test
    public void testStructsAndFields() {
        String src = """
            type point(x int, y int)
            fun main()
                p1: point(0, 1)
                p2: point(2, 3)
                println(p1.y)
                println(p2.x + p2.y)
            """;
        assertAllBackends(src, "1\n5\n");
    }

    @Test
    public void testNestedLoopsAndBreak() {
        String src = """
            fun main()
                sum: 0
                i: 0
                loop i < 3
                    j: 0
                    loop j < 3
                        sum +: i * j
                        j +: 1
                        if j = 2
                            exit
                    i +: 1
                println(sum)
            """;
        // Accumulates pairs (i,j) with j in {0,1} for i=0,1,2: 0*0+0*1 + 1*0+1*1 + 2*0+2*1 = 0+0 + 0+1 + 0+2 = 3
        assertAllBackends(src, "3\n");
    }

    @Test
    public void testStructUpdateAndAccess() {
        String src = """
            type node(val int, next int)
            fun main()
                n: node(1, -1)
                println(n.val)
                n.val: 7
                println(n.val)
            """;
        assertAllBackends(src, "1\n7\n");
    }

    @Test
    public void testArrayOfStructs_AllBackends() {
        String src = """
            type point(x int, y int)
            fun main()
                a: point[2]
                a[0]: point(1, 2)
                a[1]: point(3, 4)
                println(a.len)
                println(a[0].x)
                println(a[1].y)
                a[0].x: 7
                println(a[0].x)
                a[1]: point(5, 6)
                println(a[1].x)
                println(a[1].y)
            """;
        assertAllBackends(src, "2\n1\n4\n7\n5\n6\n");
    }

    @Test
    public void testNullObjectAccessPanics_AllBackends() {
        String src = """
            type node(next node)
            fun main()
                n: 0
                println(n.next)
            """;
        // Interpreter should throw runtime exception
        try { runInterpreter(src); fail("Interpreter did not panic on null access"); } catch (RuntimeException ex) {}
        try { runStackVM(src); fail("StackVM did not panic on null access"); } catch (RuntimeException ex) {}
        try { runRegVM(src); fail("RegVM did not panic on null access"); } catch (RuntimeException ex) {}
    }

    @Test
    public void testStructEqualsZero_AllBackends() {
        String src = """
            type node(next node)
            fun isNull(n node) int
                if n = 0
                    ret 1
                ret 0
            fun main()
                a: 0
                b: node(0)
                println(isNull(a))
                println(isNull(b))
            """;
        assertAllBackends(src, "1\n0\n");
    }

    @Test
    public void testStructEqualsNonZeroForbidden_CompileError() {
        String src = """
            type node(next node)
            fun main()
                n: node(0)
                if n = 1
                    println(1)
            """;
        // Interpreter is not compiled; check Stack and Reg compilers for compile-time error
        try { new org.bau.mini.vm.stack.StackCompiler().compile(parse(src)); fail("Expected compile error for struct = 1"); } catch (RuntimeException ex) {}
        try { new org.bau.mini.vm.reg.RegCompiler().compile(parse(src)); fail("Expected compile error for struct = 1 (Reg)"); } catch (RuntimeException ex) {}
    }

    @Test
    public void testArrayOfUserTypeZeroInitAllFields() {
        String src = """
            type x(a int, b real, c text, d int[])
            fun main()
                arr: x[3]
                println(arr.len)
                println(arr[0])
                println(arr[1] = 0)
                println(arr[2] <> 0)
            """;
        assertAllBackends(src, "3\n"
                + "0\n"
                + "1\n"
                + "0\n"
                + "");
    }

    @Test
    public void testExitOutsideLoopCompileError() {
        String src = """
            fun main()
                exit
            """;
        Stmt.Program prog = parse(src);
        // Stack VM compile should fail
        try {
            new StackCompiler().compile(prog);
            fail("Expected compile error for exit outside loop (StackVM)");
        } catch (RuntimeException ex) {
            // ok
        }
        // Reg VM compile should fail
        try {
            new org.bau.mini.vm.reg.RegCompiler().compile(prog);
            fail("Expected compile error for exit outside loop (RegVM)");
        } catch (RuntimeException ex) {
            // ok
        }
    }

    @Test
    public void testReturnTypeMismatchCompileError() {
        // ret with value in void function
        String src1 = """
            fun f()
                ret 1
            """;
        Stmt.Program p1 = parse(src1);
        try { new StackCompiler().compile(p1); fail("Expected compile error: void function returns value (StackVM)"); } catch (RuntimeException ex) {}
        try { new org.bau.mini.vm.reg.RegCompiler().compile(p1); fail("Expected compile error: void function returns value (RegVM)"); } catch (RuntimeException ex) {}

        // ret without value in int function
        String src2 = """
            fun g() int
                ret
            """;
        Stmt.Program p2 = parse(src2);
        try { new StackCompiler().compile(p2); fail("Expected compile error: missing return value (StackVM)"); } catch (RuntimeException ex) {}
        try { new org.bau.mini.vm.reg.RegCompiler().compile(p2); fail("Expected compile error: missing return value (RegVM)"); } catch (RuntimeException ex) {}

        // wrong return type
        String src3 = """
            fun h() real
                ret 1
            """;
        Stmt.Program p3 = parse(src3);
        try { new StackCompiler().compile(p3); fail("Expected compile error: wrong return type (StackVM)"); } catch (RuntimeException ex) {}
        try { new org.bau.mini.vm.reg.RegCompiler().compile(p3); fail("Expected compile error: wrong return type (RegVM)"); } catch (RuntimeException ex) {}
    }

    @Test
    public void testCallValidationCompileErrors() {
        // unknown function
        String s1 = """
            fun main()
                foo(1)
            """;
        Stmt.Program p1 = parse(s1);
        try { new StackCompiler().compile(p1); fail("Expected compile error: unknown function (StackVM)"); } catch (RuntimeException ex) {}
        try { new org.bau.mini.vm.reg.RegCompiler().compile(p1); fail("Expected compile error: unknown function (RegVM)"); } catch (RuntimeException ex) {}

        // non-callable value as target
        String s2 = """
            fun main()
                a: 1
                a(2)
            """;
        Stmt.Program p2 = parse(s2);
        try { new StackCompiler().compile(p2); fail("Expected compile error: non-callable target (StackVM)"); } catch (RuntimeException ex) {}
        try { new org.bau.mini.vm.reg.RegCompiler().compile(p2); fail("Expected compile error: non-callable target (RegVM)"); } catch (RuntimeException ex) {}

        // arity mismatch for function
        String s3 = """
            fun f(a int) int
                ret a
            fun main()
                println(f())
            """;
        Stmt.Program p3 = parse(s3);
        try { new StackCompiler().compile(p3); fail("Expected compile error: arity mismatch (StackVM)"); } catch (RuntimeException ex) {}
        try { new org.bau.mini.vm.reg.RegCompiler().compile(p3); fail("Expected compile error: arity mismatch (RegVM)"); } catch (RuntimeException ex) {}

        // arity mismatch for constructor
        String s4 = """
            type p(x int, y int)
            fun main()
                p(1)
            """;
        Stmt.Program p4 = parse(s4);
        try { new StackCompiler().compile(p4); fail("Expected compile error: ctor arity mismatch (StackVM)"); } catch (RuntimeException ex) {}
        try { new org.bau.mini.vm.reg.RegCompiler().compile(p4); fail("Expected compile error: ctor arity mismatch (RegVM)"); } catch (RuntimeException ex) {}
    }

    @Test
    public void testInvalidFieldAccessCompileErrors() {
        // field on non-struct
        String s1 = """
            fun main()
                a: 1
                println(a.x)
            """;
        Stmt.Program p1 = parse(s1);
        try { new StackCompiler().compile(p1); fail("Expected compile error: field on non-struct (StackVM)"); } catch (RuntimeException ex) {}
        try { new org.bau.mini.vm.reg.RegCompiler().compile(p1); fail("Expected compile error: field on non-struct (RegVM)"); } catch (RuntimeException ex) {}

        // unknown field on struct
        String s2 = """
            type p(x int, y int)
            fun main()
                q: p(1,2)
                println(q.z)
            """;
        Stmt.Program p2 = parse(s2);
        try { new StackCompiler().compile(p2); fail("Expected compile error: unknown field (StackVM)"); } catch (RuntimeException ex) {}
        try { new org.bau.mini.vm.reg.RegCompiler().compile(p2); fail("Expected compile error: unknown field (RegVM)"); } catch (RuntimeException ex) {}
    }

    @Test
    public void testInvalidOperandTypesAndUndefinedVarCompileErrors() {
        // invalid arithmetic types
        String a1 = """
            fun main()
                println('x' + 1)
            """;
        Stmt.Program pa1 = parse(a1);
        try { new StackCompiler().compile(pa1); fail("Expected compile error: '+' types (StackVM)"); } catch (RuntimeException ex) {}
        try { new org.bau.mini.vm.reg.RegCompiler().compile(pa1); fail("Expected compile error: '+' types (RegVM)"); } catch (RuntimeException ex) {}

        // logical 'and' accepts truthiness; no compile error expected

        String a3 = """
            fun main()
                println(1 << 'x')
            """;
        Stmt.Program pa3 = parse(a3);
        try { new StackCompiler().compile(pa3); fail("Expected compile error: shift types (StackVM)"); } catch (RuntimeException ex) {}
        try { new org.bau.mini.vm.reg.RegCompiler().compile(pa3); fail("Expected compile error: shift types (RegVM)"); } catch (RuntimeException ex) {}

        // undefined variable
        String u1 = """
            fun main()
                println(x)
            """;
        Stmt.Program pu1 = parse(u1);
        try { new StackCompiler().compile(pu1); fail("Expected compile error: undefined var (StackVM)"); } catch (RuntimeException ex) {}
        try { new org.bau.mini.vm.reg.RegCompiler().compile(pu1); fail("Expected compile error: undefined var (RegVM)"); } catch (RuntimeException ex) {}
    }



}


