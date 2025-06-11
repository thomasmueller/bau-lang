package org.bau.parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.bau.runtime.Memory;

public class While implements Statement {
    ArrayList<Statement> list = new ArrayList<>();
    ArrayList<Statement> listContinue = new ArrayList<>();
    List<Statement> autoClose;
    Expression condition;

    private boolean continueIdUsed;
    private int continueId;

    @Override
    public Statement replace(Variable old, Expression with) {
        While c = new While();
        c.condition = condition.replace(old, with);
        c.list = new ArrayList<Statement>();
        for (Statement s : list) {
            c.list.add(s.replace(old, with));
        }
        return c;
    }


    public void setContinueId(int continueId) {
        this.continueId = continueId;
    }

    int getContinueIdAndMarkUsed() {
        continueIdUsed = true;
        return continueId;
    }

    @Override
    public StatementResult run(Memory m) {
        // TODO this is likely quite slow
        ArrayList<Statement> l2 = new ArrayList<>();
        l2.addAll(list);
        int continuePoint = l2.size();
        l2.addAll(listContinue);
        if (autoClose != null) {
            l2.addAll(autoClose);
        }
        outer:
        while (true) {
            long v = condition.eval(m).longValue();
            if (v != 1) {
                break;
            }
            StatementResult result = Program.runSequence(m, l2, continuePoint);
            if (result == StatementResult.OK) {
                // ok
            } else if (result == StatementResult.BREAK) {
                break outer;
            } else {
                return result;
            }
        }
        return StatementResult.OK;
    }

    @Override
    public void collectTypes(HashSet<DataType> set, MemoryType memoryType) {
        Program.collectTypes(list, set, memoryType);
        Program.collectTypes(listContinue, set, memoryType);
        Program.collectTypes(autoClose, set, memoryType);
    }

    @Override
    public void optimize(ProgramContext context) {
        for (Statement s : list) {
            s.optimize(context);
        }
        for (Statement s : listContinue) {
            s.optimize(context);
        }
        if (autoClose != null) {
            for (Statement s : autoClose) {
                s.optimize(context);
            }
        }
    }

    public String toC() {
        StringBuilder buff = new StringBuilder();
        buff.append("while (" + condition.toC() + ") {\n");
        boolean hasReturn = false;
        for (Statement s : list) {
            if (s instanceof Return) {
                hasReturn = true;
            }
            buff.append(Statement.indent(s.toC()));
        }
        StringBuilder buffContinue = new StringBuilder();
        for (Statement s : listContinue) {
            buffContinue.append(Statement.indent(s.toC()));
        }
        if (!hasReturn && autoClose != null) {
            for (Statement s : autoClose) {
                buffContinue.append(Statement.indent(s.toC()));
            }
        }
        if (continueIdUsed) {
            buff.append(Statement.indent("continue" + continueId + ":;\n"));
        }
        if (buffContinue.length() > 0) {
            buff.append(buffContinue);
        }
        buff.append("}\n");
        return buff.toString();
    }

    public String toString() {
        // TODO this is not correct if continue is used
        StringBuilder buff = new StringBuilder();
        buff.append("while " + condition + "\n");
        for(Statement s : list) {
            buff.append(Statement.indent(s.toString()));
        }
        for(Statement s : listContinue) {
            buff.append(Statement.indent(s.toString()));
        }
        return buff.toString();
    }

    public void autoClose(List<Statement> autoClose) {
        this.autoClose = autoClose;
    }

    @Override
    public void used(Program program) {
        for (Statement s : list) {
            s.used(program);
        }
        for (Statement s : listContinue) {
            s.used(program);
        }
        if (autoClose != null) {
            for (Statement s : autoClose) {
                s.used(program);
            }
        }
        if (condition != null) {
            condition.used(program);
        }
    }

}
