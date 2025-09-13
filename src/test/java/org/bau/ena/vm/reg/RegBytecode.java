package org.bau.ena.vm.reg;

import java.util.ArrayList;
import java.util.List;

public final class RegBytecode {
    public enum Op {
        NOP, MOV, LOADI, LOADR, LOADS, CONCAT, LOAD, STORE, ALOADI, ASTOREI, ALOADT, ALOADS, ASTORET, ASTORES, ALOADR,
        ASTORER, JMP, JMPIF, CALL, RET, NEWARR, LENI, LENF, LENT, LENS, PRINT, I2R, I2S, R2S, NEWOBJ, GETF, SETF,
        ISNULL, ADD, SUB, MUL, DIV, MOD, AND, OR, BAND, BOR, XOR, SHL, SHR, EQ, NE, LT, LE, GT, GE, NEG, FADD, FSUB,
        FMUL, FDIV, FNEG, FCMP, TCMP
    }

    public static final class Insn {
        public final Op op;
        public final int a, b, c;
        public final int imm;

        private Insn(Op op, int a, int b, int c, int imm) {
            this.op = op;
            this.a = a;
            this.b = b;
            this.c = c;
            this.imm = imm;
        }

        public static Insn nop() {
            return new Insn(Op.NOP, 0, 0, 0, 0);
        }

        public static Insn withA(Insn src, int newA) {
            return new Insn(src.op, newA, src.b, src.c, src.imm);
        }

        public static Insn mov(int rd, int ra) {
            return new Insn(Op.MOV, rd, ra, 0, 0);
        }

        public static Insn loadi(int rd, int v) {
            return new Insn(Op.LOADI, rd, 0, 0, v);
        }

        public static Insn loadr(int rd, int k) {
            return new Insn(Op.LOADR, rd, 0, 0, k);
        }

        public static Insn loads(int rd, int k) {
            return new Insn(Op.LOADS, rd, 0, 0, k);
        }

        // removed generic ALU
        public static Insn concat(int rd, int ra, int rb) {
            return new Insn(Op.CONCAT, rd, ra, rb, 0);
        }

        public static Insn op3(Op op, int rd, int ra, int rb) {
            return new Insn(op, rd, ra, rb, 0);
        }

        public static Insn load(int rd, int addr) {
            return new Insn(Op.LOAD, rd, addr, 0, 0);
        }

        public static Insn store(int addr, int rs) {
            return new Insn(Op.STORE, addr, rs, 0, 0);
        }

        public static Insn aloadi(int rd, int ra, int rb) {
            return new Insn(Op.ALOADI, rd, ra, rb, 0);
        }

        public static Insn astorei(int ra, int rb, int rc) {
            return new Insn(Op.ASTOREI, ra, rb, rc, 0);
        }

        public static Insn aloadt(int rd, int ra, int rb) {
            return new Insn(Op.ALOADT, rd, ra, rb, 0);
        }

        public static Insn aloads(int rd, int ra, int rb) {
            return new Insn(Op.ALOADS, rd, ra, rb, 0);
        }

        public static Insn astoret(int ra, int rb, int rc) {
            return new Insn(Op.ASTORET, ra, rb, rc, 0);
        }

        public static Insn asstores(int ra, int rb, int rc) {
            return new Insn(Op.ASTORES, ra, rb, rc, 0);
        }

        public static Insn aloadr(int rd, int ra, int rb) {
            return new Insn(Op.ALOADR, rd, ra, rb, 0);
        }

        public static Insn astorer(int ra, int rb, int rc) {
            return new Insn(Op.ASTORER, ra, rb, rc, 0);
        }

        public static Insn jmp(int target) {
            return new Insn(Op.JMP, target, 0, 0, 0);
        }

        public static Insn jmpif(int ra, int target) {
            return new Insn(Op.JMPIF, ra, target, 0, 0);
        }

        public static Insn call(int rd, int f, int argc) {
            return new Insn(Op.CALL, rd, f, argc, 0);
        }

        public static Insn ret(int ra) {
            return new Insn(Op.RET, ra, 0, 0, 0);
        }

