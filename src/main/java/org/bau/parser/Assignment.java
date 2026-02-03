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
    public void setBounds(Solver solver, int depth, boolean loop) {
        if (initial || modify == null) {
            if (value instanceof Operation) {
                Operation op = (Operation) value;
                if (op.operator.equals("%")) {
                    // if the bounds of the left operand are >= 0, then
                    // we can set the bounds to be >= 0 and smaller than the right
                    Solver.Rule r = Solver.rule(Operation.toSolverExpr(op.left), ">=", Solver.number(0));
                    if (solver.isTrue(r)) {
                        r = Solver.rule(Operation.toSolverExpr(leftValue), ">=", Solver.number(0));
                        r.depth = depth;
                        solver.addRule(r);
                        r = Solver.rule(Operation.toSolverExpr(leftValue), "<", Operation.toSolverExpr(op.right));
                        r.depth = depth;
                        solver.addRule(r);
                    }
                } else if (op.operator.equals(">>")) {
                    // logical right shift will always result in >= 0
                    Solver.Rule r = Solver.rule(Operation.toSolverExpr(leftValue), ">=", Solver.number(0));
                    r.depth = depth;
                    solver.addRule(r);
                } else {
                    Solver.Rule r = Solver.rule(Operation.toSolverExpr(leftValue), "=", Operation.toSolverExpr(value));
                    if (r.isComplete()) {
                        solver.addRule(r);
                    }
                }
            } else {
                Solver.Rule r = Solver.rule(Operation.toSolverExpr(leftValue), "=", Operation.toSolverExpr(value));
                if (r.isComplete()) {
                    solver.addRule(r);
                }
                if (leftValue.type().memoryType() == MemoryType.OWNER) {
                    // set it to be not null (the old owner is set to null elsewhere)
                    Solver.Rule r2 = Operation.toRule(leftValue, "<>", NumberValue.ZERO);
                    // survive loops etc
                    r2.always = true;
                    solver.addRule(r2);
                }
            }
        }
        value.setOwnedBoundsToNull(solver, depth, loop);
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
                panic = leftValue.setValue(m, v2, incRefCount, initial);
            } else {
                panic = leftValue.setValue(m, val, incRefCount, initial);
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
        if (memoryType == MemoryType.OWNER) {
            // re-assignment of owned variable
            if (!initial) {
                if (leftValue.type().memoryType() == MemoryType.OWNER) {
                    set.add(type);
                }
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
        leftValue.incrementReassignCount();
    }

    public String toC() {
        StringBuilder buff = new StringBuilder();
        if (!(value instanceof NullValue)) {
            if (!(value instanceof Call || value instanceof New)) {
                buff.append(Variable.incrementRefCountC(value.toC(), value.type()));
            }
        }
        if (!initial) {
            buff.append(leftValue.decrementRefCountC());
        }
        String result;
        if (value instanceof Call) {
            Call c = (Call) value;
            if (c.def.exceptionType != null) {
                buff.append(exceptionVar + " = " + value.toC() + ";\n");
                buff.append("if (" + exceptionVar + ".exception.exceptionType != -1) { _lastException = " + exceptionVar + ".exception; goto " + catchLabel + "; }\n");
                result = exceptionVar + ".result";
            } else {
                result = value.toC();
            }
        } else {
            result = value.toC();
        }
        if (initial && !isGlobalScope && !(leftValue instanceof FieldAccess)) {
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

    public void setConstantBounds(Solver solver, Variable v, Expression expr) {
        DataType type = value.type();
        if (type.memoryType() == MemoryType.OWNER) {
            solver.removeRulesFor(Solver.variable(v.name));
        }
        if (type.isArray()) {
            if (value instanceof New) {
                New n = (New) value;
                v.setConstantLength(n.arrayLength);
                FieldAccess f = new FieldAccess(v, "len", DataType.INT_TYPE);
                Solver.Rule r = Operation.toRule(f, "=", n.arrayLength);
                if (r != null) {
                    r.always = true;
                    r.global = v.global;
                    solver.addRule(r);
                }
            } else if (value instanceof StringLiteral) {
                StringLiteral n = (StringLiteral) value;
                NumberValue len = new NumberValue(n.array.len(), DataType.INT_TYPE, false);
                v.setConstantLength(len);
                FieldAccess f = new FieldAccess(v, "len", DataType.INT_TYPE);
                Solver.Rule r = Operation.toRule(f, "=", len);
                if (r != null) {
                    r.always = true;
                    r.global = v.global;
                    solver.addRule(r);
                }
            } else if (value instanceof ArrayConstant) {
                ArrayConstant n = (ArrayConstant) value;
                NumberValue len = new NumberValue(n.len(), DataType.INT_TYPE, false);
                v.setConstantLength(len);
                FieldAccess f = new FieldAccess(v, "len", DataType.INT_TYPE);
                Solver.Rule r = Operation.toRule(f, "=", len);
                if (r != null) {
                    r.always = true;
                    r.global = v.global;
                    solver.addRule(r);
                }
            } else if (value instanceof Variable) {
                Variable v2 = (Variable) value;
                v.copyConstant(v2);
                FieldAccess f1 = new FieldAccess(v2, "len", DataType.INT_TYPE);
                FieldAccess f2 = new FieldAccess(v, "len", DataType.INT_TYPE);
                Solver.Rule r = Operation.toRule(f1, "=", f2);
                if (r != null) {
                    r.always = true;
                    r.global = v.global;
                    solver.addRule(r);
                }
            }
        }
        if (type.memoryType() == MemoryType.OWNER) {
            // set it to be not null (the old owner is set to null elsewhere)
            Solver.Rule r = Operation.toRule(v, "<>", NumberValue.ZERO);
            // survive loops etc
            r.always = true;
            solver.addRule(r);
        } else {
            Solver.Rule r = Operation.toRule(v, "=", value);
            if (r != null) {
                r.always = true;
                r.global = v.global;
                solver.addRule(r);
            }
        }
    }

}
