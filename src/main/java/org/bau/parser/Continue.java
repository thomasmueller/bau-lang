package org.bau.parser;

import java.util.HashSet;
import java.util.List;

import org.bau.runtime.Memory;

public class Continue implements Statement {
    Expression condition;
    int continuedId;
    List<Statement> autoClose;

    @Override
    public Statement replace(Variable old, Expression with) {
        Continue c = new Continue();
        c.condition = condition == null ? null : condition.replace(old, with);
        return c;
    }

    @Override
    public StatementResult run(Memory m) {
        if (condition != null) {
            long v = condition.eval(m).longValue();
            if (v != 1) {
                return StatementResult.OK;
            }
        }
        if (autoClose != null) {
            StatementResult result = Program.runSequence(m, autoClose);
            if (result == StatementResult.OK) {
                return StatementResult.CONTINUE;
            } else {
                return result;
            }
        }
        return StatementResult.CONTINUE;
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
            for(Statement s : autoClose) {
                buff.append(Statement.indent(s.toC()));
            }
        }
        if (condition != null) {
            buff.append(Statement.indent("goto continue" + continuedId + ";\n"));
            buff.append("}\n");
        } else {
            buff.append("continue;\n");
        }
        if (condition != null) {
            buff.append(Borrow.resetUsedOwned(condition.getUsedOwned()));
        }
        return buff.toString();
    }

    public String toString() {
        return condition != null ? "continue " + condition + "\n" : "continue\n";
    }

}
