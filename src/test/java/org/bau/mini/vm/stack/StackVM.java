package org.bau.mini.vm.stack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StackVM {
    // Configurable timeout
    private long maxOps = 1_000_000L;
    private int checkEvery = 1 << 16; // 65536

    public StackVM setMaxOps(long maxOps) {
        this.maxOps = maxOps;
        return this;
    }

    public StackVM setCheckEvery(int checkEvery) {
        this.checkEvery = checkEvery <= 0 ? 1 : checkEvery;
        return this;
    }

    public Object run(StackBytecode bc, String entry) {
        Map<String, StackBytecode.Func> nameToFunc = new HashMap<>();
        for (StackBytecode.Func f : bc.functions)
            nameToFunc.put(f.name, f);
        String start = nameToFunc.containsKey(entry) ? entry : (nameToFunc.containsKey("main") ? "main" : "<toplevel>");
        return exec(nameToFunc.get(start), bc);
    }

    private Object exec(StackBytecode.Func f, StackBytecode bc) {
        return exec(f, bc, new Object[f.numLocals], new ArrayList<>());
    }

    @SuppressWarnings("unchecked")
    private Object exec(StackBytecode.Func f, StackBytecode bc, Object[] locals, List<Object> globals) {
        List<Object> stack = new ArrayList<>();
        int pc = 0;
        List<StackBytecode.Insn> code = f.code;
        long ops = 0;
        while (pc < code.size()) {
            StackBytecode.Insn in = code.get(pc++);
            if ((++ops % checkEvery) == 0) {
                if (ops > maxOps)
                    throw new RuntimeException("timeout");
            }
            switch (in.op) {
            case LLOAD -> stack.add(locals[in.a]);
            case LSTORE -> locals[in.a] = stack.remove(stack.size() - 1);
            case PUSHI -> stack.add((long) in.a);
            case PUSHR -> stack.add(bc.realPool.get(in.a));
            case PUSHT -> stack.add(bc.textPool.get(in.a));
            case GLOAD -> stack.add(getGlobal(globals, in.a));
            case GSTORE -> setGlobal(globals, in.a, stack.remove(stack.size() - 1));
            case NEWARR -> {
                Object lv = stack.remove(stack.size() - 1);
                long len = (lv instanceof Long l) ? l
                        : (lv instanceof Double d ? (long) d.doubleValue()
                                : (lv instanceof List<?> al ? al.size() : (lv instanceof String s ? s.length() : 0)));
                int tcode = in.a; // 1=int,2=real,3=text, <0 => struct id -(tid+1)
                List<Object> arr = new ArrayList<>((int) len);
                if (tcode == 1) {
                    for (int i = 0; i < len; i++)
                        arr.add(0L);
                } else if (tcode == 2) {
                    for (int i = 0; i < len; i++)
                        arr.add(0.0);
                } else if (tcode == 3) {
                    for (int i = 0; i < len; i++)
                        arr.add("");
                } else if (tcode < 0) {
                    // Array of user-defined structs: default to null (0) entries
                    for (int i = 0; i < len; i++)
                        arr.add(0L);
                } else {
                    for (int i = 0; i < len; i++)
                        arr.add(0L);
                }
                stack.add(arr);
            }
            case ALOAD -> {
                long idx = toLong(stack.remove(stack.size() - 1));
                Object arr = stack.remove(stack.size() - 1);
                stack.add(((List<?>) arr).get((int) idx));
            }
            case ASTORE -> {
                Object val = stack.remove(stack.size() - 1);
                long idx = toLong(stack.remove(stack.size() - 1));
                Object arr = stack.remove(stack.size() - 1);
                ((List<Object>) arr).set((int) idx, val);
            }
            case LEN -> {
                Object x = stack.remove(stack.size() - 1);
                stack.add(lengthOf(x));
            }
            case NEWOBJ -> {
                int typeId = in.a;
                int arity = in.b;
                Map<String, Object> obj = new HashMap<>();
                String[] fields = bc.typeFields.get(typeId);
                for (int i = arity - 1; i >= 0; i--)
                    obj.put(fields[i], stack.remove(stack.size() - 1));
                stack.add(obj);
            }
            case GETF -> {
                String fname = bc.textPool.get(in.a);
                Object o = stack.remove(stack.size() - 1);
                if (o instanceof Long l && l == 0L)
                    throw new RuntimeException("panic: null object access");
                Map<String, Object> obj = (Map<String, Object>) o;
                stack.add(obj.get(fname));
            }
            case SETF -> {
                String fname = bc.textPool.get(in.a);
                Object o = stack.remove(stack.size() - 1);
                if (o instanceof Long l && l == 0L)
                    throw new RuntimeException("panic: null object access");
                Map<String, Object> obj = (Map<String, Object>) o;
                Object val = stack.remove(stack.size() - 1);
                obj.put(fname, val);
            }
            case ADD -> bin(stack, (a, b) -> addOp(a, b));
            case SUB -> bin(stack, (a, b) -> {
                if (a instanceof Double || b instanceof Double)
                    return toDouble(a) - toDouble(b);
                else
                    return toLong(a) - toLong(b);
            });
            case MUL -> bin(stack, (a, b) -> {
                if (a instanceof Double || b instanceof Double)
                    return toDouble(a) * toDouble(b);
                else
                    return toLong(a) * toLong(b);
            });
            case DIV -> bin(stack, (a, b) -> {
                if (a instanceof Double || b instanceof Double)
                    return toDouble(a) / toDouble(b);
                else
                    return divOp(toLong(a), toLong(b));
            });
            case MOD -> bin(stack, (a, b) -> {
                if (a instanceof Double || b instanceof Double)
                    return toDouble(a) % toDouble(b);
                else
                    return modOp(toLong(a), toLong(b));
            });
            case SHL -> bin(stack, (a, b) -> toLong(a) << toLong(b));
            case SHR -> bin(stack, (a, b) -> toLong(a) >>> toLong(b));
            case BAND -> bin(stack, (a, b) -> toLong(a) & toLong(b));
            case BOR -> bin(stack, (a, b) -> toLong(a) | toLong(b));
            case BXOR -> bin(stack, (a, b) -> toLong(a) ^ toLong(b));
            case AND -> bin(stack, (a, b) -> truth(isTruthy(a) && isTruthy(b)));
            case OR -> bin(stack, (a, b) -> truth(isTruthy(a) || isTruthy(b)));
            case CMPEQ -> bin(stack, (a, b) -> truth(equalsOp(a, b)));
            case CMPNE -> bin(stack, (a, b) -> truth(!equalsOp(a, b)));
            case CMPLT -> bin(stack, (a, b) -> truth(cmpOp(a, b) < 0));
            case CMPLE -> bin(stack, (a, b) -> truth(cmpOp(a, b) <= 0));
            case CMPGT -> bin(stack, (a, b) -> truth(cmpOp(a, b) > 0));
            case CMPGE -> bin(stack, (a, b) -> truth(cmpOp(a, b) >= 0));
            case NEG -> {
                Object x = stack.remove(stack.size() - 1);
                if (x instanceof Double) {
                    stack.add(-((Double) x));
                } else {
                    stack.add(-toLong(x));
                }
            }
            case I2R -> {
                Object x = stack.remove(stack.size() - 1);
                stack.add(toDouble(x));
            }
            case I2S -> {
                Object x = stack.remove(stack.size() - 1);
                stack.add(String.valueOf(toLong(x)));
            }
            case R2S -> {
                Object x = stack.remove(stack.size() - 1);
                stack.add(String.valueOf(toDouble(x)));
            }
            case JMP -> pc = in.a;
            case JMPIF -> {
                Object c = stack.remove(stack.size() - 1);
                if (!isTruthy(c))
                    pc = in.a;
            }
            case PRINT -> {
                Object x = stack.remove(stack.size() - 1);
                if (x instanceof List<?>) {
                    StringBuilder sb = new StringBuilder();
                    for (Object e : (List<?>) x)
                        sb.append(stringify(e));
                    System.out.println(sb.toString());
                } else {
                    System.out.println(x);
                }
            }
            case FORMAT -> {
                Object x = stack.remove(stack.size() - 1);
                if (x instanceof List<?>) {
                    StringBuilder sb = new StringBuilder();
                    for (Object e : (List<?>) x)
                        sb.append(stringify(e));
                    stack.add(sb.toString());
                } else {
                    stack.add(String.valueOf(x));
                }
            }
            case DUP -> {
                Object x = stack.get(stack.size() - 1);
                stack.add(x);
            }
            case CALL -> {
                int argc = in.b;
                // args are on stack: arg0 .. argN-1 (left to right); for simplicity assume no
                // locals
                Object[] args = new Object[argc];
                for (int i = argc - 1; i >= 0; i--)
                    args[i] = stack.remove(stack.size() - 1);
                StackBytecode.Func callee = bc.functions.get(in.a);
                Object[] childLocals = new Object[callee.numLocals];
                for (int i = 0; i < args.length && i < childLocals.length; i++)
                    childLocals[i] = args[i];
                Object ret = exec(callee, bc, childLocals, globals);
                stack.add(ret);
            }
            case RET -> {
                Object ret = stack.isEmpty() ? null : stack.remove(stack.size() - 1);
                return ret;
            }
            }
        }
        return null;
    }

    private interface Bin {
        Object apply(Object a, Object b);
    }

    private void bin(List<Object> st, Bin fn) {
        Object b = st.remove(st.size() - 1), a = st.remove(st.size() - 1);
        st.add(fn.apply(a, b));
    }

    private Object getGlobal(List<Object> g, int idx) {
        while (g.size() <= idx)
            g.add(0L);
        return g.get(idx);
    }

    private void setGlobal(List<Object> g, int idx, Object v) {
        while (g.size() <= idx)
            g.add(0L);
        g.set(idx, v);
    }

    private long toLong(Object v) {
        if (v instanceof Long l)
            return l;
        if (v instanceof Double d)
            return (long) d.doubleValue();
        throw new RuntimeException("invalid int value");
    }

    private double toDouble(Object v) {
        if (v instanceof Double d)
            return d;
        if (v instanceof Long l)
            return l.doubleValue();
        throw new RuntimeException("invalid real value");
    }

    private boolean isTruthy(Object v) {
        return (v instanceof Long l ? l != 0 : (v instanceof Double d ? d != 0.0 : true));
    }

    private Object truth(boolean b) {
        return b ? 1L : 0L;
    }

    private boolean equalsOp(Object a, Object b) {
        if (a instanceof Double || b instanceof Double)
            return toDouble(a) == toDouble(b);
        if (a instanceof String || b instanceof String)
            return String.valueOf(a).equals(String.valueOf(b));
        if (a instanceof Long && b instanceof Long)
            return ((Long) a).longValue() == ((Long) b).longValue();
        // Fallback: identity/value equality without numeric coercion
        return a == b || (a != null && a.equals(b));
    }

    private int cmpOp(Object a, Object b) {
        if (a instanceof Double || b instanceof Double)
            return Double.compare(toDouble(a), toDouble(b));
        if (a instanceof String || b instanceof String)
            return String.valueOf(a).compareTo(String.valueOf(b));
        return Long.compare(toLong(a), toLong(b));
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
        if (x instanceof List)
            return ((List<?>) x).size();
        if (x instanceof String s)
            return s.length();
        return 0;
    }

    private Object addOp(Object a, Object b) {
        if (a instanceof String || b instanceof String)
            return String.valueOf(a) + String.valueOf(b);
        if (a instanceof List || b instanceof List)
            throw new RuntimeException("array concatenation is not supported");
        if (a instanceof Double || b instanceof Double)
            return toDouble(a) + toDouble(b);
        return toLong(a) + toLong(b);
    }

    private String stringify(Object v) {
        return v == null ? "null" : (v instanceof String s ? s : String.valueOf(v));
    }
}
