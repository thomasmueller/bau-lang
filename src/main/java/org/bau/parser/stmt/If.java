package org.bau.parser.stmt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.bau.parser.BasicBlock;
import org.bau.parser.DataType;
import org.bau.parser.FunctionContext;
import org.bau.parser.MemoryType;
import org.bau.parser.Program;
import org.bau.parser.ProgramContext;
import org.bau.parser.expr.Expression;
import org.bau.parser.expr.Variable;
import org.bau.runtime.Memory;
import org.bau.runtime.Value;

public class If implements Statement {

    public Expression condition;
    public List<Statement> thenList;
    public List<Statement> thenAutoClose;
    public List<Statement> elseList;
    public List<Statement> elseAutoClose;

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

    public String format() {
        StringBuilder buff = new StringBuilder();
        buff.append("if ");
        buff.append(condition.format()).append("\n");
        for(Statement s : thenList) {
            buff.append(Statement.indent(s.format()));
        }
        if (elseList != null) {
            buff.append("else\n");
            for (Statement s : elseList) {
                buff.append(Statement.indent(s.format()));
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
    public BasicBlock linkBasicBlocks(FunctionContext functionContext, BasicBlock current, BasicBlock breakTarget,
            BasicBlock continueTarget) {
        functionContext.linkBasicBlocks(this, current);
        if (thenList.isEmpty() && elseList.isEmpty()) {
            return current;
        }
        BasicBlock after = functionContext.newBasicBlock();
        if (!thenList.isEmpty()) {
            BasicBlock t = functionContext.newBasicBlock();
            current.addSuccessor(t);
            t = functionContext.linkBasicBlocks(thenList, t, breakTarget, continueTarget);
            t.addSuccessor(after);
        }
        if (elseList != null && !elseList.isEmpty()) {
            BasicBlock e = functionContext.newBasicBlock();
            current.addSuccessor(e);
            e = functionContext.linkBasicBlocks(elseList, e, breakTarget, continueTarget);
            e.addSuccessor(after);
        }
        current.addSuccessor(after);
        return after;
    }

    @Override
    public void setVariableVersions(FunctionContext functionContext, BasicBlock basicBlock) {
        if (condition != null) {
            condition.setVariableVersions(functionContext, basicBlock);
        }
    }

    @Override
    public void setVariableVersions(String name, int oldVersion, int newVersion) {
        if (condition != null) {
            condition.setVariableVersions(name, oldVersion, newVersion);
        }
    }

    @Override
    public DataType canThrowException() {
        if (condition == null) {
            return null;
        }
        return condition.canThrowException();
    }

    @Override
    public List<Variable> getDeclaredVariables() {
        ArrayList<Variable> result = new ArrayList<>();
        for (Statement s : thenList) {
            result.addAll(s.getDeclaredVariables());
        }
        if (elseList != null) {
            for (Statement s : elseList) {
                result.addAll(s.getDeclaredVariables());
            }
        }
        return result;
    }

    @Override
    public void resolveTypes(Program program) {
        program.resolveTypes(thenList);
        program.resolveTypes(thenAutoClose);
        program.resolveTypes(elseList);
        program.resolveTypes(elseAutoClose);
        if (condition != null) {
            condition.resolveTypes(program);
        }
    }

}
