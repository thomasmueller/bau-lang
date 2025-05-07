package org.bau.parser;

import java.util.HashSet;

import org.bau.runtime.Memory;
import org.bau.runtime.Value;
import org.bau.runtime.Value.ValueException;
import org.bau.runtime.Value.ValueInt;
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
        if (initial || modify == null) {
            if (value instanceof Operation) {
                Operation op = (Operation) value;
                if (op.operator.equals("%")) {
                    // if the bounds of the left operand are >= 0, then
                    // we can set the bounds to be >= 0 and smaller than the right
                    Bounds b = op.left.getBounds();
                    if (b != null && b.largerThan(scope, -1)) {
                        leftValue.setBoundValue(scope, "<", op.right);
                    }
                } else {
                    leftValue.setBoundValue(scope, "=", value);
                }
            } else {
                leftValue.setBoundValue(scope, "=", value);
            }
        } else {
            // TODO modify bounds
            // leftValue.setBoundValue(scope, modify, value);
        }
        value.setOwnedBoundsToNull(scope);
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

    @Override
    public void collectTypes(HashSet<DataType> set, MemoryType memoryType) {
        if (memoryType == MemoryType.BORROW) {
            if (leftValue.type().memoryType() == MemoryType.BORROW) {
                set.add(type);
            }
        }
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
        if (initial && !isGlobalScope) {
            buff.append(type.toC());
            buff.append(' ');
        }
        buff.append(leftValue.assignmentC());
        buff.append(' ');
        if ("%".equals(modify) || "/".equals(modify)) {
            // first, use "%" or "/" by "0", and then
            // replace the last '0' with the right expression
            // TODO this is a hack
            Operation op = new Operation(leftValue, modify, new NumberValue(ValueInt.ZERO, DataType.INT_TYPE, false));
            String ops = op.toC();
            int index = ops.lastIndexOf('0');
            ops = ops.substring(0, index) + result + ops.substring(index + 1);
            buff.append("= ");
            buff.append(ops);
        } else {
            if (modify == null) {
                // reqular assignment
            } else {
                buff.append(modify);
            }
            if (initial && value instanceof New && value.toC().equals(type.toC())) {
                // value types are auto-initialized
            } else {
                buff.append("= ");
                buff.append(result);
            }
        }
        buff.append(";\n");
        if (!(value instanceof NullValue)) {
            if (Program.SIMPLE_REF_COUNTING) {
                if (!(value instanceof Call || value instanceof New)) {
                    buff.append(leftValue.incrementRefCountC());
                }
            } else {
                if (!(value instanceof Call)) {
                    buff.append(leftValue.incrementRefCountC());
                }
            }
        }
        buff.append(Borrow.resetUsedOwned(value.getUsedOwned()));
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

    @Override
    public void used(Program program) {
        if ("%".equals(modify) || "/".equals(modify)) {
            Operation op = new Operation(leftValue, modify, value);
            op.used(program);
        }
        leftValue.used(program);
        if (type != null) {
            type.used(program);
        }
        value.used(program);
    }

    public void setConstantBounds(Variable v) {
        if (value.type().isArray()) {
            if (value instanceof New) {
                New n = (New) value;
                v.addLenBoundCondition(null, "=", n.arrayLength);
            } else if (value instanceof StringLiteral) {
                StringLiteral n = (StringLiteral) value;
                v.addLenBoundCondition(null, "=", new NumberValue(n.array.len(), DataType.INT_TYPE, false));
            } else if (value instanceof ArrayConstant) {
                ArrayConstant n = (ArrayConstant) value;
                v.addLenBoundCondition(null, "=", new NumberValue(n.len(), DataType.INT_TYPE, false));
            } else if (value instanceof Variable) {
                Variable v2 = (Variable) value;
                v.copyBounds(v2);
            }
        }
        v.setBoundValue(null, "=", value);
    }

}
