package org.bau.parser;

import java.util.ArrayList;
import java.util.HashSet;

import org.bau.runtime.Memory;
import org.bau.runtime.Value;
import org.bau.runtime.Value.ValuePanic;

public class Free implements Statement {

    public static final String INC_USE = "_incUse";
    public static final String DEC_USE = "_decUse";
    public static final String INC_USE_STACK = "_incUseStack";
    public static final String DEC_USE_STACK = "_decUseStack";

    private final Variable var;

    Free(Variable var) {
        this.var = var;
    }

    @Override
    public Statement replace(Variable old, Expression with) {
        return new Free((Variable) var.replace(old, with));
    }

    public static StatementResult decRefCount(Variable var, Memory m) {
        Value val;
        if (var.global) {
            val = m.getGlobal(var.name);
            m.setGlobal(var.name, null);
        } else {
            val = m.getLocal(var.name);
            m.setLocal(var.name, null);
        }
        if (val == null) {
            return StatementResult.OK;
        }
        return decRefCount(val, var.type(), m);
    }

    public static StatementResult decRefCount(Value val, DataType type, Memory m) {
        long heapId = val.longValue();
        boolean gc = m.decHeap(heapId);
        StatementResult result = StatementResult.OK;
        if (gc) {
            FunctionDefinition def = type.autoClose;
            if (def != null) {
                m.setLocal("this", val);
                ArrayList<Statement> l2 = new ArrayList<>();
                l2.addAll(def.list);
                l2.addAll(def.autoClose);
                result = Program.runSequence(m, l2);
            }
            if (result == StatementResult.PANIC) {
                return result;
            }
            m.incHeap(heapId);
            if (!m.decHeap(heapId)) {
                String message = "Object re-referenced in the close method";
                ValuePanic v = new ValuePanic(message);
                m.print(v);
                m.println();
                m.setGlobal(Memory.PANIC, v);
                return StatementResult.PANIC;
            }
            m.removeHeap(heapId);
        }
        return result;
    }

    private static StatementResult free(Variable var, Memory m) {
        Value val;
        if (var.global) {
            val = m.getGlobal(var.name);
            m.setGlobal(var.name, null);
        } else {
            val = m.getLocal(var.name);
            m.setLocal(var.name, null);
        }
        if (val == null) {
            return StatementResult.OK;
        }
        return free(val, var.type(), m);
    }

    private static StatementResult free(Value val, DataType type, Memory m) {
        Value.ValueStruct struct = (Value.ValueStruct) val;
        for (Variable f : type.fields) {
            Value v = struct.get(f.name);
            if (f.type().needIncDec()) {
                StatementResult result = decRefCount(v, f.type(), m);
                if (result == StatementResult.PANIC) {
                    return result;
                }
            } else if (f.type().needFree()) {
                StatementResult result = free(v, f.type(), m);
                if (result == StatementResult.PANIC) {
                    return result;
                }
            }
        }
        return StatementResult.OK;
    }

    @Override
    public void collectTypes(HashSet<DataType> set, MemoryType memoryType) {
        if (memoryType == MemoryType.OWNER) {
            if (var.type().memoryType() == MemoryType.OWNER) {
                set.add(var.type());
            }
        }
    }

    public void optimize(ProgramContext context) {
    }

    @Override
    public StatementResult run(Memory m) {
        if (var.type().needIncDec()) {
            return decRefCount(var, m);
        } else if (var.type().needFree()) {
            return free(var, m);
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public String toC() {
        if (var.type().needIncDec()) {
            if (var.type().memoryType() == MemoryType.REF_COUNT) {
                return DEC_USE_STACK + "(" + var.toC() + ", " + var.type().nameC() +");\n";
            } else if (var.type().memoryType() == MemoryType.OWNER) {
                return var.type().nameC() + "_free(" + var.toC() + ");\n";
            } else {
                return "";
            }
        } else if (var.type().needFree()) {
            return var.type().nameC() + "_free(&" + var.toC() + ");\n";
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public String toString() {
        return "# free " + var.name;
    }

    @Override
    public void used(Program program) {
        var.type().used(program);
    }

}
