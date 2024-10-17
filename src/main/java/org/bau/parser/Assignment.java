package org.bau.parser;

import org.bau.runtime.Memory;
import org.bau.runtime.Value;
import org.bau.runtime.Value.ValueException;
import org.bau.runtime.Value.ValuePanic;

public class Assignment implements Statement {
    LeftValue leftValue;
    DataType type;
    boolean initial;
    boolean isConstant;
    String modify;
    Expression value;
    boolean isGlobalScope;

    private String exceptionVar;
    private String catchLabel;

    @Override
    public Assignment replace(Variable old, Expression with) {
        LeftValue l2 = (LeftValue) leftValue.replace(old, with);
        Expression v2 = value.replace(old, with);
        if (leftValue == l2 && value == v2) {
            return this;
        }
        Assignment c = new Assignment();
        c.leftValue = l2;
        c.type = type;
        c.initial = initial;
        c.isConstant = isConstant;
        c.modify = modify;
        c.value = v2;
        return c;
    }

    @Override
    public void setBounds(Expression scope) {
        if (initial) {
            leftValue.setBoundValue(scope, "=", value);
        } else {
            // TODO update bounds
            //leftValue.setBoundValue(scope, modify, value);
        }
    }

    @Override
    public StatementResult run(Memory m) {
        boolean incRefCount = true;
        if (value instanceof Call) {
            incRefCount = false;
        }
        Value val = value.eval(m);
        if (val != null) {
            if (val instanceof ValuePanic) {
                return StatementResult.PANIC;
            } else if (val instanceof ValueException) {
                return StatementResult.THROW;
            }
            Value panic;
            if (modify != null) {
                Value old = leftValue.eval(m);
                if (old == null) {
                    throw new IllegalStateException();
                }
                Value v2 = Operation.eval(leftValue.type(), old, modify, val);
                panic = leftValue.setValue(m, v2, incRefCount);
            } else {
                panic = leftValue.setValue(m, val, incRefCount);
            }
            if (panic != null) {
                m.setGlobal(Memory.PANIC, val);
                return StatementResult.PANIC;
            }
        }
        return StatementResult.OK;
    }

    public void optimize(ProgramContext context) {
        if (value instanceof Call) {
            Call c = (Call) value;
            if (c.def.exceptionType != null) {
                context.needToCatch = c.def.exceptionType;
                String s = c.def.getExceptionStruct();
                exceptionVar = "_x" + context.nextExceptionVariableId++;
                context.delareList.add(c.def.exceptionType.nameC() + " _lastException;");
                context.delareList.add(s + " " + exceptionVar + ";");
                catchLabel = "catch" + context.nextCatchLabel;
            }
        }
    }

    public String toC() {
        StringBuilder buff = new StringBuilder();
        if (!initial) {
            buff.append(leftValue.decrementRefCountC());
        }
        String result;
        if (value instanceof Call) {
            Call c = (Call) value;
            if (c.def.exceptionType != null) {
                buff.append(exceptionVar + " = " + value.toC() + ";\n");
                buff.append("if (" + exceptionVar + ".exception.exceptionType != -1) { _lastException = " + exceptionVar + ".exception; goto " + catchLabel + "; };\n");
                result = exceptionVar + ".result";
            } else {
                result = value.toC();
            }
        } else {
            result = value.toC();
        }
        if (initial) {
            buff.append(type.toC());
            buff.append(' ');
        }
        buff.append(leftValue.assignmentC());
        buff.append(' ');
        if (modify != null) {
            buff.append(modify);
        }
        if (initial && value instanceof New && value.toC().equals(type.toC())) {
            // value types are auto-initialized
        } else {
            buff.append("= ");
            buff.append(result);
        }
        buff.append(";\n");
        if (value instanceof Call || value instanceof New) {
        } else {
            buff.append(leftValue.incrementRefCountC());
        }
        return buff.toString();
    }

    public String toString() {
        StringBuilder buff = new StringBuilder();
        buff.append(leftValue);
        if (isConstant) {
            buff.append(" : ");
        } else if (initial) {
            buff.append(" := ");
        } else if (modify != null) {
            buff.append(' ');
            buff.append(modify);
            buff.append("= ");
        } else {
            buff.append(" = ");
        }
        buff.append(value);
        buff.append("\n");
        return buff.toString();
    }

}
