package org.bau.mini;

import org.bau.mini.ast.Expr;
import org.bau.mini.ast.Stmt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class Interpreter {
    static final class MiniFunction {
        final String name;
        final Stmt.Function def;

        MiniFunction(String name, Stmt.Function def) {
            this.name = name;
            this.def = def;
        }
    }

    static final class Environment {
        final Environment parent;
        final Map<String, Object> values = new HashMap<>();

        Environment(Environment parent) {
            this.parent = parent;
        }

        Object get(String name) {
            if (values.containsKey(name))
                return values.get(name);
            if (parent != null)
                return parent.get(name);
            throw new RuntimeException("Undefined variable: " + name);
        }

        void set(String name, Object value) {
            values.put(name, value);
        }

        boolean hasLocal(String name) {
            return values.containsKey(name);
        }
    }

    private final Map<String, MiniFunction> functions = new HashMap<>();
    private final Map<String, Stmt.TypeDef> types = new HashMap<>();

    private static final class FunPtr {
        final String name;

        FunPtr(String n) {
            name = n;
        }
    }

    // Operation timeout support (disabled by default)
    private long maxOps = Long.MAX_VALUE;
    private int checkEvery = 65536;
    private long opCount = 0;
    private long nextCheck = checkEvery;

    public Interpreter setMaxOps(long maxOps) {
        this.maxOps = maxOps <= 0 ? Long.MAX_VALUE : maxOps;
        return this;
    }

    public Interpreter setCheckEvery(int k) {
        this.checkEvery = Math.max(1, k);
        this.nextCheck = this.opCount + this.checkEvery;
        return this;
    }

    private void tick() {
        opCount++;
        if (opCount >= nextCheck) {
            if (opCount > maxOps)
                throw new RuntimeException("timeout");
            nextCheck += checkEvery;
        }
    }

    Object execute(Stmt.Program program) {
        // First pass: collect functions and types
        for (Stmt item : program.items()) {
            if (item instanceof Stmt.Function fn)
                functions.put(fn.name(), new MiniFunction(fn.name(), fn));
            else if (item instanceof Stmt.TypeDef td)
                types.put(td.name(), td);
        }

        Environment globals = new Environment(null);
        // Built-in: println implemented via format+puts at call time (no special AST
        // def)
        functions.put("println", new MiniFunction("println", null));
        // Built-in: puts
        functions.put("puts", new MiniFunction("puts", null));
        // Built-in: format
        functions.put("format", new MiniFunction("format", null));

        // If there's a main, call it; else run top-level statements
        if (functions.containsKey("main")) {
            return callFunction("main", List.of(), globals);
        }

        for (Stmt item : program.items()) {
            if (item instanceof Stmt.Function || item instanceof Stmt.TypeDef)
                continue;
            execStmt(item, globals);
        }
        return null;
    }

    private Object callFunction(String name, List<Object> args, Environment callerEnv) {
        if (name.equals("println")) {
            // Accept either many args or a single text[] argument
            java.util.List<?> vargs;
            if (args.size() == 1 && (args.get(0) instanceof java.util.List))
                vargs = (java.util.List<?>) args.get(0);
            else
                vargs = args;
            StringBuilder sb = new StringBuilder();
            for (Object a : vargs)
                sb.append(stringify(a));
            System.out.println(sb.toString());
            return null;
        }
        if (name.equals("puts")) {
            if (args.size() != 1)
                throw new RuntimeException("puts expects 1 argument");
            System.out.println(stringify(args.get(0)));
            return null;
        }
        if (name.equals("format")) {
            StringBuilder sb = new StringBuilder();
            for (Object a : args)
                sb.append(stringify(a));
            return sb.toString();
        }
        MiniFunction mf = functions.get(name);
        if (mf == null) {
            // Constructor call for a type
            Stmt.TypeDef td = types.get(name);
            if (td != null) {
                if (args.size() != td.fields().length)
                    throw new RuntimeException("arity mismatch for constructor " + name);
                Map<String, Object> obj = new HashMap<>();
                for (int i = 0; i < td.fields().length; i++)
                    obj.put(td.fields()[i].name(), args.get(i));
                return obj;
            }
            throw new RuntimeException("Unknown function: " + name);
        }
        Stmt.Function def = mf.def;
        // Support single trailing array parameter (varargs-like)
        List<Object> callArgs = args;
        Stmt.Param[] params = def.params();
        if (params.length != args.size()) {
            if (params.length >= 1 && params[params.length - 1].type() != null
                    && params[params.length - 1].type().endsWith("[]")) {
                int fixed = params.length - 1;
                if (args.size() < fixed)
                    throw new RuntimeException("arity mismatch for " + name);
                // Passthrough if provided equals params and last arg already an array
                if (args.size() == params.length && args.get(fixed) instanceof java.util.List) {
                    // pass through
                } else {
                    String elemType = params[params.length - 1].type().substring(0,
                            params[params.length - 1].type().length() - 2);
                    List<Object> arr = new ArrayList<>();
                    for (int i = fixed; i < args.size(); i++)
                        arr.add(convertTo(elemType, args.get(i)));
                    List<Object> packed = new ArrayList<>(fixed + 1);
                    for (int i = 0; i < fixed; i++)
                        packed.add(args.get(i));
                    packed.add(arr);
                    callArgs = packed;
                }
            } else {
                throw new RuntimeException("arity mismatch for " + name);
            }
        }
        Environment env = new Environment(callerEnv);
        for (int i = 0; i < def.params().length; i++) {
            env.set(def.params()[i].name(), callArgs.get(i));
        }
        try {
            execBlock(def.body(), env);
        } catch (ReturnSignal r) {
            return r.value;
        }
        return null;
    }

    private void execBlock(Stmt.Block block, Environment env) {
        for (Stmt s : block.statements())
            execStmt(s, env);
    }

    private static final class ExitSignal extends RuntimeException {

        private static final long serialVersionUID = 1L;
    }

    private static final class ReturnSignal extends RuntimeException {
        private static final long serialVersionUID = 1L;
        final Object value;

        ReturnSignal(Object v) {
            this.value = v;
        }
    }

    private void execStmt(Stmt s, Environment env) {
        tick();
        if (s instanceof Stmt.Assign a) {
            Object value = eval(a.value(), env);
            assign(a.target(), value, env);
        } else if (s instanceof Stmt.Update u) {
            Object prev = eval(u.target(), env);
            Object add = eval(u.value(), env);
            assign(u.target(), addOp(prev, add), env);
        } else if (s instanceof Stmt.ExprStmt e) {
            eval(e.expr(), env);
        } else if (s instanceof Stmt.If iff) {
            for (int i = 0; i < iff.conds().length; i++) {
                if (isTruthy(eval(iff.conds()[i], env))) {
                    execBlock(iff.blocks()[i], env);
                    return;
                }
            }
            if (iff.elseBlock() != null)
                execBlock(iff.elseBlock(), env);
        } else if (s instanceof Stmt.Loop loop) {
            while (isTruthy(eval(loop.cond(), env))) {
                try {
                    execBlock(loop.body(), env);
                } catch (ExitSignal es) {
                    break;
                }
            }
        } else if (s instanceof Stmt.For fr) {
            env.set(fr.var(), 0L);
            while (toLong(eval(new Expr.Variable(fr.var(), 0, 0), env)) < toLong(eval(fr.limit(), env))) {
                try {
                    execBlock(fr.body(), env);
                } catch (ExitSignal es) {
                    break;
                }
                env.set(fr.var(), (Long) env.get(fr.var()) + 1L);
            }
        } else if (s instanceof Stmt.Exit) {
            throw new ExitSignal();
        } else if (s instanceof Stmt.Return r) {
            Object v = r.value() == null ? null : eval(r.value(), env);
            throw new ReturnSignal(v);
        } else {
            // Function, TypeDef handled in first pass
        }
    }

    private void assign(Expr target, Object value, Environment env) {
        if (target instanceof Expr.Variable v) {
            env.set(v.name(), value);
            return;
        }
        if (target instanceof Expr.Member m) {
            Map<String, Object> obj = asObject(eval(m.target(), env));
            obj.put(m.name(), value);
            return;
        }
        if (target instanceof Expr.Index ix) {
            List<Object> arr = asArray(eval(ix.target(), env));
            int idx = (int) toLong(eval(ix.index(), env));
            if (idx < 0 || idx >= arr.size())
                throw new RuntimeException("Index out of bounds");
            arr.set(idx, value);
            return;
        }
        throw new RuntimeException("Invalid assignment target");
    }

    private Object eval(Expr e, Environment env) {
        tick();
        if (e instanceof Expr.Literal l)
            return l.value();
        if (e instanceof Expr.Variable v) {
            String name = v.name();
            if (env.hasLocal(name) || env.parent != null) {
                try {
                    return env.get(name);
                } catch (RuntimeException ex) {
                    /* fallthrough */ }
            }
            // Treat bare function name as function pointer value
            if (functions.containsKey(name))
                return new FunPtr(name);
            // Treat bare type name as constructor reference
            if (types.containsKey(name))
                return name;
            return env.get(name);
        }
        if (e instanceof Expr.Paren p)
            return eval(p.inner(), env);
        if (e instanceof Expr.NewArray na) {
            int len = (int) toLong(eval(na.length(), env));
            List<Object> arr = new ArrayList<>(len);
            // Fill with default values depending on base type; for user-defined types build
            // default object
            Object def;
            if ("int".equals(na.baseType()))
                def = 0L;
            else if ("real".equals(na.baseType()))
                def = 0.0;
            else if ("text".equals(na.baseType()))
                def = "";
            else if (na.baseType().endsWith("[]"))
                def = new ArrayList<>();
            else
                def = 0L; // user-defined type arrays default to null entries
            for (int i = 0; i < len; i++)
                arr.add(copyIfMutable(def));
            return arr;
        }
        if (e instanceof Expr.Member m) {
            Object target = eval(m.target(), env);
            if (target instanceof List) {
                if (m.name().equals("len"))
                    return (long) ((List<?>) target).size();
                throw new RuntimeException("Unknown array member: " + m.name());
            }
            if (target instanceof String s) {
                if (m.name().equals("len"))
                    return (long) s.length();
                throw new RuntimeException("Unknown text member: " + m.name());
            }
            Map<String, Object> obj = asObject(target);
            return obj.get(m.name());
        }
        if (e instanceof Expr.Index ix) {
            // Special-case: TypeName[len] creates a new array of that (user-defined) type
            if (ix.target() instanceof Expr.Variable v && types.containsKey(v.name())) {
                int len = (int) toLong(eval(ix.index(), env));
                List<Object> arr = new ArrayList<>(len);
                // Initialize user-defined type arrays with null (0)
                for (int i = 0; i < len; i++)
                    arr.add(0L);
                return arr;
            }
            List<Object> arr = asArray(eval(ix.target(), env));
            int idx = (int) toLong(eval(ix.index(), env));
            if (idx < 0 || idx >= arr.size())
                throw new RuntimeException("Index out of bounds");
            return arr.get(idx);
        }
        if (e instanceof Expr.Call c) {
            // Evaluate target; it can be a function name (variable resolves to FunPtr) or
            // direct function identifier
            Object target = null;
            if (c.target() instanceof Expr.Variable v) {
                // Try to treat as direct named function first
                if (functions.containsKey(v.name()))
                    target = new FunPtr(v.name());
            }
            if (target == null)
                target = eval(c.target(), env);
            List<Object> args = new ArrayList<>();
            for (Expr a : c.args())
                args.add(eval(a, env));
            if (target instanceof FunPtr fp)
                return callFunction(fp.name, args, env);
            if (target instanceof String s)
                return callFunction(s, args, env);
            throw new RuntimeException("Unsupported call target");
        }
        if (e instanceof Expr.Unary u) {
            Object x = eval(u.expr(), env);
            return switch (u.op()) {
            case "-" -> {
                if (x instanceof Double) {
                    yield -((Double) x);
                } else {
                    yield -toLong(x);
                }
            }
            default -> throw new RuntimeException("Unknown unary op " + u.op());
            };
        }
        if (e instanceof Expr.Binary b) {
            Object a = eval(b.left(), env);
            Object c = eval(b.right(), env);
            return switch (b.op()) {
            case "+" -> addOp(a, c);
            case "-" -> {
                if (a instanceof Double || c instanceof Double) {
                    yield toDouble(a) - toDouble(c);
                } else {
                    yield binLong(a, c, (x, y) -> x - y);
                }
            }
            case "*" -> {
                if (a instanceof Double || c instanceof Double) {
                    yield toDouble(a) * toDouble(c);
                } else {
                    yield binLong(a, c, (x, y) -> x * y);
                }
            }
            case "/" -> {
                if (a instanceof Double || c instanceof Double) {
                    yield toDouble(a) / toDouble(c);
                } else {
                    yield binLong(a, c, this::divOp);
                }
            }
            case "%" -> {
                if (a instanceof Double || c instanceof Double) {
                    yield toDouble(a) % toDouble(c);
                } else {
                    yield binLong(a, c, this::modOp);
                }
            }
            case "<<" -> binLong(a, c, (x, y) -> x << y);
            case ">>" -> binLong(a, c, (x, y) -> (x >>> y));
            case "&" -> binLong(a, c, (x, y) -> x & y);
            case "|" -> binLong(a, c, (x, y) -> x | y);
            case "^" -> binLong(a, c, (x, y) -> x ^ y);
            case "=" -> truth(equalsOp(a, c));
            case "<>" -> truth(!equalsOp(a, c));
            case "<" -> truth(cmpOp(a, c) < 0);
            case ">" -> truth(cmpOp(a, c) > 0);
            case "<=" -> truth(cmpOp(a, c) <= 0);
            case ">=" -> truth(cmpOp(a, c) >= 0);
            case "and" -> truth(isTruthy(a) && isTruthy(c));
            case "or" -> truth(isTruthy(a) || isTruthy(c));
            default -> throw new RuntimeException("Unknown op " + b.op());
            };
        }
        throw new RuntimeException("Unknown expression kind");
    }

    private Object addOp(Object a, Object b) {
        if (a instanceof String || b instanceof String)
            throw new RuntimeException("text '+' not supported; use format(...)");
        if (a instanceof List || b instanceof List)
            throw new RuntimeException("array concatenation is not supported");
        if (a instanceof Double || b instanceof Double)
            return toDouble(a) + toDouble(b);
        return toLong(a) + toLong(b);
    }

    private long toLong(Object v) {
        return v instanceof Long l ? l : (long) ((Double) v).doubleValue();
    }

    private double toDouble(Object v) {
        return v instanceof Double d ? d : ((Long) v).doubleValue();
    }

    private String stringify(Object v) {
        return v == null ? "null" : (v instanceof String s ? s : String.valueOf(v));
    }

    private boolean isTruthy(Object v) {
        return (v instanceof Long l ? l != 0 : (v instanceof Double d ? d != 0.0 : true));
    }

    private Object truth(boolean b) {
        return b ? 1L : 0L;
    }

    private boolean equalsOp(Object a, Object b) {
        // Numeric equality (handles mixed int/real)
        if (a instanceof Double || b instanceof Double)
            return toDouble(a) == toDouble(b);
        if (a instanceof Long && b instanceof Long)
            return ((Long) a).longValue() == ((Long) b).longValue();
        // Text equality
        if (a instanceof String || b instanceof String)
            return stringify(a).equals(stringify(b));
        // Fallback: reference/value equality without coercion (covers structs/maps vs
        // 0)
        return a == b || (a != null && a.equals(b));
    }

    private int cmpOp(Object a, Object b) {
        if (a instanceof Double || b instanceof Double)
            return Double.compare(toDouble(a), toDouble(b));
        if (a instanceof String || b instanceof String)
            return stringify(a).compareTo(stringify(b));
        return Long.compare(toLong(a), toLong(b));
    }

    @SuppressWarnings("unchecked")
    private Object copyIfMutable(Object v) {
        if (v instanceof List<?> l)
            return new ArrayList<>(l);
        if (v instanceof Map<?, ?> m)
            return new HashMap<>((Map<String, Object>) m);
        return v;
    }

    private interface LongOp {
        long apply(long a, long b);
    }

    private Object binLong(Object a, Object b, LongOp op) {
        return op.apply(toLong(a), toLong(b));
    }

    private long divOp(long a, long b) {
        if (b == 0)
            return a > 0 ? Long.MAX_VALUE : a < 0 ? Long.MIN_VALUE : 0;
        return a / b;
    }

    private long modOp(long a, long b) {
        if (b == 0)
            return 0;
        return a % b;
    }

    // Simple runtime conversions for varargs packing
    private Object convertTo(String typeName, Object v) {
        return switch (typeName) {
        case "int" -> (v instanceof Long) ? v : (long) toDouble(v);
        case "real" -> (v instanceof Double) ? v : toDouble(v);
        case "text" -> stringify(v);
        default -> v; // user types unsupported here; assume already correct
        };
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asObject(Object v) {
        if (v instanceof Long l && l == 0L) {
            throw new RuntimeException("panic: null object access");
        }
        return (Map<String, Object>) v;
    }

    @SuppressWarnings("unchecked")
    private List<Object> asArray(Object v) {
        return (List<Object>) v;
    }
}
