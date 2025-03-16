package org.bau.parser;

import java.util.HashSet;

import org.bau.runtime.Memory;

public class NativeCode implements Statement {

    private final String nativeCode;

    NativeCode(String nativeCode) {
        this.nativeCode = nativeCode;
    }

    @Override
    public Statement replace(Variable old, Expression with) {
        return this;
    }

    @Override
    public StatementResult run(Memory m) {
        // m.print(new Value.ValueI8Array("native ".getBytes(StandardCharsets.UTF_8)));
        // m.print(new Value.ValueI8Array(nativeCode.getBytes(StandardCharsets.UTF_8)));
        // m.println();
        return StatementResult.OK;
    }

    @Override
    public void collectTypes(HashSet<DataType> set, MemoryType memoryType) {
        // nothing
    }

    public void optimize(ProgramContext context) {
    }

    @Override
    public String toC() {
        return nativeCode;
    }

    public String toString() {
        return "native(" + StringLiteral.escape(nativeCode) + ")";
    }

}
