package org.bau.mini;

import org.bau.mini.ast.Stmt;
import org.bau.mini.vm.reg.RegBytecode;
import org.bau.mini.vm.reg.RegCompiler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RegCompileTool {
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: RegCompileTool <input.mini> <output.rbvm>");
            System.exit(1);
        }
        String src = Files.readString(Path.of(args[0]));
        Lexer lx = new Lexer(src);
        Parser p = new Parser(lx.tokenize());
        Stmt.Program prog = p.parseProgram();
        RegCompiler comp = new RegCompiler();
        RegBytecode bc = comp.compile(prog);
        comp.writeToFile(bc, Path.of(args[1]));
        System.out.println("Wrote bytecode to " + args[1]);
    }
}
