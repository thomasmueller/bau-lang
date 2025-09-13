package org.bau.ena;

import org.bau.ena.ast.Stmt;
import org.bau.ena.vm.reg.RegBytecode;
import org.bau.ena.vm.reg.RegCompiler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RegCompileTool {
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: RegCompileTool <input.ena> <output.rbvm>");
            System.exit(1);
        }
        String src = Files.readString(Path.of(args[0]));
        Stmt.Program prog = Parser.parse(src);
        RegCompiler comp = new RegCompiler();
        RegBytecode bc = comp.compile(prog);
        comp.writeToFile(bc, Path.of(args[1]));
        System.out.println("Wrote bytecode to " + args[1]);
    }
}
