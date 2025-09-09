package org.bau.mini.vm.stack;

import java.util.ArrayList;
import java.util.List;

public final class StackBytecode {
    public enum Op {
        PUSHI, PUSHR, PUSHT, LLOAD, LSTORE, GLOAD, GSTORE, ALOAD, ASTORE, NEWARR, LEN, NEWOBJ, GETF, SETF, ADD, SUB,
        MUL, DIV, MOD, SHL, SHR, BAND, BOR, BXOR, AND, OR, CMPEQ, CMPNE, CMPLT, CMPLE, CMPGT, CMPGE, NEG, I2R, I2S, R2S,
        JMP, JMPIF, CALL, RET, PRINT, FORMAT, DUP
    }

    public static final class Insn {
        public final Op op;
        public int a, b;

        public Insn(Op op) {
            this(op, 0, 0);
        }

        public Insn(Op op, int a) {
            this(op, a, 0);
        }

        public Insn(Op op, int a, int b) {
            this.op = op;
            this.a = a;
            this.b = b;
        }

        @Override
        public String toString() {
            return op + (a != 0 ? " " + a : "") + (b != 0 ? "," + b : "");
        }
    }

    public static final class Func {
        public final String name;
        public final List<Insn> code = new ArrayList<>();
        public final int numLocals;

        public Func(String name, int numLocals) {
            this.name = name;
            this.numLocals = numLocals;
        }
    }

    public final List<String> textPool = new ArrayList<>();
    public final List<Double> realPool = new ArrayList<>();
    public final List<String> types = new ArrayList<>();
    public final List<String[]> typeFields = new ArrayList<>();
    public final List<String[]> typeFieldTypes = new ArrayList<>();
    public final List<Func> functions = new ArrayList<>();

    public int addText(String s) {
        textPool.add(s);
        return textPool.size() - 1;
    }

    public int addReal(double d) {
        realPool.add(d);
        return realPool.size() - 1;
    }

    public int addType(String name, String[] fields) {
        types.add(name);
        typeFields.add(fields);
        typeFieldTypes.add(new String[fields.length]);
        return types.size() - 1;
    }

    public int addType(String name, String[] fields, String[] fieldTypes) {
        types.add(name);
        typeFields.add(fields);
        typeFieldTypes.add(fieldTypes);
        return types.size() - 1;
    }
}
