package org.bau.parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.bau.runtime.Memory;
import org.bau.runtime.Value;

public class Catch implements Statement {
    ArrayList<Statement> list = new ArrayList<>();
    Variable var;
    List<Statement> autoClose;

    private String nextSkipLabel;
    private String catchLabel;

    @Override
    public Statement replace(Variable old, Expression with) {
        Catch c = new Catch();
        c.var = (Variable) var.replace(old, with);
        return c;
    }

    public String toString() {
        StringBuilder buff = new StringBuilder();
        buff.append("catch " + var + "\n");
        for(Statement s : list) {
            buff.append(Statement.indent(s.toString()));
        }
        return buff.toString();
    }

    @Override
    public StatementResult run(Memory m) {
        Value val = m.getGlobal(Memory.EXCEPTION);
        if (val == null) {
            return StatementResult.OK;
        }
        m.setLocal(var.name, val);
        m.setGlobal(Memory.EXCEPTION, null);
        return Program.runSequence(m, list);
    }

    @Override
    public void collectTypes(HashSet<DataType> set, MemoryType memoryType) {
        // nothing
    }

    public void optimize(ProgramContext context) {
        nextSkipLabel = "skip" + context.nextSkipLabel++;
        catchLabel = "catch" + context.nextCatchLabel++;
        context.needToCatch = null;
    }

    @Override
    public String toC() {
        StringBuilder buff = new StringBuilder();
        buff.append("goto " + nextSkipLabel + ";\n");
        buff.append(catchLabel + ":;\n");
        buff.append(var.type().toC() + " " + var.nameC() + " = _lastException;\n");
        for(Statement s : list) {
            buff.append(Statement.indent(s.toC()));
        }
        if (autoClose != null) {
            for(Statement s : autoClose) {
                buff.append(Statement.indent(s.toC()));
            }
        }
        buff.append(nextSkipLabel + ":;\n");
        return buff.toString();
    }

    public List<Expression> getUsedOwned() {
        return List.of();
    }

    public void autoClose(List<Statement> autoClose) {
        this.autoClose = autoClose;
    }

    @Override
    public void used(Program program) {
        for (Statement s : list) {
            s.used(program);
        }
        for (Statement s : autoClose) {
            s.used(program);
        }
        var.type().used(program);
    }

}
