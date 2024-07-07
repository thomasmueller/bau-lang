package org.bau.parser;

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
    public String toC(ProgramContext context) {
        return nativeCode;
    }
    
    public String toString() {
        return "native(" + StringLiteral.escape(nativeCode) + ")";
    }

}
