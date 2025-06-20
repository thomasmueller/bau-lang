package org.bau.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import org.bau.runtime.Memory;
import org.bau.runtime.Value;
import org.bau.runtime.Value.ValueException;
import org.bau.runtime.Value.ValuePanic;
import org.bau.std.Std;

public class Call implements Statement, Expression, LeftValue {
    boolean statement;
    ArrayList<Expression> args = new ArrayList<>();
    FunctionDefinition def;

    private String exceptionVar;
    private String catchLabel;

    @Override
    public Value eval(Memory m) {
        if (def.list == null) {
            // it was replaced, for example declared with just a name,
            // and then later a concrete implementation was set
            FunctionDefinition newDef = m.getFunction(def.getFunctionId());
            if (newDef == null) {
                m.addUncompiledFunction(def.getFunctionId(), def);
                return null;
            }
            def = newDef;
        }
        if (m == null || m.evaluateOnlyConstExpr() && (def == null || !def.constExpr)) {
            return null;
        }
        if (def.list.isEmpty() && def.code != null) {
            // const function that is not yet compiled
            m.addUncompiledFunction(def.getFunctionId(), def);
        }
        if (m.tick()) {
            return null;
        }
        HashMap<Variable, Value> params = new HashMap<>();
        ArrayList<Value> list = new ArrayList<>(args.size());
        Value varArgsValue = null;
        for (int i = 0; i < args.size(); i++) {
            Expression a = args.get(i);
            Value v = a.eval(m);
            if (v == null) {
                return null;
            }
            if (def.varArgs && i >= def.parameters.size() - 1) {
                if (i == def.parameters.size() - 1) {
                    int len = args.size() - i;
                    Variable var = def.parameters.get(i);
                    varArgsValue = new Value.ValueArray(len, new Value.ValueInt(0));
                    Value varArgsRef = new Value.ValueRef(m.putHeap(varArgsValue));
                    params.put(var, varArgsRef);
                    v = Operation.convertToType(v, var.type().baseType());
                    list.add(v);
                }
                varArgsValue.set(i - def.parameters.size() + 1, v);
            } else {
                Variable var = def.parameters.get(i);
                v = Operation.convertToType(v, var.type());
                params.put(var, v);
                list.add(v);
            }
        }
        m.saveLocal();
        for (Entry<Variable, Value> e : params.entrySet()) {
            if (!def.isConstructor) {
                e.getKey().setValue(m, e.getValue(), true, true);
            } else {
                m.setLocal(e.getKey().name, e.getValue());
            }
        }
        StatementResult r = Program.runSequence(m, def.list);
        if (def.cCode != null) {
            Value result = Std.eval(def.name, m);
            m.setGlobal(Memory.RESULT, result);
        }
        m.restoreLocal();
        if (r == StatementResult.THROW) {
            return new ValueException(m.getGlobal(Memory.EXCEPTION).toString());
        } else if (r == StatementResult.PANIC) {
            return new ValuePanic(m.getGlobal(Memory.PANIC).toString());
        } else if (r == StatementResult.TIMEOUT) {
            return new ValuePanic("Timeout");
        }
        Value val = m.getGlobal(Memory.RESULT);
        val = Operation.convertToType(val, def.returnType);
        m.setGlobal(Memory.RESULT, val);
        return val;
    }

    @Override
    public StatementResult run(Memory m) {
        if ("println".equals(def.name)) {
            for (Expression e : args) {
                Value v = e.eval(m);
                if (v instanceof Value.ValueRef) {
                    v = m.getHeap(v.longValue());
                }
                m.print(v);
            }
            m.println();
        } else {
            try {
                Value v = eval(m);
                if (v instanceof ValueException) {
                    return StatementResult.THROW;
                }
                if (v instanceof ValuePanic) {
                    return StatementResult.PANIC;
                }
            } catch (StackOverflowError e) {
                String message = "Stack overflow";
                ValuePanic v = new ValuePanic(message);
                m.print(v);
                m.println();
                m.setGlobal(Memory.PANIC, v);
                return StatementResult.PANIC;
            }
        }
        return StatementResult.OK;
    }

    @Override
    public void collectTypes(HashSet<DataType> set, MemoryType memoryType) {
        def.collectTypes(set, memoryType);
    }

    public Call replace(Variable old, Expression with) {
        Call c = new Call();
        c.statement = statement;
        c.args = new ArrayList<Expression>();
        c.def = def;
        for (int i = 0; i < args.size(); i++) {
            c.args.add(args.get(i).replace(old, with));
        }
        return c;
    }

    @Override
    public DataType type() {
        return def.returnType;
    }

    public DataType exceptionType() {
        return def.exceptionType;
    }

    public DataType canThrowException() {
        return def.exceptionType;
    }

    public void optimize(ProgramContext context) {
        if (statement && def.exceptionType != null) {
            String s = def.getExceptionStruct();
            exceptionVar = "_x" + context.nextExceptionVariableId++;
            context.delareList.add(def.exceptionType.nameC() + " _lastException;");
            context.delareList.add(s + " " + exceptionVar + ";");
            catchLabel = "catch" + context.nextCatchLabel;
            context.needToCatch = def.exceptionType;
        }
    }

    @Override
    public String toC() {
        if (def.callType == null && "println".equals(def.name)) {
            return printlnToC();
        }
        if (statement) {
            if (def.exceptionType != null && exceptionVar != null) {
                StringBuilder buff = new StringBuilder();
                buff.append(exceptionVar + " = ");
                buff.append(callToC());
                buff.append("if (" + exceptionVar + ".exception.exceptionType != -1) { _lastException = " + exceptionVar + ".exception; goto " + catchLabel + "; };\n");
                return buff.toString();
            } else {
                return callToC();
            }
        } else {
            return callToC();
        }
    }

