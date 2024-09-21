package org.bau.parser;

import java.util.ArrayList;

import org.bau.runtime.Memory;
import org.bau.runtime.Value;
import org.bau.runtime.Value.ValuePanic;

public class Free implements Statement {

    public static final String INC_USE = "_incUse";
    public static final String DEC_USE = "_decUse";

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
        return decRefCount(val, var.type, m);
    }

    public static StatementResult decRefCount(Value val, DataType type, Memory m) {
        long heapId = val.longValue();
        boolean close = m.decHeap(heapId);
        StatementResult result = StatementResult.OK;
        if (close) {
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

    @Override
    public StatementResult run(Memory m) {
        return decRefCount(var, m);
    }

    @Override
    public String toC(ProgramContext context) {
        if (var.needToDecrementRefCountOnFree()) {
            return DEC_USE + "(" + var.toC() + ", " + var.type().nameC() +");\n";
        } else {
            return "";
        }
    }

    @Override
    public String toString() {
        return "# free " + var.name;
    }

}
