package org.bau.parser;

import java.util.ArrayList;

import org.bau.runtime.Memory;
import org.bau.runtime.Value;

public class Call implements Statement, Expression {
    boolean statement;
    ArrayList<Expression> args = new ArrayList<>();
    FunctionDefinition def;

    @Override
    public Value eval(Memory memory) {
        if (!def.constExpr) {
            return null;
        }
        Memory m = new Memory();
        ArrayList<Value> list = new ArrayList<>(args.size());
        for (int i = 0; i < args.size(); i++) {
            Expression a = args.get(i);
            Value v = a.eval(null);
            if (v == null) {
                return null;
            }
            m.set(def.parameters.get(i).name, null, null, v);
            list.add(v);
        }
        for(Statement stat : def.list) {
            boolean brk = stat.run(m);
            if (brk) {
                break;
            }
        }
        Value val = m.get(Memory.RESULT);
        return val;
    }

    @Override
    public boolean run(Memory m) {
        if ("println".equals(def.name)) {
            for (Expression e : args) {
                Value v = e.eval(m);
                m.print(v);
            }
        }
        return false;
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
                    if (a.type().name().startsWith("0..")) {
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
