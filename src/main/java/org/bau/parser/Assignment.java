package org.bau.parser;

import org.bau.runtime.Memory;
import org.bau.runtime.Value;

public class Assignment implements Statement {
    LeftValue leftValue;
    DataType type;
    boolean initial;
    boolean isConstant;
    String modify;
    Expression value;
    boolean isGlobalScope;
    
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
            leftValue.setBoundValue(scope, "=", leftValue);
        } else {
            //leftValue.setBoundValue(scope, modify, value);
        }
    }
    
    @Override
    public boolean run(Memory m) {
        Value val = value.eval(m);
        if (val != null) {
            if (leftValue instanceof Variable) {
                Variable var = (Variable) leftValue;
                m.set(var.name, null, modify, val);
            }
        }
        // TODO support field access
        // m.set(variable.identifier, arrayIndex, modify, value);
        return false;
    }
    
    public String toC(ProgramContext context) {
        StringBuilder buff = new StringBuilder();
        if (!initial) {
            buff.append(leftValue.decrementRefCountC());
        }
        String result;
        if (value instanceof Call) {
            Call c = (Call) value;
            if (c.def.exceptionType != null) {
                context.needToCatch = c.def.exceptionType;
                String s = c.def.getExceptionStruct();
                context.delareList.add(s + " _x;");
                buff.append("_x = " + value.toC() + ";\n");
                String catchLabel = "catch" + context.nextCatchLabel;
                buff.append("if (_x.exception.exceptionType != -1) goto " + catchLabel + ";\n");
                buff.append(type.toC() + " result = _x.result;\n");
                result = "result";
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
            leftValue.needToDecrementRefCountOnFree(true);
        } else {
            boolean needInc = true;
            if (value instanceof LeftValue) {
                LeftValue var = (LeftValue) value;
                if (var.needToDecrementRefCountOnFree()) {
                    var.needToDecrementRefCountOnFree(false);
                    needInc = false;
                }
            }
            if (needInc) {
                // the function call has already incremented
                buff.append(leftValue.incrementRefCountC());
            }
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
