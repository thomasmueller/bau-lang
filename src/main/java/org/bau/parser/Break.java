package org.bau.parser;

import java.util.HashSet;
import java.util.List;

import org.bau.runtime.Memory;
import org.bau.runtime.Value;

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
    public StatementResult run(Memory m) {
        if (condition != null) {
            Value val = condition.eval(m);
            if (val == null) {
                return null;
            }
            long v = val.longValue();
            if (v != 1) {
                return StatementResult.OK;
            }
        }
        if (autoClose != null) {
            StatementResult result = Program.runSequence(m, autoClose);
            if (result == StatementResult.OK) {
                return StatementResult.BREAK;
            } else {
                return result;
            }
        }
        return StatementResult.BREAK;
    }

    @Override
    public void collectTypes(HashSet<DataType> set, MemoryType memoryType) {
        Program.collectTypes(autoClose, set, memoryType);
    }

    public void optimize(ProgramContext context) {
    }

    public String toC() {
        StringBuilder buff = new StringBuilder();
        if (condition != null) {
            buff.append("if (" + condition.toC() + ") {\n");
        }
        if (autoClose != null) {
            for (Statement s : autoClose) {
                buff.append(Statement.indent(s.toC()));
            }
        }
        if (condition != null) {
            buff.append(Statement.indent("break;\n"));
            buff.append("}\n");
        } else {
            buff.append("break;\n");
        }
        if (condition != null) {
            buff.append(Borrow.resetUsedOwned(condition.getUsedOwned()));
        }
        return buff.toString();
    }

    public String toString() {
        return condition != null ? "break " + condition + "\n" : "break\n";
    }

    @Override
    public void used(Program program) {
        if (condition != null) {
            condition.used(program);
        }
        if (autoClose != null) {
            for(Statement s : autoClose) {
                s.used(program);
            }
        }
    }

}
