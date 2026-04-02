package org.bau.std;

import java.util.ArrayList;

import org.bau.parser.DataType;
import org.bau.parser.FullName;
import org.bau.parser.FunctionDefinition;
import org.bau.parser.Parser;
import org.bau.parser.Program;
import org.bau.parser.expr.Variable;
import org.bau.parser.stmt.Return;
import org.bau.runtime.Memory;
import org.bau.runtime.Value;

public class Std {

    private final static boolean ARRAY_BOUND_CHECK = true;

    public static void register(Program prog) {
        DataType i8 = prog.addType(DataType.I8_TYPE);
        DataType i16 = prog.addType(DataType.I16_TYPE);
        DataType i32 = prog.addType(DataType.I32_TYPE);
        DataType i64 = prog.addType(DataType.INT_TYPE);
        DataType f32 = prog.addType(DataType.F32_TYPE);
        DataType f64 = prog.addType(DataType.FLOAT_TYPE);
        prog.addType(DataType.TYPE_TYPE);

        FunctionDefinition f = new FunctionDefinition(new FullName("", DataType.INT), 0);
        Variable var = new Variable("x", DataType.INT_TYPE);
        f.parameters.add(var);
        f.returnType = i64;
        f.constExpr = true;
        f.list.add(new Return(var));
        prog.addFunction(f);

        f = new FunctionDefinition(new FullName("", DataType.I32), 0);
        var = new Variable("x", DataType.INT_TYPE);
        f.parameters.add(var);
        f.returnType = i32;
        f.constExpr = true;
        f.list.add(new Return(var));
        prog.addFunction(f);

        f = new FunctionDefinition(new FullName("", DataType.I16), 0);
        var = new Variable("x", DataType.INT_TYPE);
        f.parameters.add(var);
        f.returnType = i16;
        f.constExpr = true;
        f.list.add(new Return(var));
        prog.addFunction(f);

        f = new FunctionDefinition(new FullName("", DataType.I8), 0);
        var = new Variable("x", DataType.INT_TYPE);
        f.parameters.add(var);
        f.returnType = i8;
        f.constExpr = true;
        f.list.add(new Return(var));
        prog.addFunction(f);

        f = new FunctionDefinition(new FullName("", DataType.FLOAT), 0);
        var = new Variable("x", f64);
        f.parameters.add(var);
        f.returnType = f64;
        f.constExpr = true;
        f.list.add(new Return(var));
        prog.addFunction(f);

        f = new FunctionDefinition(new FullName("", DataType.F32), 0);
        var = new Variable("x", f64);
        f.parameters.add(var);
        f.returnType = f32;
        f.constExpr = true;
        f.list.add(new Return(var));
        prog.addFunction(f);

        f = new FunctionDefinition(new FullName("", "idiv"), 0);
        f.parameters.add(new Variable("a", i64));
        f.parameters.add(new Variable("b", i64));
        f.includes = new ArrayList<>();
        f.includes.add("<limits.h>");
        f.returnType = i64;
        f.cCode = "if (b != 0) return a / b;\n"
                + "if (a == 0) return 0;\n"
                + "return a > 0 ? LLONG_MAX : LLONG_MIN;\n";
        prog.addFunction(f);

        f = new FunctionDefinition(new FullName("", "imod"), 0);
        f.parameters.add(new Variable("a", i64));
        f.parameters.add(new Variable("b", i64));
        f.includes = new ArrayList<>();
        f.includes.add("<limits.h>");
        f.returnType = i64;
        f.cCode = "if (b != 0) return a % b;\n"
                + "return 0;\n";
        prog.addFunction(f);

        f = new FunctionDefinition(new FullName("", "shiftLeft"), 0);
        f.parameters.add(new Variable("a", i64));
        f.parameters.add(new Variable("b", i64));
        f.includes = new ArrayList<>();
        f.returnType = i64;
        f.cCode = "return a << b;\n";
        prog.addFunction(f);

        f = new FunctionDefinition(new FullName("", "shiftRight_int"), 0);
        f.parameters.add(new Variable("a", i64));
        f.parameters.add(new Variable("b", i64));
        f.includes = new ArrayList<>();
        f.returnType = i64;
        f.cCode = "return ((uint64_t) a) >> b;\n";
        prog.addFunction(f);

        f = new FunctionDefinition(new FullName("", "shiftRight_i32"), 0);
        f.parameters.add(new Variable("a", i32));
        f.parameters.add(new Variable("b", i64));
        f.includes = new ArrayList<>();
        f.returnType = i32;
        f.cCode = "return ((uint32_t) a) >> b;\n";
        prog.addFunction(f);

        f = new FunctionDefinition(new FullName("", "shiftRight_i16"), 0);
        f.parameters.add(new Variable("a", i16));
        f.parameters.add(new Variable("b", i64));
        f.includes = new ArrayList<>();
        f.returnType = i16;
        f.cCode = "return ((uint16_t) a) >> b;\n";
        prog.addFunction(f);

        f = new FunctionDefinition(new FullName("", "shiftRight_i8"), 0);
        f.parameters.add(new Variable("a", i8));
        f.parameters.add(new Variable("b", i64));
        f.includes = new ArrayList<>();
        f.returnType = i8;
        f.cCode = "return ((uint8_t) a) >> b;\n";
        prog.addFunction(f);

        f = new FunctionDefinition(new FullName("", "idx"), 0);
        f.parameters.add(new Variable("x", i64));
        f.parameters.add(new Variable("len", i64));
        f.includes = new ArrayList<>();
        f.returnType = i64;
        if (ARRAY_BOUND_CHECK) {
            // when printing to stderr, it's harder to find where it happens
            f.cCode = "if (x >= 0 && x < len) return x;\n"
                    + "return arrayOutOfBounds(x, len);\n";
        } else {
            f.cCode = "return x;\n";
        }
        prog.addFunction(f);
    }

