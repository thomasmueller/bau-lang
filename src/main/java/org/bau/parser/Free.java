package org.bau.parser;

import org.bau.runtime.Memory;

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

    @Override
    public StatementResult run(Memory m) {
        // TODO free up memory
        return StatementResult.OK;
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
