package org.bau.parser;

import java.util.HashSet;

import org.bau.runtime.Memory;

public interface Statement {

    enum StatementResult {
        OK,
        BREAK,
        RETURN,
        CONTINUE,
        THROW,
        PANIC,
        TIMEOUT
    }

    public static String indent(String s) {
        if (s.isEmpty()) {
            return s;
        }
        boolean nl = s.endsWith("\n");
        s = s.trim().replaceAll("\n", "\n    ");
        if (nl) {
            s += "\n";
        }
        return "    " + s;
    }

    Statement replace(Variable old, Expression with);

    StatementResult run(Memory m);

    void optimize(ProgramContext context);

    String toC();

    default void setBounds(Solver solver, int depth, boolean loop) {

    }

    void collectTypes(HashSet<DataType> set, MemoryType memoryType);

    void used(Program program);

}