    public static void registerRange(Program program) {
        if (program.getFunctionIfExists(null, "", "range", 2) != null) {
            return;
        }
        String type = "fun range(start int, lessThan int) int\n"
                + "  _ := start\n"
                + "  loop _ < lessThan\n"
                + "    return _\n"
                + "    _ += 1";
        Parser parser = new Parser(type);
        Program p = parser.parse();
        program.addFunction(p.getFunction(null, "", "range", 2));
    }

    public static void registerUntil(Program program) {
        if (program.getFunctionIfExists(null, "", "until", 1) != null) {
            return;
        }
        String type = "fun until(lessThan int) 0 .. lessThan\n"
                + "  if lessThan > 0\n"
                + "    _ := 0 .. lessThan\n"
                + "    loop\n"
                + "      return _\n"
                + "      _next : _ + 1\n"
                + "      break _next >= lessThan\n"
                + "      _ = _next";
        Parser parser = new Parser(type);
        Program p = parser.parse();
        program.addFunction(p.getFunction(null, "", "until", 1));
    }

    public static void registerStd(String module, Program program) {
        String stdModule = "org.bau.Std";
        if (program.getSourceFile(stdModule) != null) {
            return;
        }
        ArrayList<String> list = new ArrayList<>();
        // prevent recursion
        program.addImport("", stdModule, "Std", list);
        String moduleSource = program.readModule(stdModule);
        Parser p = new Parser(program, stdModule, moduleSource, 0);
        // disable the scan phase -- otherwise we would register recursively
        p.setScanPhase(false);
        p.parse();
        p.isImport = true;
        list.add("ord");
        list.add("convertIntToI8Array");
        list.add("appendValue");
        program.addSourceFile(stdModule, "");
        program.addImport(module, stdModule, "Std", list);
    }

    public static Value eval(String name, Memory m) {
        switch (name) {
        case DataType.INT:
            return new Value.ValueInt(m.getLocal("x").longValue());
        case DataType.I32:
            return new Value.ValueI32(m.getLocal("x").intValue());
        case DataType.I16:
            return new Value.ValueI16((short) m.getLocal("x").intValue());
        case DataType.I8:
            return new Value.ValueI8((byte) m.getLocal("x").intValue());
        case DataType.FLOAT:
        case DataType.F32:
            return new Value.ValueFloat(m.getLocal("x").get().doubleValue());
        }
        throw new IllegalStateException();
    }

}
