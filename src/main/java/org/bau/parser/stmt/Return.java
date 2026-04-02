package org.bau.parser.stmt;

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
import org.bau.runtime.Value.ValueException;
import org.bau.runtime.Value.ValuePanic;

public class Return implements Statement {

    private String exceptionStruct;

    public Expression expr;
    public List<Statement> autoClose;

    public Return(Expression expr) {
        this.expr = expr;
    }

    @Override
    public Statement replace(Variable old, Expression with) {
        Return c = new Return(expr.replace(old, with));
        return c;
    }

    @Override
    public StatementResult run(Memory m) {
        if (expr == null) {
            return StatementResult.RETURN;
        }
        Value val = expr.eval(m);
        if (val != null) {
            if (val instanceof ValueException) {
                return StatementResult.THROW;
            } else if (val instanceof ValuePanic) {
                return StatementResult.PANIC;
            }
            m.setGlobal(Memory.RESULT, val);
        }
        return StatementResult.RETURN;
    }

    @Override
    public void collectTypes(HashSet<DataType> set, MemoryType memoryType) {
        Program.collectTypes(autoClose, set, memoryType);
    }

    public void optimize(ProgramContext context) {
        if (context.function.exceptionType != null) {
            exceptionStruct = context.function.getExceptionStruct();
        }
    }

    public String toC() {
        StringBuilder buff = new StringBuilder();
        if (autoClose != null) {
            for (Statement s : autoClose) {
                buff.append(s.toC());
            }
        }
        if (exceptionStruct != null) {
            buff.append("return ok" + exceptionStruct + "(");
            if (expr != null) {
                buff.append(expr.toC());
            }
            buff.append(");\n");
        } else if (expr != null) {
            buff.append("return " + expr.toC() + ";\n");
        } else {
            buff.append("return;\n");
        }
        return buff.toString();
    }

    public String format() {
        return expr != null ? "return " + expr.format() + "\n" : "return\n";
    }

    @Override
    public void used(Program program) {
        if (expr != null) {
            expr.used(program);
        }
        if (autoClose != null) {
            for (Statement s : autoClose) {
                s.used(program);
            }
        }
    }

    @Override
    public DataType canThrowException() {
        if (expr == null) {
            return null;
        }
        return expr.canThrowException();
    }

    @Override
    public void setVariableVersions(FunctionContext functionContext, BasicBlock basicBlock) {
        if (expr != null) {
            expr.setVariableVersions(functionContext, basicBlock);
        }
    }

    @Override
    public void setVariableVersions(String name, int oldVersion, int newVersion) {
        if (expr != null) {
            expr.setVariableVersions(name, oldVersion, newVersion);
        }
    }

    @Override
    public void skipIncrementDecrementRefCount(String varName) {
        if (autoClose != null) {
            for (Statement s : autoClose) {
                s.skipIncrementDecrementRefCount(varName);
            }
        }
    }

    @Override
    public void resolveTypes(Program program) {
        program.resolveTypes(autoClose);
        if (expr != null) {
            expr.resolveTypes(program);
        }
    }

}
