package org.bau.parser;

import java.util.ArrayList;
import java.util.List;

import org.bau.parser.Solver.Rule;
import org.bau.parser.Statement.StatementResult;
import org.bau.runtime.Memory;
import org.bau.runtime.Value;
import org.bau.runtime.Value.ValueArray;
import org.bau.runtime.Value.ValueNull;
import org.bau.runtime.Value.ValueStruct;

public class Variable implements Expression, LeftValue {
    String name;
    String module;
    private DataType type;
    private Expression length;
    boolean isConstant;
    Value constantValue;
    public boolean global;
    public boolean used;
    int reassignCount;
    boolean skipIncrementDecrementRefCount;
    boolean isInternal;

    public Variable(String name, DataType type) {
        this(null, name, false, type);
    }

    public Variable(String module, String name, boolean global, DataType type) {
        this.module = module;
        this.name = name;
        this.global = global;
        this.type = type;
    }

    public static String getGlobalVariableId(String module, String name) {
        if (module != null) {
            return module + "." + name;
        }
        return name;
    }

    @Override
    public Value eval(Memory memory) {
        if (isConstant && constantValue != null) {
            return constantValue;
        }
        if (memory == null) {
            return null;
        }
        if (global) {
            return memory.getGlobal(name);
        }
        return memory.getLocal(name);
    }

    public DataType canThrowException() {
        return null;
    }

    @Override
    public DataType type() {
        return type;
    }

    public Expression replace(Variable old, Expression with) {
        if (name.equals(old.name)) {
            return with;
        }
        return this;
    }

    public String toString() {
        return name;
    }

    public String declarationToC() {
        StringBuilder buff = new StringBuilder();
        DataType t = type();
        if (t.isFunctionPointer) {
            if (t.functionPointerReturnType == null) {
                buff.append("void ");
            } else {
                buff.append(t.functionPointerReturnType.toC());
            }
            buff.append(" (*" + nameC() + ")(");
            for (int i = 0; i < t.functionPointerArgs.size(); i++) {
                if (i > 0) {
                    buff.append(", ");
                }
                buff.append(t.functionPointerArgs.get(i).toC());
            }
            buff.append(")");
            return buff.toString();
        }
        buff.append(type().toC());
        buff.append(' ');
        buff.append(nameC());
        return buff.toString();
    }

    public String toC() {
        if (constantValue != null && type.isNumber() && !type.isArray()) {
            StringBuilder buff = new StringBuilder();
            if (type.isFloatingPoint()) {
                buff.append(NumberValue.toC(constantValue.doubleValue()));
            } else {
                buff.append(NumberValue.toC(constantValue.longValue()));
            }
            buff.append(" /* " + nameC() + " */");
            return buff.toString();
        }
        return nameC();
    }

    @Override
    public List<Expression> getUsedOwned() {
        ArrayList<Expression> usedOwned = new ArrayList<Expression>();
        DataType type = type();
        if (type != null && type.memoryType() == MemoryType.OWNER) {
            usedOwned.add(this);
        }
        return usedOwned;
    }

    @Override
    public void setOwnedBoundsToNull(Solver solver, int depth, boolean loop) {
        if (type != null && type.memoryType() == MemoryType.OWNER) {
            Solver.Rule r = Operation.toRule(this, "=", NumberValue.ZERO);
            r.depth = depth;
            solver.removeRulesFor(r.left);
            solver.addRule(r);
        }
    }

    public static String decrementRefCountC(String expr, String empty, DataType type) {
        if (type.needIncDec()) {
            if (type.memoryType() == MemoryType.REF_COUNT) {
                return Free.DEC_USE_STACK + "(" + expr + ", " + type.nameC() +");\n";
            } else if (type.memoryType() == MemoryType.OWNER) {
                return type.nameC() + "_free(" + expr + ");\n";
            } else {
                return empty;
            }
        } else if (type.needFree()) {
            return type.nameC() + "_free(&" + expr + ");\n";
        }
        return empty;
    }

    @Override
    public String decrementRefCountC() {
        if (skipIncrementDecrementRefCount) {
            return "";
        }
        return decrementRefCountC(nameC(), "", type());
    }

