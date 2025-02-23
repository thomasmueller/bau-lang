package org.bau.parser;

import java.util.ArrayList;
import java.util.List;

import org.bau.runtime.Memory;
import org.bau.runtime.Value;

public class If implements Statement {
    ArrayList<Expression> conditions = new ArrayList<>();
    ArrayList<ArrayList<Statement>> listList = new ArrayList<>();
    ArrayList<List<Statement>> autoClose = new ArrayList<>();

    @Override
    public If replace(Variable old, Expression with) {
        If c = new If();
        c.conditions = new ArrayList<>(conditions);
        for (int i = 0; i < conditions.size(); i++) {
            c.conditions.set(i, c.conditions.get(i).replace(old, with));
        }
        c.listList = new ArrayList<>();
        c.autoClose = new ArrayList<>();
        for (int i = 0; i < listList.size(); i++) {
            ArrayList<Statement> l2 = new ArrayList<>();
            ArrayList<Statement> list = listList.get(i);
            for (int j = 0; j < list.size(); j++) {
                l2.add(list.get(j).replace(old, with));
            }
            c.listList.add(l2);
        }
        for (int i = 0; i < autoClose.size(); i++) {
            ArrayList<Statement> l2 = new ArrayList<>();
            List<Statement> list = autoClose.get(i);
            for (int j = 0; j < list.size(); j++) {
                l2.add(list.get(j).replace(old, with));
            }
            c.autoClose.add(l2);
        }
        return c;
    }

    @Override
    public StatementResult run(Memory m) {
        ArrayList<Statement> list = null;
        List<Statement> ac = null;
        for (int i = 0; i < conditions.size(); i++) {
            Expression cond = conditions.get(i);
            Value value = cond.eval(m);
            if (value == null) {
                throw new IllegalStateException();
            }
            long v = value.longValue();
            if (v != 0) {
                list = listList.get(i);
                ac = autoClose.get(i);
                break;
            }
        }
        if (list == null && listList.size() > conditions.size()) {
            // else
            list = listList.get(listList.size() - 1);
            ac = autoClose.get(listList.size() - 1);
        }
        if (list == null) {
            return StatementResult.OK;
        }
        ArrayList<Statement> l2 = new ArrayList<>();
        l2.addAll(list);
        l2.addAll(ac);
        return Program.runSequence(m, l2);
    }

    @Override
    public void optimize(ProgramContext context) {
        for (int i = 0; i < listList.size(); i++) {
            for (Statement s : listList.get(i)) {
                s.optimize(context);
            }
            for (Statement s : autoClose.get(i)) {
                s.optimize(context);
            }
        }
    }

    public String toC() {
        StringBuilder buff = new StringBuilder();
        buff.append("if (");
        buff.append(conditions.get(0).toC()).append(") {\n");
        for (int i = 0; i < conditions.size(); i++) {
            if (i > 0) {
                buff.append("} else if (");
                buff.append(conditions.get(i).toC()).append(") {\n");
            }
            ArrayList<Statement> list = listList.get(i);
            boolean hasReturn = false;
            for (Statement s : list) {
                if (s instanceof Return) {
                    hasReturn = true;
                }
                buff.append(Statement.indent(s.toC()));
            }
            if (!hasReturn) {
                List<Statement> autoCloseList = autoClose.get(i);
                for (Statement s : autoCloseList) {
                    buff.append(Statement.indent(s.toC()));
                }
            }
        }
        if (listList.size() > conditions.size()) {
            buff.append("} else {\n");
            ArrayList<Statement> list = listList.get(listList.size() - 1);
            boolean hasReturn = false;
            for (Statement s : list) {
                if (s instanceof Return) {
                    hasReturn = true;
                }
                buff.append(Statement.indent(s.toC()));
            }
            if (!hasReturn) {
                List<Statement> autoCloseList = autoClose.get(listList.size() - 1);
                for (Statement s : autoCloseList) {
                    buff.append(Statement.indent(s.toC()));
                }
            }
        }
        buff.append("}\n");
        return buff.toString();
    }

    public String toString() {
        StringBuilder buff = new StringBuilder();
        buff.append("if ");
        buff.append(conditions.get(0).toString()).append("\n");
        for (int i = 0; i < conditions.size(); i++) {
            if (i > 0) {
                buff.append("elif ");
                buff.append(conditions.get(i).toString()).append("\n");
            }
            ArrayList<Statement> list = listList.get(i);
            for(Statement s : list) {
                buff.append(Statement.indent(s.toString()));
            }
        }
        if (listList.size() > conditions.size()) {
            buff.append("else\n");
            ArrayList<Statement> list = listList.get(listList.size() - 1);
            for(Statement s : list) {
                buff.append(Statement.indent(s.toString()));
            }
        }
        return buff.toString();
    }

    public void autoClose(List<Statement> autoClose) {
        this.autoClose.add(autoClose);
    }

}
