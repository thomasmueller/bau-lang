package org.bau.parser.stmt;

import java.util.HashSet;

import org.bau.parser.DataType;
import org.bau.parser.MemoryType;
import org.bau.parser.Program;
import org.bau.parser.ProgramContext;
import org.bau.parser.expr.Expression;
import org.bau.parser.expr.StringLiteral;
import org.bau.parser.expr.Variable;
import org.bau.runtime.Memory;

public class NativeCode implements Statement {

    private final String nativeCode;

    public NativeCode(String nativeCode) {
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

    public String format() {
        return "native(" + StringLiteral.escape(nativeCode) + ")";
    }

    @Override
    public void used(Program program) {
    }

    @Override
    public DataType canThrowException() {
        return null;
    }

    @Override
    public void setVariableVersions(String name, int oldVersion, int newVersion) {
    }

    @Override
    public void resolveTypes(Program program) {
    }

}
