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
    public boolean run(Memory m) {
        return false;
    }

    @Override
    public String toC(ProgramContext context) {
        return nativeCode;
    }
    
    public String toString() {
        return "native(" + StringLiteral.escape(nativeCode) + ")";
    }

}
