package org.bau.parser;

import java.util.List;

import org.bau.runtime.Memory;

public class Break implements Statement {
    Expression condition;
    List<Statement> autoClose;
    
    @Override
    public Statement replace(Variable old, Expression with) {
        Break c = new Break();
        c.condition = condition == null ? null : condition.replace(old, with);
        return c;
    }

    @Override
    public boolean run(Memory m) {
        if (condition == null) {
            return true;
        }
        long v = condition.eval(m).longValue();
        if (v != 1) {
            return true;
        }
        return false;
    }

    public String toC(ProgramContext context) {
        StringBuilder buff = new StringBuilder();
        if (condition != null) {
            buff.append("if (" + condition.toC() + ") {\n");
        }
        if (autoClose != null) {
            for(Statement s : autoClose) {
                buff.append(Statement.indent(s.toC(context)));
            }
        }
        if (condition != null) {
            buff.append(Statement.indent("break;\n"));
            buff.append("}\n");
        } else {
            buff.append("break;\n");
        }
        return buff.toString();
    }

    public String toString() {
        return condition != null ? "break " + condition + "\n" : "break\n";
    }

}
