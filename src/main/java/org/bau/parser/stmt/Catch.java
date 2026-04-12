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

public class Catch implements Statement {

    private String nextSkipLabel;
    private String catchLabel;

    public ArrayList<Statement> list = new ArrayList<>();
    public Variable var;
    List<Statement> autoClose;

    @Override
    public Statement replace(Variable old, Expression with) {
        Catch c = new Catch();
        c.var = (Variable) var.replace(old, with);
        return c;
    }

    public String format() {
        StringBuilder buff = new StringBuilder();
        buff.append("catch " + var.format() + "\n");
        for(Statement s : list) {
            buff.append(Statement.indent(s.format()));
        }
        return buff.toString();
    }

    @Override
    public StatementResult run(Memory m) {
        Value val = m.getGlobal(Memory.EXCEPTION);
        if (val == null) {
            return StatementResult.OK;
        }
        m.setLocal(var.name(), val);
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
        buff.append("} while(0);\n");
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
        buff.append("} while(0);\n");
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

    @Override
    public BasicBlock linkBasicBlocks(FunctionContext functionContext, BasicBlock current, BasicBlock breakTarget,
            BasicBlock continueTarget) {
        BasicBlock catchBlock = functionContext.newBasicBlock();
        ArrayList<BasicBlock> throwList = functionContext.getCatchPredecessors();
        for(BasicBlock b : throwList) {
            b.addSuccessor(catchBlock);
        }
        throwList.clear();
        functionContext.linkBasicBlocks(this, catchBlock);
        catchBlock = functionContext.linkBasicBlocks(list, catchBlock, null, null);

        BasicBlock next = functionContext.newBasicBlock();
        catchBlock.addSuccessor(next);
        return next;
    }

    @Override
    public DataType canThrowException() {
        return null;
    }

    @Override
    public void setVariableVersions(FunctionContext functionContext, BasicBlock basicBlock) {
        basicBlock.setVariableVersion(var.name(), var.getVersion());
    }

    @Override
    public void setVariableVersions(String name, int oldVersion, int newVersion) {
    }

    @Override
    public List<Variable> getDeclaredVariables() {
        ArrayList<Variable> result = new ArrayList<>();
        result.add(var);
        for (Statement s : list) {
            result.addAll(s.getDeclaredVariables());
        }
        return result;
    }

    @Override
    public void resolveTypesForStatement(FunctionContext context) {
        context.resolveTypes(list);
        context.resolveTypes(autoClose);
        Expression e = var.resolveTypes(context);
        if (!(e instanceof Variable)) {
            context.getProgram().syntaxError(var.fileId, var.location, "Expected a variable, got " + e.format());
        } else {
            var = (Variable) e;
        }
    }

}
