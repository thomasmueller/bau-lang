package org.bau.parser;

import java.util.List;

import org.bau.runtime.Memory;
import org.bau.runtime.Value;
import org.bau.runtime.Value.ValueException;
import org.bau.runtime.Value.ValuePanic;

public class Return implements Statement {
    Expression expr;
    List<Statement> autoClose;

    private String exceptionStruct;

    public Return(Expression expr) {
        this.expr = expr;
    }

    @Override
    public Statement replace(Variable old, Expression with) {
        Return c = new Return(expr.replace(old, with));
        return c;
    }

    @Override
    public StatementResult run(Memory m) {
        if (expr == null) {
            return StatementResult.RETURN;
        }
        Value val = expr.eval(m);
        if (val != null) {
            if (val instanceof ValueException) {
                return StatementResult.THROW;
            } else if (val instanceof ValuePanic) {
                return StatementResult.PANIC;
            }
            m.setGlobal(Memory.RESULT, val);
        }
        return StatementResult.RETURN;
    }

    public void optimize(ProgramContext context) {
        if (context.function.exceptionType != null) {
            exceptionStruct = context.function.getExceptionStruct();
        }
    }

    public String toC() {
        StringBuilder buff = new StringBuilder();
        if (autoClose != null) {
            for (Statement s : autoClose) {
                buff.append(Statement.indent(s.toC()));
            }
        }
        if (exceptionStruct != null) {
            buff.append("return ok" + exceptionStruct + "(");
            if (expr != null) {
                buff.append(expr.toC());
            }
            buff.append(");\n");
        } else if (expr != null) {
            buff.append("return " + expr.toC() + ";\n");
        } else {
            buff.append("return;\n");
        }
        return buff.toString();
    }

    public String toString() {
        return expr != null ? "return " + expr + "\n" : "return\n";
    }

}
