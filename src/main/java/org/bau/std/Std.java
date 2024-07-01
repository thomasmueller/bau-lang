package org.bau.std;

import java.util.ArrayList;
import java.util.Collections;

import org.bau.parser.DataType;
import org.bau.parser.FunctionDefinition;
import org.bau.parser.Parser;
import org.bau.parser.Program;
import org.bau.parser.Variable;

public class Std {
    public static void register(Program prog) {
        DataType i8 = prog.addType(new DataType(null, DataType.I8, 1, true, Collections.emptyList()));
        DataType i16 = prog.addType(new DataType(null, DataType.I16, 2, true, Collections.emptyList()));
        DataType i32 = prog.addType(new DataType(null, DataType.I32, 4, true, Collections.emptyList()));
        DataType i64 = prog.addType(new DataType(null, DataType.INT, 8, true, Collections.emptyList()));
        DataType f32 = prog.addType(new DataType(null, DataType.F32, 4, true, Collections.emptyList()));
        DataType f64 = prog.addType(new DataType(null, DataType.F64, 8, true, Collections.emptyList()));
        prog.addType(new DataType(null, DataType.TYPE, 8, true, Collections.emptyList()));
        
        FunctionDefinition f = new FunctionDefinition();
        f.name = DataType.INT;
        f.parameters.add(new Variable("x", i64));
        f.returnType = i64;
        f.cCode = "return x;\n";
        prog.addFunction(f);

        f = new FunctionDefinition();
        f.name = DataType.I32;
        f.parameters.add(new Variable("x", i32));
        f.returnType = i32;
        f.cCode = "return x;\n";
        prog.addFunction(f);

        f = new FunctionDefinition();
        f.name = DataType.I16;
        f.parameters.add(new Variable("x", i16));
        f.returnType = i16;
        f.cCode = "return x;\n";
        prog.addFunction(f);

        f = new FunctionDefinition();
        f.name = DataType.I8;
        f.parameters.add(new Variable("x", i8));
        f.returnType = i8;
        f.cCode = "return x;\n";
        prog.addFunction(f);

        f = new FunctionDefinition();
        f.name = DataType.F64;
        f.parameters.add(new Variable("x", f64));
        f.returnType = f64;
        f.cCode = "return x;\n";
        prog.addFunction(f);

        f = new FunctionDefinition();
        f.name = DataType.F32;
        f.parameters.add(new Variable("x", f32));
        f.returnType = f32;
        f.cCode = "return x;\n";
        prog.addFunction(f);

        f = new FunctionDefinition();
        f.name = "idiv";
        f.parameters.add(new Variable("a", i64));
        f.parameters.add(new Variable("b", i64));
        f.includes = new ArrayList<>();
        f.includes.add("<limits.h>");
        f.returnType = i64;
        f.cCode = """
                if (b != 0) return a / b;
                if (a == 0) return 0;
                return a > 0 ? LLONG_MAX : LLONG_MIN;
                """;
        prog.addFunction(f);
        
        f = new FunctionDefinition();
        f.name = "imod";
        f.parameters.add(new Variable("a", i64));
        f.parameters.add(new Variable("b", i64));
        f.includes = new ArrayList<>();
        f.includes.add("<limits.h>");
        f.returnType = i64;
        f.cCode = """
                if (b != 0) return a % b;
                if (a == 0) return 0;
                return a > 0 ? LLONG_MAX : LLONG_MIN;
                """;
        prog.addFunction(f);
        
        f = new FunctionDefinition();
        f.name = "shiftLeft";
        f.parameters.add(new Variable("a", i64));
        f.parameters.add(new Variable("b", i64));
        f.includes = new ArrayList<>();
        f.returnType = i64;
        f.cCode = """
                return a << b;
                """;
        prog.addFunction(f);
        
        f = new FunctionDefinition();
        f.name = "shiftRight_int";
        f.parameters.add(new Variable("a", i64));
        f.parameters.add(new Variable("b", i64));
        f.includes = new ArrayList<>();
        f.returnType = i64;
        f.cCode = """
                return ((uint64_t) a) >> b;
                """;
        prog.addFunction(f);

        f = new FunctionDefinition();
        f.name = "shiftRight_i32";
        f.parameters.add(new Variable("a", i32));
        f.parameters.add(new Variable("b", i64));
        f.includes = new ArrayList<>();
        f.returnType = i32;
        f.cCode = """
                return ((uint32_t) a) >> b;
                """;
        prog.addFunction(f);
        
        f = new FunctionDefinition();
        f.name = "shiftRight_i16";
        f.parameters.add(new Variable("a", i16));
        f.parameters.add(new Variable("b", i64));
        f.includes = new ArrayList<>();
        f.returnType = i16;
        f.cCode = """
                return ((uint16_t) a) >> b;
                """;
        prog.addFunction(f);

        f = new FunctionDefinition();
        f.name = "shiftRight_i8";
        f.parameters.add(new Variable("a", i8));
        f.parameters.add(new Variable("b", i64));
        f.includes = new ArrayList<>();
        f.returnType = i8;
        f.cCode = """
                return ((uint8_t) a) >> b;
                """;
        prog.addFunction(f);

        f = new FunctionDefinition();
        f.name = "idx";
        f.parameters.add(new Variable("x", i64));
        f.parameters.add(new Variable("len", i64));
        f.includes = new ArrayList<>();
        f.returnType = i64;
        // when printing to stderr, it's harder to find where it happens
        f.cCode = """
                if (x >= 0 && x < len) return x;
                fprintf(stdout, "Array index %lld is out of bounds for the array length %lld\\n", x, len);
                exit(1);
                """;
        prog.addFunction(f);
        
    }

    public static void registerRange(Program program) {
        if (program.getFunctionIfExists(null, null, "range", 2) != null) {
            return;
        }
        String type = """
            fun range(start int, lessThan int) int
              _ := start
              while _ < lessThan
                return _
                _ += 1
            """;
        Parser parser = new Parser(type);
        Program p = parser.parse();
        program.addFunction(p.getFunction(null, null, "range", 2));
    }
    
    public static void registerUntil(Program program) {
        if (program.getFunctionIfExists(null, null, "until", 1) != null) {
            return;
        }
        String type = """
            fun until(lessThan int) 0 .. lessThan
              if lessThan > 0
                _ := 0 .. lessThan
                while 1 = 1
                  return _
                  next : _ + 1
                  break next >= lessThan
                  _ = next
            """;
        Parser parser = new Parser(type);
        Program p = parser.parse();
        program.addFunction(p.getFunction(null, null, "until", 1));
    }
    
    public static void registerStd(Program program) {
        if (program.getImport("Std") != null) {
            return;
        }
        String stdModule = "org.bau.Std";
        String moduleSource = program.readModule(stdModule);
        Parser p = new Parser(program, stdModule, moduleSource);
        p.parse();
        p.isImport = true;
        ArrayList<String> list = new ArrayList<>();
        list.add("ord");
        program.addImport(stdModule, "Std", list);
    }    

}
