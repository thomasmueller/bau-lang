package org.bau.parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.bau.runtime.Memory;
import org.bau.runtime.Value;

public class If implements Statement {
    Expression condition;
    List<Statement> thenList;
    List<Statement> thenAutoClose;
    List<Statement> elseList;
    List<Statement> elseAutoClose;

    @Override
    public If replace(Variable old, Expression with) {
        If c = new If();
        c.condition = condition.replace(old, with);
        c.thenList = new ArrayList<>();
        for (int j = 0; j < thenList.size(); j++) {
            c.thenList.add(thenList.get(j).replace(old, with));
        }
        c.thenAutoClose = new ArrayList<>();
        for (int j = 0; j < thenAutoClose.size(); j++) {
            c.thenAutoClose.add(thenAutoClose.get(j).replace(old, with));
        }
        if (elseList != null) {
            c.elseList = new ArrayList<>();
            for (int j = 0; j < elseList.size(); j++) {
                c.elseList.add(elseList.get(j).replace(old, with));
            }
            c.elseAutoClose = new ArrayList<>();
            for (int j = 0; j < elseAutoClose.size(); j++) {
                c.elseAutoClose.add(elseAutoClose.get(j).replace(old, with));
            }
        }
        return c;
    }

    @Override
    public StatementResult run(Memory m) {
        List<Statement> list = null;
        List<Statement> ac = null;
        Value value = condition.eval(m);
        if (value == null) {
            // eg. due to an uncompiled function
            return StatementResult.TIMEOUT;
        }
        long v = value.longValue();
        if (v != 0) {
            list = thenList;
            ac = thenAutoClose;
        } else if (elseList != null) {
            list = elseList;
            ac = elseAutoClose;
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
    public void collectTypes(HashSet<DataType> set, MemoryType memoryType) {
        Program.collectTypes(thenList, set, memoryType);
        Program.collectTypes(thenAutoClose, set, memoryType);
        if (elseList != null) {
            Program.collectTypes(elseList, set, memoryType);
            Program.collectTypes(elseAutoClose, set, memoryType);
        }
    }

    @Override
    public void optimize(ProgramContext context) {
        for (Statement s : thenList) {
            s.optimize(context);
        }
        for (Statement s : thenAutoClose) {
            s.optimize(context);
        }
        if (elseList != null) {
            for (Statement s : elseList) {
                s.optimize(context);
            }
            for (Statement s : elseAutoClose) {
                s.optimize(context);
            }        }
    }

    public String toC() {
        StringBuilder buff = new StringBuilder();
        buff.append("if (");
        buff.append(condition.toC()).append(") {\n");
        boolean hasReturn = Program.hasReturn(thenList);
        int catchCount = Program.catchCount(thenList);
        for (int j = 0; j < catchCount; j++) {
            buff.append(Statement.indent("do { do {\n"));
        }
        for (Statement s : thenList) {
            buff.append(Statement.indent(s.toC()));
        }
        if (!hasReturn) {
            for (Statement s : thenAutoClose) {
                buff.append(Statement.indent(s.toC()));
            }
        }
        if (elseList != null) {
            buff.append("} else {\n");
            hasReturn = Program.hasReturn(elseList);
            for (Statement s : elseList) {
                buff.append(Statement.indent(s.toC()));
            }
            if (!hasReturn) {
                for (Statement s : elseAutoClose) {
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
        buff.append(condition.toString()).append("\n");
        for(Statement s : thenList) {
            buff.append(Statement.indent(s.toString()));
        }
        if (elseList != null) {
            buff.append("else\n");
            for (Statement s : elseList) {
                buff.append(Statement.indent(s.toString()));
            }
        }
        return buff.toString();
    }

    @Override
    public void used(Program program) {
        condition.used(program);
        for (Statement s : thenList) {
            s.used(program);
        }
        for (Statement s : thenAutoClose) {
            s.used(program);
        }
        if (elseList != null) {
            for (Statement s : elseList) {
                s.used(program);
            }
            for (Statement s : elseAutoClose) {
                s.used(program);
            }
        }
    }

    @Override
    public void link(FunctionContext functionContext, Statement prev, Statement next, Statement breakTarget, Statement continueTarget) {
        // TODO link last (then and else) with next,
        // break (including nested) with loop next,
        // continue (including nested) with loop head,
        // throw (included nested) with catch
        if (thenList.size() > 0) {
            functionContext.linkStatements(this, thenList.get(0));
            functionContext.linkList(thenList, prev, next, breakTarget, continueTarget);
        }
        if (elseList == null || elseList.size() == 0) {
            functionContext.linkStatements(this, next);
        } else {
            functionContext.linkStatements(this, elseList.get(0));
            functionContext.linkList(elseList, prev, next, breakTarget, continueTarget);
        }
    }

    @Override
    public void printLinks(String indentation, FunctionContext functionContext) {
        functionContext.printLinks(indentation, this);
        functionContext.printLinks(indentation + "    ", thenList);
        if (elseList != null) {
            functionContext.printLinks(indentation + "    ", elseList);
        }
    }

    @Override
    public void setVariableVersions(FunctionContext functionContext, PhiBlock lastPhi) {
        functionContext.setVariableVersions(thenList, lastPhi);
        if (elseList != null) {
            functionContext.setVariableVersions(elseList, lastPhi);
        }
    }

}
