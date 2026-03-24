package org.bau.parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.bau.runtime.Memory;
import org.bau.runtime.Value;

public class Loop implements Statement {
    ArrayList<Statement> list = new ArrayList<>();
    ArrayList<Statement> listContinue = new ArrayList<>();
    List<Statement> autoClose;
    Expression condition;
    BasicBlock basicBlock;

    @Override
    public Statement replace(Variable old, Expression with) {
        Loop c = new Loop();
        c.condition = condition.replace(old, with);
        for (Statement s : list) {
            c.list.add(s.replace(old, with));
        }
        return c;
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
            Value va = condition.eval(m);
            if (va == null) {
                return null;
            }
            long v = va.longValue();
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
        if (basicBlock != null) {
            buff.append(basicBlock.toC());
        }
        buff.append("while (" + condition.toC() + ") {\n");
        boolean hasReturn = Program.hasReturn(list);
        int catchCount = Program.catchCount(list);
        for (int j = 0; j < catchCount; j++) {
            buff.append(Statement.indent("do { do {\n"));
        }
        for (Statement s : list) {
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
        if (buffContinue.length() > 0) {
            buff.append(buffContinue);
        }
        buff.append("}\n");
        return buff.toString();
    }

    public String toString() {
        // TODO this is not correct if continue is used
        StringBuilder buff = new StringBuilder();
        buff.append("loop " + condition + "\n");
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

    @Override
    public BasicBlock linkBasicBlocks(FunctionContext functionContext, BasicBlock current, BasicBlock breakTarget,
            BasicBlock continueTarget) {
        BasicBlock next = functionContext.newBasicBlock();
        current.addSuccessor(next);
        current = next;
        functionContext.linkBasicBlocks(this, current);
        BasicBlock after = functionContext.newBasicBlock();
        breakTarget = after;
        BasicBlock loopHead = current;
        continueTarget = current;
        if (listContinue.size() > 0) {
            continueTarget = functionContext.newBasicBlock();
        }
        if (list.size() > 0) {
            BasicBlock loop = functionContext.newBasicBlock();
            current.addSuccessor(loop);
            current = loop;
            current = functionContext.linkBasicBlocks(list, current, breakTarget, continueTarget);
        }
        if (listContinue.size() > 0) {
            current.addSuccessor(continueTarget);
            current = continueTarget;
            current = functionContext.linkBasicBlocks(listContinue, current, breakTarget, continueTarget);
        }
        loopHead.addSuccessor(breakTarget);
        current.addSuccessor(next);
        return breakTarget;
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

    public void setBasicBlock(BasicBlock basicBlock) {
        this.basicBlock = basicBlock;
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
        for(Statement s : list) {
            result.addAll(s.getDeclaredVariables());
        }
        return result;
    }

}
