package org.bau.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.bau.runtime.Memory;
import org.bau.runtime.Value;
import org.bau.runtime.Value.ValueException;
import org.bau.runtime.Value.ValuePanic;
import org.bau.std.Std;

public class Call implements Statement, Expression {
    boolean statement;
    ArrayList<Expression> args = new ArrayList<>();
    FunctionDefinition def;

    @Override
    public Value eval(Memory m) {
        if (def.list == null) {
            // it was replaced, for example declared with just a name,
            // and then later a concrete implementation was set
            def = m.getFunction(def.getFunctionId());
        }
        if (m == null || m.evaluateOnlyConstExpr() && !def.constExpr) {
            return null;
        }
        if (m.tick()) {
            return null;
        }
        HashMap<String, Value> params = new HashMap<>();
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
                    varArgsValue = new Value.ValueArray(len, new Value.ValueInt(0));
                    params.put(def.parameters.get(i).name, varArgsValue);
                    list.add(v);
                }
                varArgsValue.set(i - def.parameters.size() + 1, v);
            } else {
                params.put(def.parameters.get(i).name, v);
                list.add(v);
            }
        }
        m.saveLocal();
        for (Entry<String, Value> e : params.entrySet()) {
            m.setLocal(e.getKey(), e.getValue());
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
        }
        Value val = m.getGlobal(Memory.RESULT);
        return val;
    }

    @Override
    public StatementResult run(Memory m) {
        if ("println".equals(def.name)) {
            for (Expression e : args) {
                Value v = e.eval(m);
                m.print(v);
            }
            m.println();
        } else {
            Value v = eval(m);
            if (v instanceof ValueException) {
                return StatementResult.THROW;
            }
        }
        return StatementResult.OK;
    }
    
    public Call replace(Variable old, Expression with) {
        Call c = new Call();
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

    @Override
    public String toC() {
        return toC(null);
    }
    
    public DataType exceptionType() {
        return def.exceptionType;
    }
    
    public DataType canThrowException() {
        return def.exceptionType;
    }    

    @Override
    public String toC(ProgramContext context) {
        if (def.callType == null && "println".equals(def.name)) {
            return printlnToC();
        }
        if (statement && def.exceptionType != null) {
            StringBuilder buff = new StringBuilder();
            String s = def.getExceptionStruct();
            context.delareList.add(s + " _x;");
            buff.append("_x = ");
            buff.append(callToC());
            String catchLabel = "catch" + context.nextCatchLabel;
            buff.append("if (_x.exception.exceptionType != -1) goto " + catchLabel + ";\n");
            context.needToCatch = def.exceptionType;
            return buff.toString();
        }
        return callToC();
    }

    public String callToC() {
        StringBuilder buff = new StringBuilder();
        if (def.callType != null) {
            buff.append(def.callType.name()).append('_');
        }
        if (def.module != null) {
            buff.append(def.module.replace('.', '_') + "_");
        }
        buff.append(def.name + "_");
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
        }
        return buff.toString();
    }

    public String printlnToC() {
        StringBuilder buff = new StringBuilder();
        buff.append("printf(");
        StringBuilder b2 = new StringBuilder();
        b2.append("\"");
        for (Expression a : args) {
            if (a instanceof StringLiteral) {
                String s = ((StringLiteral) a).value;
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
                case "f64":
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
    public Expression writeStatements(Parser parser, ArrayList<Statement> target) {
        for (int i = 0; i < args.size(); i++) {
            Expression a = args.get(i);
            a = a.writeStatements(parser, target);
            args.set(i, a);
        }
        if (def.returnType == null) {
            return this;
        }
        Variable var = parser.assignTempVariable(target, this);
        return var;
    }    
    
}