    public String callToC() {
        StringBuilder buff = new StringBuilder();
        if (def.module != null) {
            buff.append(Program.esc(def.module).replace('.', '_') + "_");
        }
        if (def.callType != null) {
            buff.append(def.callType.idC()).append('_');
        }
        buff.append(def.nameC() + "_");
        if (def.varArgs) {
            buff.append("var");
        } else {
            buff.append(args.size());
        }
        buff.append('(');
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) {
                buff.append(", ");
            }
            if (def.varArgs && i == def.parameters.size() - 1) {
                buff.append("/* argCount */ ");
                buff.append(args.size() - i);
                buff.append(", ");
            }
            Expression e = args.get(i);
            buff.append(e.toC());
        }
        buff.append(")");
        if (statement) {
            buff.append(";\n");
            buff.append(Borrow.resetUsedOwned(getUsedOwned()));
        }
        return buff.toString();
    }

    @Override
    public List<Expression> getUsedOwned() {
        ArrayList<Expression> usedOwned = new ArrayList<Expression>();
        for (int i = 0; i < args.size(); i++) {
            if (i == 0 && def.callType != null) {
                // the "this" pointer is not used up
                continue;
            }
            Expression e = args.get(i);
            DataType type = e.type();
            if (type != null && type.memoryType() == MemoryType.OWNER) {
                usedOwned.add(e);
            }
        }
        return usedOwned;
    }

    public String printlnToC() {
        StringBuilder buff = new StringBuilder();
        buff.append("printf(");
        StringBuilder b2 = new StringBuilder();
        b2.append("\"");
        for (Expression a : args) {
            if (a instanceof StringLiteral) {
                String s = ((StringLiteral) a).value;
                s = s.replaceAll("%", "%%");
                b2.append(StringLiteral.escape(s));
            } else {
                switch (a.type().name()) {
                case "i8":
                case "i16":
                case "i32":
                    b2.append("%d");
                    break;
                case "int":
                    b2.append("%lld");
                    break;
                case "f32":
                    b2.append("%f");
                    break;
                case "float":
                    b2.append("%.9f");
                    break;
                case "i8[]":
                    b2.append("%.*s");
                    break;
                default:
                    if (a.type().enumValues != null) {
                        b2.append("%lld");
                        break;
                    } else if (a.type().name().startsWith("0..")) {
                        b2.append("%lld");
                        break;
                    } else {
                        throw new IllegalArgumentException(a.type().name());
                    }
                }

            }
        }
        b2.append("\\n\"");
        buff.append(b2.toString());
        for (Expression a : args) {
            if (a instanceof StringLiteral) {
                continue;
            }
            if (a.type().isArray()) {
                buff.append(", ");
                buff.append(a.toC()).append("->len");
                buff.append(", ");
                buff.append(a.toC()).append("->data");
            } else  {
                buff.append(", ");
                if ("int".equals(a.type().name())) {
                    buff.append("(long long)");
                }
                buff.append(a.toC());
            }
        }
        buff.append(")");
        if (statement) {
            buff.append(";\n");
        }
        return buff.toString();
    }

    public String toString() {
        StringBuilder buff = new StringBuilder();
        buff.append(def.name);
        buff.append('(');
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) {
                buff.append(", ");
            }
            buff.append(args.get(i));
        }
        buff.append(")");
        if (statement) {
            buff.append('\n');
        }
        return buff.toString();
    }

    @Override
    public boolean isEasyToRead() {
        return true;
    }

    @Override
    public Bounds getBounds() {
        return null;
    }

    @Override
    public Expression simplify() {
        return this;
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public void setBounds(Expression scope) {
        List<Expression> list = getUsedOwned();
        for (Expression e : list) {
            e.setOwnedBoundsToNull(scope);
        }
    }

    @Override
    public void setOwnedBoundsToNull(Expression scope) {
        for (Expression a : args) {
            a.setOwnedBoundsToNull(scope);
        }
    }

    @Override
    public Expression writeStatements(Parser parser, boolean assignment, ArrayList<Statement> target) {
        for (int i = 0; i < args.size(); i++) {
            Expression a = args.get(i);
            a = a.writeStatements(parser, false, target);
            args.set(i, a);
        }
        if (def.returnType == null) {
            return this;
        }
        if (assignment) {
            return this;
        }
        Variable var = parser.assignTempVariable(target, this);
        return var;
    }

    @Override
    public void used(Program program) {
        program.getFunctionById(def.getFunctionId()).used(program);
        for (Expression e : args) {
            e.used(program);
        }
    }

    @Override
    public String assignmentC() {
        // can not assign
        throw new IllegalStateException();
    }

    @Override
    public void setBoundValue(Expression scope, String modify, Expression value) {
        // can not assign
        throw new IllegalStateException();
    }

    @Override
    public void addBoundCondition(Expression scope, String operation, Expression right) {
        // can not assign
        throw new IllegalStateException();
    }

    @Override
    public String decrementRefCountC() {
        // can not assign
        throw new IllegalStateException();
    }

    @Override
    public String incrementRefCountC() {
        // can not assign
        throw new IllegalStateException();
    }

    @Override
    public Value setValue(Memory memory, Value val, boolean incRefCount, boolean initial) {
        // can not assign
        throw new IllegalStateException();
    }

    @Override
    public boolean isContant() {
        // can not assign
        throw new IllegalStateException();
    }

    @Override
    public void incrementReassignCount() {
        // ignore
    }

}
