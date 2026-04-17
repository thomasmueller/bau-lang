package org.bau.modules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;

import org.bau.parser.Parser;
import org.bau.parser.Program;
import org.junit.Test;

public class ModuleTest {

    @Test
    public void illegalModuleImports() {
        try {
            HashMap<String, String> modules = new HashMap<>();
            modules.put("com.acme.Math", """
                    fun square(x float) float
                        return x * x
                    """);
            modules.put("com.other.Math", """
                    fun square(x float) float
                        return (x + 0) * (x + 0)
                    """);
            new Parser(modules, """
                    import com.acme.Math
                    import com.other.Math
                    fun main()
                        println(Math.square(2))
                    """).parse();
            fail();
        } catch (IllegalStateException e) {
            assertEquals("Duplicate import for module alias 'Math'; need to use an alias at line 3:\n"
                    + "fun main()\n"
                    + "^\n", e.getMessage());
        }
    }

    @Test
    public void illegalSymbolImports() {
        try {
            HashMap<String, String> modules = new HashMap<>();
            modules.put("com.acme.Math", """
                    fun square(x float) float
                        return x * x
                    """);
            modules.put("com.other.Math", """
                    fun square(x float) float
                        return (x + 0) * (x + 0)
                    """);
            new Parser(modules, """
                    import com.acme.Math
                        square
                    import com.other.Math: AcmeMath
                        square
                    fun main()
                        println(square(2))
                    """).parse();
            fail();
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage(), e.getMessage().startsWith("Duplicate import for symbol 'square'"));
        }
    }

    @Test
    public void traitInModule() {
        HashMap<String, String> modules = new HashMap<>();
        modules.put("test.Input", """
                trait Input
                    read() int
                type MemoryInput: Input
                    pos int
                    mem i8[]
                fun MemoryInput.read() int
                    result : mem[pos]
                    pos += 1
                    return result
                """);
        Program prog = new Parser(modules, """
                import test.Input
                    Input
                type NullFile : Input
                    size int
                fun NullFile.read() int
                    return 0
                fun main()
                    x : NullFile()
                    y Input := x
                    z : y.read() + 0
                    println(z)
                """).parse();
        String s = prog.run();
        assertEquals("1\n", s);
        // verify that conversion works (this is not really a test)
        prog.toC();
    }
}