    @Override
    public String incrementRefCountC() {
        if (skipIncrementDecrementRefCount) {
            return "";
        }
        // copy of FieldAccess
        if (type().needIncDec()) {
            if (type().memoryType() == MemoryType.REF_COUNT) {
                return Free.INC_USE_STACK + "(" + nameC() + ");\n";
            } else {
                return "";
            }
        } else if (type().needFree()) {
            return type().toC() + "_copy(&" + nameC() + ");\n";
        }
        return "";
    }

    public static String incrementRefCountC(String expr, DataType type) {
        // copy of FieldAccess
        if (type.needIncDec()) {
            if (type.memoryType() == MemoryType.REF_COUNT) {
                return Free.INC_USE_STACK + "(" + expr + ");\n";
            } else {
                return "";
            }
        } else if (type.needFree()) {
            return type.toC() + "_copy(&" + expr + ");\n";
        }
        return "";
    }

    @Override
    public boolean isEasyToRead() {
        return true;
    }

    public Expression getConstantLength() {
        return length;
    }

    @Override
    public Expression simplify() {
        if (isConstant && constantValue != null) {
            return new NumberValue(constantValue, type(), false);
        }
        return this;
    }

    public void setConstantLength(Expression length) {
        this.length = length;
    }

    @Override
    public boolean isSimple() {
        return true;
    }

    @Override
    public Expression writeStatements(Parser parser, boolean assignment, ArrayList<Statement> target) {
        return this;
    }

    @Override
    public Value setValue(Memory memory, Value val, boolean incRefCount, boolean initial) {
        if (global) {
            if (type.needIncDec() && !(val instanceof ValueArray)) {
                // ValueArray is a constant
                Value old = memory.getGlobal(name);
                memory.setGlobal(name, val);
                if (!skipIncrementDecrementRefCount) {
                    if (incRefCount) {
                        memory.incHeap(val.longValue());
                    }
                    if (old != null && !initial) {
                        // if initial is true, then the old value
                        // could be from another variable
                        StatementResult result = Free.decRefCount(old, type, memory);
                        if (result == StatementResult.PANIC) {
                            return memory.getGlobal(Memory.PANIC);
                        }
                    }
                }
            } else {
                memory.setGlobal(name, val);
            }
        } else {
            if (type.needIncDec() && !(val instanceof ValueArray)) {
                // ValueArray is a constant
                Value old = memory.getLocal(name);
                memory.setLocal(name, val);
                if (!skipIncrementDecrementRefCount) {
                    if (incRefCount) {
                        memory.incHeap(val.longValue());
                    }
                    if (old != null && !initial) {
                        StatementResult result = Free.decRefCount(old, type, memory);
                        if (result == StatementResult.PANIC) {
                            return memory.getGlobal(Memory.PANIC);
                        }
                    }
                }
            } else {
                memory.setLocal(name, val);
            }
        }
        if (type().isCopyType() && type().needFree()) {
            // for value type (structs that are not ref counted),
            // we need to increment the ref counts of the fields
            // TODO this needs to be done recursively
            if (val instanceof ValueStruct) {
                ValueStruct s = (ValueStruct) val;
                for (Variable f : type().fields) {
                    if (f.type.needIncDec()) {
                        Value v = s.get(f.name);
                        if (v != ValueNull.INSTANCE) {
                            memory.incHeap(v.longValue());
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public boolean isContant() {
        return isConstant;
    }

    @Override
    public void used(Program program) {
        type.used(program);
        this.used = true;
    }

    public boolean isUsed() {
        return used;
    }

    public void copyConstant(Variable v2) {
        length = v2.length;
    }

    @Override
    public void incrementReassignCount() {
        reassignCount++;
    }

    public String nameC() {
        if (type.isFunctionPointer) {
            return Program.esc(name) + "_" + type.functionPointerArgs.size();
        }
        if (isInternal) {
            return "_" + name.substring(1);
        }
        return Program.esc(name);
    }

    @Override
    public String assignmentC() {
        return nameC();
    }

    @Override
    public boolean containsModifiableVariables() {
        return !isConstant;
    }

    @Override
    public List<Rule> getRules() {
        Rule r = Solver.rule(Solver.variable(toString()), "<>", Solver.number(0));
        return List.of(r);
    }

}
