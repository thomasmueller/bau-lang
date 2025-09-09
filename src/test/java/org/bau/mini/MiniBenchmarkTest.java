package org.bau.mini;

import org.bau.mini.ast.Stmt;
import org.bau.mini.vm.reg.RegBytecode;
import org.bau.mini.vm.reg.RegCompiler;
import org.bau.mini.vm.reg.RegVM;
import org.bau.mini.vm.stack.StackBytecode;
import org.bau.mini.vm.stack.StackCompiler;
import org.bau.mini.vm.stack.StackVM;
import org.junit.Test;
import org.bau.perf.Profiler;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import static org.junit.Assert.*;

public class MiniBenchmarkTest {
    private static final long MAX_OPS = 100_000_000L;
    private Object parse(String src) {
        Lexer lx = new Lexer(src);
        List<Token> toks = lx.tokenize();
        Parser p = new Parser(toks);
        return p.parseProgram();
    }

    private String runInterpreter(String src) {
        Stmt.Program prog = (Stmt.Program) parse(src);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream old = System.out;
        System.setOut(new PrintStream(baos));
        long start = System.currentTimeMillis();
        try { new Interpreter().setMaxOps(MAX_OPS).setCheckEvery(65536).execute(prog); } finally { System.setOut(old); }
        long time = System.currentTimeMillis() - start;
        System.out.println("runInterpreter: " + time + " ms");
        return baos.toString().replace("\r\n", "\n");
    }

    private String runStackVM(String src) {
        Stmt.Program prog = (Stmt.Program) parse(src);
        StackCompiler comp = new StackCompiler();
        StackBytecode bc = comp.compile(prog);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream old = System.out;
        System.setOut(new PrintStream(baos));
        long start = System.currentTimeMillis();
        try { new StackVM().setMaxOps(MAX_OPS).run(bc, "main"); } finally { System.setOut(old); }
        long time = System.currentTimeMillis() - start;
        System.out.println("runStackVM: " + time + " ms");
        return baos.toString().replace("\r\n", "\n");
    }

    private String runRegVM(String src) {
        Stmt.Program prog = (Stmt.Program) parse(src);
        RegCompiler comp = new RegCompiler();
        RegBytecode bc = comp.compile(prog);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream old = System.out;
        System.setOut(new PrintStream(baos));
        long start = System.currentTimeMillis();
        Profiler prof = new Profiler().startCollecting();
        try { new RegVM().setMaxOps(MAX_OPS).run(bc, "main"); } finally { System.setOut(old); }
        long time = System.currentTimeMillis() - start;
        System.out.println("runRegVM: " + time + " ms");
        System.out.println(prof.getTop(10));
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
    public void testBinaryTrees() {
        String src = """
        type Node(left Node, right Node)
        fun nodeCount(n Node) int
            res: 1
            if n.left <> 0
                res +: nodeCount(n.left)
            if n.right <> 0
                res +: nodeCount(n.right)
            ret res
        fun buildTree(depth int) Node
            if depth = 0
                ret Node(0, 0)
            ret Node(buildTree(depth - 1), buildTree(depth - 1))
        fun count(depth int) int
            ret nodeCount(buildTree(depth))
        fun stretch(depth int)
            println('stretch tree of depth ', depth, '  check: ', count(depth))
        fun main()
            n: 3
            minDepth: 4
            maxDepth: n
            if minDepth + 2 > n
                maxDepth: minDepth + 2
            stretchDepth: maxDepth + 1
            stretch(stretchDepth)
            longLived: buildTree(maxDepth)
            depth: minDepth
            loop depth <= maxDepth
                iterations: 1 << (maxDepth - depth + minDepth)
                sum: 0
                for i < iterations
                    sum +: count(depth)
                println(iterations, '  trees of depth ', depth, '  check: ', sum)
                depth +: 2
            println('long lived tree of depth ', maxDepth, '  check: ', nodeCount(longLived))
                """;
        String expected = "stretch tree of depth 7  check: 255\n"
                + "64  trees of depth 4  check: 1984\n"
                + "16  trees of depth 6  check: 2032\n"
                + "long lived tree of depth 6  check: 127\n"
                + "";
        assertAllBackends(src, expected);
    }

    @Test
    public void testFannkuchMiniN4() {
        String src = """
            fun fannkuch(n int) int
                perm1: int[n]
                for i < n
                    perm1[i]: i
                perm: int[n]
                count: int[n]
                flips: 0
                checksum: 0
                nperm: 0
                r: n
                loop r > 0
                    loop r > 1
                        count[r - 1]: r
                        r: r - 1
                    for i < n
                        perm[i]: perm1[i]
                    f: 0
                    k: perm[0]
                    loop k <> 0
                        for i < perm.len
                            if 2 * i >= k
                                exit
                            t: perm[i]
                            perm[i]: perm[k - i]
                            perm[k - i]: t
                        k: perm[0]
                        f +: 1
                    if f > flips
                        flips: f
                    if (nperm & 1) = 0
                        checksum: checksum + f
                    else
                        checksum: checksum - f
                    loop 1 = 1
                        if r = n
                            println(checksum)
                            ret flips
                        p0: perm1[0]
                        i: 0
                        loop i < r
                            j: i + 1
                            perm1[i]: perm1[j]
                            i: j
                        perm1[r]: p0
                        count[r]: count[r] - 1
                        if count[r] > 0
                            exit
                        r: r + 1
                    nperm +: 1
                ret flips

            fun main()
                n: 4
                println('Pfannkuchen(', n, ') is ', fannkuch(n))
            """;
        String expected = fannkuchExpectedOutput(4);
        assertAllBackends(src, expected);
    }

    private String fannkuchExpectedOutput(int n) {
        int[] perm1 = new int[n];
        for (int i = 0; i < n; i++) perm1[i] = i;
        int[] perm = new int[n];
        int[] count = new int[n];
        int flips = 0;
        int checksum = 0;
        int nperm = 0;
        int r = n;
        while (r > 0) {
            while (r > 1) { count[r - 1] = r; r = r - 1; }
            System.arraycopy(perm1, 0, perm, 0, n);
            int f = 0;
            int k = perm[0];
            while (k != 0) {
                for (int i = 0; i < perm.length; i++) {
                    if (2 * i >= k) break;
                    int t = perm[i];
                    perm[i] = perm[k - i];
                    perm[k - i] = t;
                }
                k = perm[0];
                f++;
            }
            if (f > flips) flips = f;
            if ((nperm & 1) == 0) checksum += f; else checksum -= f;
            while (true) {
                if (r == n) {
                    return checksum + "\n" + "Pfannkuchen(" + n + ") is " + flips + "\n";
                }
                int p0 = perm1[0];
                int i = 0;
                while (i < r) { int j = i + 1; perm1[i] = perm1[j]; i = j; }
                perm1[r] = p0;
                count[r] = count[r] - 1;
                if (count[r] > 0) break;
                r = r + 1;
            }
            nperm++;
        }
        return checksum + "\n" + "Pfannkuchen(" + n + ") is " + flips + "\n";
    }
}
