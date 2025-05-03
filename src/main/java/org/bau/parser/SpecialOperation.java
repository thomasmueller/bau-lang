package org.bau.parser;

import java.util.HashSet;

import org.bau.runtime.Memory;

public class SpecialOperation implements Statement {

    private final SpecialOperationType type;

    SpecialOperation(SpecialOperationType type) {
        this.type = type;
    }

    @Override
    public Statement replace(Variable old, Expression with) {
        return this;
    }

    @Override
    public StatementResult run(Memory m) {
        return StatementResult.OK;
    }

    @Override
    public void optimize(ProgramContext context) {
    }

    @Override
    public String toC() {
        if (type == SpecialOperationType.ZERO_COUNT_TABLE_GC) {
            return "_zeroCountTableGC();\n";
        }
        throw new IllegalStateException();
    }

    public enum SpecialOperationType {
        ZERO_COUNT_TABLE_GC
    }

    @Override
    public void collectTypes(HashSet<DataType> set, MemoryType memoryType) {
        // nothing
    }

    @Override
    public void used(Program program) {
    }

}