        public static Insn newarr(int rd, int type, int rlen) {
            return new Insn(Op.NEWARR, rd, type, rlen, 0);
        }

        public static Insn leni(int rd, int ra) {
            return new Insn(Op.LENI, rd, ra, 0, 0);
        }

        public static Insn lenf(int rd, int ra) {
            return new Insn(Op.LENF, rd, ra, 0, 0);
        }

        public static Insn lent(int rd, int ra) {
            return new Insn(Op.LENT, rd, ra, 0, 0);
        }

        public static Insn lens(int rd, int ra) {
            return new Insn(Op.LENS, rd, ra, 0, 0);
        }

        public static Insn print(int ra) {
            return new Insn(Op.PRINT, ra, 0, 0, 0);
        }

        public static Insn i2r(int rd, int ra) {
            return new Insn(Op.I2R, rd, ra, 0, 0);
        }

        public static Insn i2s(int rd, int ra) {
            return new Insn(Op.I2S, rd, ra, 0, 0);
        }

        public static Insn r2s(int rd, int ra) {
            return new Insn(Op.R2S, rd, ra, 0, 0);
        }

        public static Insn newobj(int rd, int typeId, int argc) {
            return new Insn(Op.NEWOBJ, rd, typeId, argc, 0);
        }

        public static Insn getf(int rd, int robj, int fieldIndex) {
            return new Insn(Op.GETF, rd, robj, fieldIndex, 0);
        }

        public static Insn setf(int robj, int fieldIndex, int rval) {
            return new Insn(Op.SETF, robj, fieldIndex, rval, 0);
        }

        public static Insn isnull(int rd, int robj) {
            return new Insn(Op.ISNULL, rd, robj, 0, 0);
        }
    }

    public static final class Func {
        public final String name;
        public final List<Insn> code = new ArrayList<>();
        public final int numRegs;

        public Func(String n, int r) {
            name = n;
            numRegs = r;
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

    // Binary writer for cross-language VM loading
    // Format (big-endian):
    // magic: 'R''B''V''M' (4 bytes), version: int32 (1)
    // textPoolCount: int32; then for each: strlen: int32; bytes (UTF-8)
    // realPoolCount: int32; then for each: double64 (IEEE-754)
    // typeCount: int32; for each type: name(string), fieldCount: int32; for each
    // field: fieldName(string), fieldTypeName(string)
    // funcCount: int32; for each func: name(string), numRegs: int32, codeLen:
    // int32, then codeLen instructions:
    // op:int32 (matches Op.ordinal()), a:int32, b:int32, c:int32, imm:int32
    public void writeTo(java.io.OutputStream os) throws java.io.IOException {
        java.io.DataOutputStream out = new java.io.DataOutputStream(os);
        // magic + version
        out.writeByte('R');
        out.writeByte('B');
        out.writeByte('V');
        out.writeByte('M');
        out.writeInt(1);
        // text pool
        out.writeInt(textPool.size());
        for (String s : textPool)
            writeString(out, s);
        // real pool
        out.writeInt(realPool.size());
        for (Double d : realPool)
            out.writeDouble(d);
        // types
        out.writeInt(types.size());
        for (int i = 0; i < types.size(); i++) {
            writeString(out, types.get(i));
            String[] fields = typeFields.get(i);
            String[] ftypes = typeFieldTypes.get(i);
            out.writeInt(fields.length);
            for (int f = 0; f < fields.length; f++) {
                writeString(out, fields[f]);
                writeString(out, ftypes[f]);
            }
        }
        // functions
        out.writeInt(functions.size());
        for (Func f : functions) {
            writeString(out, f.name);
            out.writeInt(f.numRegs);
            out.writeInt(f.code.size());
            for (Insn ins : f.code) {
                out.writeInt(ins.op.ordinal());
                out.writeInt(ins.a);
                out.writeInt(ins.b);
                out.writeInt(ins.c);
                out.writeInt(ins.imm);
            }
        }
        out.flush();
    }

    private static void writeString(java.io.DataOutputStream out, String s) throws java.io.IOException {
        byte[] bytes = s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }
}
