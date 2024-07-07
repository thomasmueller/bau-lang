package org.bau.parser;

import java.util.List;

import org.bau.runtime.Memory;
import org.bau.runtime.Value;
import org.bau.runtime.Value.ValueException;
import org.bau.runtime.Value.ValuePanic;

public class Return implements Statement {
    Expression expr;
    List<Statement> autoClose;

    @Override
    public Statement replace(Variable old, Expression with) {
        Return c = new Return();
        c.expr = expr.replace(old, with);
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

    public String toC(ProgramContext context) {
        StringBuilder buff = new StringBuilder();
        if (autoClose != null) {
            for (Statement s : autoClose) {
                buff.append(Statement.indent(s.toC(context)));
            }
        }
        if (context.function.exceptionType != null) {
            String exceptionStruct = context.function.getExceptionStruct();
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
