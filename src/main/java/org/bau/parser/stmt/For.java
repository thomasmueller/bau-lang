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

public class For implements Statement {

    public ArrayList<Statement> list = new ArrayList<>();
    public Variable variable;
    public Expression generator;

    @Override
    public Statement replace(Variable old, Expression with) {
        Loop c = new Loop();
        c.condition = generator.replace(old, with);
        for (Statement s : list) {
            c.list.add(s.replace(old, with));
        }
        return c;
    }

    @Override
    public StatementResult run(Memory m) {
        // TODO
        return StatementResult.OK;
    }

    @Override
    public void collectTypes(HashSet<DataType> set, MemoryType memoryType) {
        Program.collectTypes(list, set, memoryType);
    }

    @Override
    public void optimize(ProgramContext context) {
        for (Statement s : list) {
            s.optimize(context);
        }
    }

    public String toC() {
        throw new IllegalStateException();
    }

    public String format() {
        // TODO this is not correct if continue is used
        StringBuilder buff = new StringBuilder();
        buff.append("for " + variable + " := " + generator.format() + "\n");
        for(Statement s : list) {
            buff.append(Statement.indent(s.format()));
        }
        return buff.toString();
    }

    @Override
    public void used(Program program) {
        for (Statement s : list) {
            s.used(program);
        }
        if (generator != null) {
            generator.used(program);
        }
    }

    @Override
    public BasicBlock linkBasicBlocks(FunctionContext functionContext, BasicBlock current, BasicBlock breakTarget,
            BasicBlock continueTarget) {
        throw new IllegalStateException();
    }

    @Override
    public void setVariableVersions(FunctionContext functionContext, BasicBlock basicBlock) {
        throw new IllegalStateException();
    }

    @Override
    public void setVariableVersions(String name, int oldVersion, int newVersion) {
        throw new IllegalStateException();
    }

    @Override
    public DataType canThrowException() {
        throw new IllegalStateException();
    }

    @Override
    public List<Variable> getDeclaredVariables() {
        throw new IllegalStateException();
    }

    @Override
    public void resolveTypesForStatement(FunctionContext context) {
        throw new IllegalStateException();
    }

}
