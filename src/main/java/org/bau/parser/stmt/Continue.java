package org.bau.parser.stmt;

import java.util.HashSet;
import java.util.List;

import org.bau.parser.BasicBlock;
import org.bau.parser.DataType;
import org.bau.parser.FunctionContext;
import org.bau.parser.MemoryType;
import org.bau.parser.Program;
import org.bau.parser.ProgramContext;
import org.bau.parser.expr.Borrow;
import org.bau.parser.expr.Expression;
import org.bau.parser.expr.Variable;
import org.bau.runtime.Memory;

public class Continue implements Statement {

    public Expression condition;
    public List<Statement> autoClose;
    public Loop loop;

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
            for(Statement s : loop.listContinue) {
                buff.append(Statement.indent(s.toC()));
            }
            buff.append(Statement.indent("continue;\n"));
            // buff.append(Statement.indent("goto continue" + continuedId + ";\n"));
            buff.append("}\n");
        } else {
            buff.append("continue;\n");
        }
        if (condition != null) {
            buff.append(Borrow.resetUsedOwned(condition.getUsedOwned()));
        }
        return buff.toString();
    }

    public String format() {
        return condition != null ? "continue " + condition.format() + "\n" : "continue\n";
    }

    @Override
    public void used(Program program) {
        if (condition != null) {
            condition.used(program);
        }
        if (autoClose != null) {
            for (Statement s : autoClose) {
                s.used(program);
            }
        }
    }

    @Override
    public BasicBlock linkBasicBlocks(FunctionContext functionContext, BasicBlock current, BasicBlock breakTarget,
            BasicBlock continueTarget) {
        if (continueTarget == null) {
            throw new IllegalStateException("Continue outside of a loop");
        }
        BasicBlock after = functionContext.newBasicBlock();
        if (condition == null) {
            // what comes next if never executed: use an orphaned block
            functionContext.linkBasicBlocks(this, current);
            current.addSuccessor(continueTarget);
        } else {
            current.addSuccessor(after);
            functionContext.linkBasicBlocks(this, after);
            after.addSuccessor(continueTarget);
        }
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
    public void resolveTypesForStatement(FunctionContext context) {
        context.resolveTypes(autoClose);
        if (condition != null) {
            condition = condition.resolveTypes(context);
        }
    }

}
