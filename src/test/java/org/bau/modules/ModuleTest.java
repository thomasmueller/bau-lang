package org.bau.modules;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;

import org.bau.parser.Parser;
import org.bau.parser.Program;
import org.junit.Test;

public class ModuleTest {

    @Test
    public void traitInModule() {
        HashMap<String, String> modules = new HashMap<>();
        modules.put("test.Input", """
                module test.Input
                trait Input
                    read() int
                type MemoryInput: Input
                    pos int
                    mem i8[]
                fun MemoryInput read() int
                    result : mem[pos]
                    pos += 1
                    return result
                """);
        Program prog = new Parser(modules, """
                import test.Input
                    Input
                type NullFile : Input
                    size int
                fun NullFile read() int
                    return 0
                fun main()
                    x : NullFile()
                    y Input := x
                    println(y.read())
                """).parse();
        String s = prog.run();
        assertEquals("{size=0}\n", s);
        // verify that conversion works (this is not really a test)
        prog.toC();
    }
}
