package org.bau.parser;

import org.bau.runtime.Memory;

public class Throw implements Statement {
    
    Expression expr;
    
    @Override
    public Throw replace(Variable old, Expression with) {
        Throw c = new Throw();
        c.expr = expr.replace(old, with);
        return c;
    }

    @Override
    public String toC(ProgramContext context) {
        StringBuilder buff = new StringBuilder();
        context.delareList.add(context.function.getExceptionStruct() + " _x;");
        context.needToCatch = context.function.exceptionType;
        String catchLabel = "catch" + context.nextCatchLabel;
        buff.append("_x = exception" + context.function.getExceptionStruct() + "(" + expr.toC() + "); goto " + catchLabel + ";\n");
        return buff.toString();
    }
    
    public String toString() {
        return "throw " + expr;
    }

    @Override
    public boolean run(Memory m) {
        return false;
    }

}
