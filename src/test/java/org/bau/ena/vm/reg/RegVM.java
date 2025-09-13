package org.bau.ena.vm.reg;

import static org.bau.ena.vm.reg.RegBytecode.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RegVM {
    // Configurable timeout
    private long maxOps = 1_000_000L;
    private int checkEvery = 1 << 16; // 65536

    public RegVM setMaxOps(long maxOps) {
        this.maxOps = maxOps;
        return this;
    }

    public RegVM setCheckEvery(int checkEvery) {
        this.checkEvery = checkEvery <= 0 ? 1 : checkEvery;
        return this;
    }

    // Simple heap for now: separate maps for int arrays and text objects
    private static final class IntArrHandle {
        final int id;

        IntArrHandle(int id) {
            this.id = id;
        }
    }

    private static final class RealArrHandle {
        final int id;

        RealArrHandle(int id) {
            this.id = id;
        }
    }

    private static final class TextArrHandle {
        final int id;

        TextArrHandle(int id) {
            this.id = id;
        }
    }

    private static final class UserTypeArrayHandle {
        final int id;

        UserTypeArrayHandle(int id) {
            this.id = id;
        }
    }

    private static final class TextHandle {
        final int id;

        TextHandle(int id) {
            this.id = id;
        }
    }

    private int nextArrId = 1;
    private int nextTextId = 1;
    private final Map<Integer, long[]> intArrays = new HashMap<>();
    private final Map<Integer, double[]> realArrays = new HashMap<>();
    private final Map<Integer, String[]> textArrays = new HashMap<>();
    private final Map<Integer, Object[][]> userArrays = new HashMap<>();
    private final Map<Integer, String> texts = new HashMap<>();

    public Object run(RegBytecode bc, String entry) {
        Map<String, Func> map = new HashMap<>();
        for (Func f : bc.functions)
            map.put(f.name, f);
        String start = map.containsKey(entry) ? entry : (map.containsKey("main") ? "main" : "<toplevel>");
        return exec(map.get(start), bc);
    }

    private Object exec(Func f, RegBytecode bc) {
        return exec(f, bc, new Object[256], new ArrayList<>());
    }

    private Object exec(Func f, RegBytecode bc, Object[] R, List<Object> globals) {
        int pc = 0;
        long ops = 0;
        while (pc < f.code.size()) {
            Insn in = f.code.get(pc++);
            if ((++ops % checkEvery) == 0) {
                if (ops > maxOps)
                    throw new RuntimeException("timeout");
            }
            switch (in.op) {
            case NOP -> {
                /* no-op */ }
            case MOV -> R[in.a] = R[in.b];
            case LOADI -> R[in.a] = (long) in.imm;
            case LOADR -> R[in.a] = loadReal(bc, in.imm);
            case LOADS -> {
                // Load string into heap and return handle
                String s = loadString(bc, in.imm);
                int tid = nextTextId++;
                texts.put(tid, s);
                R[in.a] = new TextHandle(tid);
            }
            case ADD -> R[in.a] = toLong(R[in.b]) + toLong(R[in.c]);
            case SUB -> R[in.a] = toLong(R[in.b]) - toLong(R[in.c]);
            case MUL -> R[in.a] = toLong(R[in.b]) * toLong(R[in.c]);
            case DIV -> R[in.a] = divOp(toLong(R[in.b]), toLong(R[in.c]));
            case MOD -> R[in.a] = modOp(toLong(R[in.b]), toLong(R[in.c]));
            case AND -> R[in.a] = truth(isTruthy(R[in.b]) && isTruthy(R[in.c]));
            case OR -> R[in.a] = truth(isTruthy(R[in.b]) || isTruthy(R[in.c]));
            case BAND -> R[in.a] = toLong(R[in.b]) & toLong(R[in.c]);
            case BOR -> R[in.a] = toLong(R[in.b]) | toLong(R[in.c]);
            case XOR -> R[in.a] = toLong(R[in.b]) ^ toLong(R[in.c]);
            case SHL -> R[in.a] = toLong(R[in.b]) << toLong(R[in.c]);
            case SHR -> R[in.a] = toLong(R[in.b]) >>> toLong(R[in.c]);
            case EQ -> R[in.a] = truth(toLong(R[in.b]) == toLong(R[in.c]));
            case NE -> R[in.a] = truth(toLong(R[in.b]) != toLong(R[in.c]));
            case LT -> R[in.a] = truth((toLong(R[in.b])) < (toLong(R[in.c])));
            case LE -> R[in.a] = truth((toLong(R[in.b])) <= (toLong(R[in.c])));
            case GT -> R[in.a] = truth((toLong(R[in.b])) > (toLong(R[in.c])));
            case GE -> R[in.a] = truth((toLong(R[in.b])) >= (toLong(R[in.c])));
            case NEG -> R[in.a] = -toLong(R[in.b]);
            case FADD -> R[in.a] = toDouble(R[in.b]) + toDouble(R[in.c]);
            case FSUB -> R[in.a] = toDouble(R[in.b]) - toDouble(R[in.c]);
            case FMUL -> R[in.a] = toDouble(R[in.b]) * toDouble(R[in.c]);
            case FDIV -> R[in.a] = toDouble(R[in.b]) / toDouble(R[in.c]);
            case FCMP -> R[in.a] = (long) Double.compare(toDouble(R[in.b]), toDouble(R[in.c]));
            case TCMP -> {
                TextHandle th1 = (TextHandle) R[in.b];
                TextHandle th2 = (TextHandle) R[in.c];
                String sa = texts.get(th1.id);
                String sb = texts.get(th2.id);
                R[in.a] = (long) sa.compareTo(sb);
            }
            case FNEG -> R[in.a] = -toDouble(R[in.b]);
            case CONCAT -> R[in.a] = concat(R[in.b], R[in.c]);
            case LOAD -> R[in.a] = getGlobal(globals, in.b);
            case STORE -> setGlobal(globals, in.a, R[in.b]);
            case ALOADR -> {
                RealArrHandle rh = (RealArrHandle) R[in.b];
                int idx = (int) toLong(R[in.c]);
                double[] arr = realArrays.get(rh.id);
                R[in.a] = arr[idx];
            }
            case ALOADT -> {
                TextArrHandle th = (TextArrHandle) R[in.b];
                int idx = (int) toLong(R[in.c]);
                String[] arr = textArrays.get(th.id);
                String s = arr[idx];
                int tid = nextTextId++;
                texts.put(tid, s == null ? "" : s);
                R[in.a] = new TextHandle(tid);
            }
            case ALOADS -> {
                UserTypeArrayHandle uh = (UserTypeArrayHandle) R[in.b];
                int idx = (int) toLong(R[in.c]);
                Object[][] arr = userArrays.get(uh.id);
                // Array of structs stores object arrays or 0L for null
                R[in.a] = arr[idx];
            }
            case ALOADI -> {
                IntArrHandle ah = (IntArrHandle) R[in.b];
                if (ah == null)
                    throw new RuntimeException("ALOADI null array handle: b=" + in.b + " pc=" + (pc - 1));
                int idx = (int) toLong(R[in.c]);
                long[] arr = intArrays.get(ah.id);
                R[in.a] = arr[idx];
            }

            case ASTORER -> {
                RealArrHandle rh = (RealArrHandle) R[in.a];
                int idx = (int) toLong(R[in.b]);
                Object val = R[in.c];
                double[] arr = realArrays.get(rh.id);
                arr[idx] = toDouble(val);
            }
            case ASTORET -> {
                TextArrHandle th = (TextArrHandle) R[in.a];
                int idx = (int) toLong(R[in.b]);
                TextHandle vth = (TextHandle) R[in.c];
                String[] arr = textArrays.get(th.id);
                arr[idx] = texts.get(vth.id);
            }
            case ASTORES -> {
                UserTypeArrayHandle uh = (UserTypeArrayHandle) R[in.a];
                int idx = (int) toLong(R[in.b]);
                Object[] v = (Object[]) R[in.c];
                Object[][] arr = userArrays.get(uh.id);
                arr[idx] = v;
            }
            case ASTOREI -> {
                IntArrHandle ah = (IntArrHandle) R[in.a];
                int idx = (int) toLong(R[in.b]);
                Object val = R[in.c];
                long[] arr = intArrays.get(ah.id);
                arr[idx] = toLong(val);
            }
            case JMP -> pc = in.a;
            case JMPIF -> {
                if (isTruthy(R[in.a]))
                    pc = in.b;
            }
            case CALL -> {
                Func callee = bc.functions.get(in.b);
                Object[] child = new Object[256];
                // move evaluated args from top of regs into callee R1..Rargc
                for (int i = 1; i <= in.c; i++)
                    child[i] = R[i];
                Object ret = exec(callee, bc, child, globals);
                R[in.a] = ret;
            }
            case RET -> {
                return in.a == 0 ? null : R[in.a];
            }
            case NEWARR -> {
                int len = (int) toLong(R[in.c]);
                if (in.b == 1) { // int
                    long[] arr = new long[len];
                    int id = nextArrId++;
                    intArrays.put(id, arr);
                    R[in.a] = new IntArrHandle(id);
                } else if (in.b == 2) { // real
                    double[] arr = new double[len];
                    int id = nextArrId++;
                    realArrays.put(id, arr);
                    R[in.a] = new RealArrHandle(id);
                } else if (in.b == 3) { // text
                    String[] arr = new String[len];
                    for (int i = 0; i < len; i++)
                        arr[i] = "";
                    int id = nextArrId++;
                    textArrays.put(id, arr);
                    R[in.a] = new TextArrHandle(id);
                } else if (in.b < 0) { // struct id encoded as -(tid+1)
                    // Array of user-defined structs: initialize entries to null (0L)
                    Object[][] arr = new Object[len][];
                    for (int i = 0; i < len; i++)
                        arr[i] = null; // represent null as null entry
                    int id = nextArrId++;
                    userArrays.put(id, arr);
                    R[in.a] = new UserTypeArrayHandle(id);
                } else {
                    long[] arr = new long[len];
                    int id = nextArrId++;
                    intArrays.put(id, arr);
                    R[in.a] = new IntArrHandle(id);
                }
            }
            case LENI -> {
                IntArrHandle ah = (IntArrHandle) R[in.b];
                long[] a = intArrays.get(ah.id);
                R[in.a] = (long) (a == null ? 0 : a.length);
            }
            case LENF -> {
                RealArrHandle ah = (RealArrHandle) R[in.b];
                double[] a = realArrays.get(ah.id);
                R[in.a] = (long) (a == null ? 0 : a.length);
            }
            case LENT -> {
                Object x = R[in.b];
                if (x instanceof TextHandle th) {
                    String s = texts.get(th.id);
                    R[in.a] = (long) (s == null ? 0 : s.length());
                } else if (x instanceof TextArrHandle ah) {
                    String[] a = textArrays.get(ah.id);
                    R[in.a] = (long) (a == null ? 0 : a.length);
                } else
                    R[in.a] = 0L;
            }
            case LENS -> {
                R[in.a] = lengthOf(R[in.b]);
            }
            case ISNULL -> {
                Object o = R[in.b];
                boolean isNull = (o == null) || (o instanceof Long l && l.longValue() == 0L);
                R[in.a] = truth(isNull);
            }
            case PRINT -> {
                Object v = R[in.a];
                if (v instanceof TextHandle th)
                    System.out.println(texts.get(th.id));
                else
                    System.out.println(v);
            }
            case I2S -> {
                Object vb = R[in.b];
                long iv = (vb instanceof Long l) ? l : (vb instanceof Double d) ? d.longValue() : 0L;
                int id = nextTextId++;
                texts.put(id, Long.toString(iv));
                R[in.a] = new TextHandle(id);
            }
            case R2S -> {
                Object vb = R[in.b];
                double dv = (vb instanceof Double d) ? d : (vb instanceof Long l) ? l.doubleValue() : 0.0;
                int id = nextTextId++;
                texts.put(id, Double.toString(dv));
                R[in.a] = new TextHandle(id);
            }
            case NEWOBJ -> {
                int argc = in.c;
                Object[] obj = new Object[argc];
                for (int i = 0; i < argc; i++)
                    obj[i] = R[i + 1];
                R[in.a] = obj;
            }
            case GETF -> {
                int rd = in.a;
                Object target = R[in.b];
                if (target == null) {
                    R[rd] = 0L;
                    break;
                }
                if (target instanceof Long) {
                    R[rd] = 0L;
                    break;
                }
                Object[] obj = (Object[]) target;
                int fieldIndex = in.c;
                R[rd] = obj[fieldIndex];
            }
            case SETF -> {
                Object target = R[in.a];
                if (target == null || (target instanceof Long))
                    throw new RuntimeException("panic: null object access");
                Object[] obj = (Object[]) target;
                int fieldIndex = in.b;
                Object val = R[in.c];
                obj[fieldIndex] = val;
            }
            case I2R -> R[in.a] = ((Long) R[in.b]).doubleValue();
            // I2S/R2S removed: not used by compiler
            }
        }
        return null;
    }

    private Object loadReal(RegBytecode bc, int k) {
        return bc.realPool.get(k);
    }

    private String loadString(RegBytecode bc, int k) {
        return bc.textPool.get(k);
    }

    // removed old ALU helpers after opcode flattening

    private long toLong(Object v) {
        return (Long) v;
    }

    private double toDouble(Object v) {
        return (Double) v;
    }

    private boolean isTruthy(Object v) {
        if (v instanceof Long vv) {
            return vv != 0;
        }
        throw new IllegalArgumentException("needs to be long");
        // return (v instanceof Long l ? l != 0 : (v instanceof Double d ? d != 0.0 :
        // true));
    }

    private Object truth(boolean b) {
        return b ? 1L : 0L;
    }

    private long divOp(long a, long b) {
        if (b == 0)
            return a > 0 ? Long.MAX_VALUE : a < 0 ? Long.MIN_VALUE : 0;
        return a / b;
    }

    private long modOp(long a, long b) {
        if (b == 0)
            return 0;
        return a % b;
    }

    private long lengthOf(Object x) {
        if (x instanceof IntArrHandle ah) {
            long[] a = intArrays.get(ah.id);
            return a == null ? 0 : a.length;
        }
        if (x instanceof RealArrHandle rh) {
            double[] a = realArrays.get(rh.id);
            return a == null ? 0 : a.length;
        }
        if (x instanceof TextArrHandle th) {
            String[] a = textArrays.get(th.id);
            return a == null ? 0 : a.length;
        }
        if (x instanceof UserTypeArrayHandle uh) {
            Object[][] a = userArrays.get(uh.id);
            return a == null ? 0 : a.length;
        }
        if (x instanceof TextHandle th2) {
            String s = texts.get(th2.id);
            return s == null ? 0 : s.length();
        }
        return 0;
    }

    private Object getGlobal(List<Object> g, int idx) {
        while (g.size() <= idx)
            g.add(null);
        Object v = g.get(idx);
        if (v == null)
            return 0L;
        return v;
    }

    private void setGlobal(List<Object> g, int idx, Object v) {
        while (g.size() <= idx)
            g.add(null);
        g.set(idx, v);
    }

    private Object concat(Object a, Object b) {
        // Text concatenation
        if (a instanceof TextHandle || b instanceof TextHandle || a instanceof String || b instanceof String) {
            String sa = (a instanceof TextHandle th) ? texts.get(th.id) : String.valueOf(a);
            String sb = (b instanceof TextHandle th2) ? texts.get(th2.id) : String.valueOf(b);
            int id = nextTextId++;
            texts.put(id, sa + sb);
            return new TextHandle(id);
        }
        // Fallback to text concat
        String sa = String.valueOf(a), sb = String.valueOf(b);
        int id = nextTextId++;
        texts.put(id, sa + sb);
        return new TextHandle(id);
    }
}
