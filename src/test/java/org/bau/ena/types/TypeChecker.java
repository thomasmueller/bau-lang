package org.bau.ena.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bau.ena.ast.Expr;
import org.bau.ena.ast.Stmt;

public final class TypeChecker {
    private Map<String, Type.Struct> structIndex = new HashMap<>();

    public static final class Scope {
        final Scope parent;
        final Map<String, Type> vars = new HashMap<>();
        final Map<String, Type.Struct> structs; // global only
        final Map<String, Type.Fun> funcs; // global only

        Scope(Scope parent, Map<String, Type.Struct> structs, Map<String, Type.Fun> funcs) {
            this.parent = parent;
            this.structs = structs;
            this.funcs = funcs;
        }

        Type lookupVar(String n) {
            if (vars.containsKey(n))
                return vars.get(n);
            return parent != null ? parent.lookupVar(n) : null;
        }
    }

    public record Result(Map<Expr, Type> exprTypes, Map<Stmt, Type> stmtTypes) {
    }

    public Result infer(Stmt.Program program) {
        Map<Expr, Type> exprT = new HashMap<>();
        Map<Stmt, Type> stmtT = new HashMap<>();
        Map<String, Type.Struct> structs = new HashMap<>();
        Map<String, Type.Fun> funcs = new HashMap<>();

        // collect types and functions
        for (Stmt item : program.items()) {
            if (item instanceof Stmt.TypeDef td) {
                Map<String, Type> fields = new HashMap<>();
                List<Type> ctorParams = new ArrayList<>();
                for (Stmt.Param p : td.fields()) {
                    Type t = fromName(p.type());
                    fields.put(p.name(), t);
                    ctorParams.add(t);
                }
                Type.Struct st = new Type.Struct(td.name(), fields);
                structs.put(td.name(), st);
                // Register a constructor-like function signature for the type name
                funcs.put(td.name(), new Type.Fun(ctorParams, st));
            } else if (item instanceof Stmt.Function fn) {
                List<Type> ps = new ArrayList<>();
                for (Stmt.Param p : fn.params())
                    ps.add(fromName(p.type()));
                Type ret = fn.returnType() == null ? Type.VOID : fromName(fn.returnType());
                funcs.put(fn.name(), new Type.Fun(ps, ret));
            }
        }

        this.structIndex = structs;
        Scope global = new Scope(null, structs, funcs);

        // infer top-level statements (globals)
        for (Stmt item : program.items()) {
            if (item instanceof Stmt.Assign a && a.target() instanceof Expr.Variable v) {
                Type vt = inferExpr(a.value(), global, exprT);
                global.vars.put(v.name(), vt);
                a.target().setType(vt);
                a.value().setType(vt);
            }
        }

        // check function bodies
        for (Stmt item : program.items()) {
            if (item instanceof Stmt.Function fn) {
                Scope s = new Scope(global, structs, funcs);
                for (int i = 0; i < fn.params().length; i++)
                    s.vars.put(fn.params()[i].name(), fromName(fn.params()[i].type()));
                inferBlock(fn.body(), s, exprT, stmtT);
                // annotate function body expressions
                for (Expr e : exprT.keySet())
                    e.setType(exprT.get(e));
            }
        }
        return new Result(exprT, stmtT);
    }

    private void inferBlock(Stmt.Block b, Scope s, Map<Expr, Type> exprT, Map<Stmt, Type> stmtT) {
        for (Stmt st : b.statements())
            inferStmt(st, s, exprT, stmtT);
    }

    private void inferStmt(Stmt st, Scope s, Map<Expr, Type> exprT, Map<Stmt, Type> stmtT) {
        if (st instanceof Stmt.Assign a) {
            // Infer both sides so target expressions (e.g., struct members, array indices)
            // gain types
            Type t = inferExpr(a.value(), s, exprT);
            inferExpr(a.target(), s, exprT);
            if (a.target() instanceof Expr.Variable v)
                s.vars.put(v.name(), t);
            stmtT.put(st, Type.VOID);
        } else if (st instanceof Stmt.ExprStmt e) {
            inferExpr(e.expr(), s, exprT);
            stmtT.put(st, Type.VOID);
        } else if (st instanceof Stmt.If iff) {
            for (Expr c : iff.conds())
                inferExpr(c, s, exprT);
            for (Stmt.Block bl : iff.blocks())
                inferBlock(bl, s, exprT, stmtT);
            if (iff.elseBlock() != null)
                inferBlock(iff.elseBlock(), s, exprT, stmtT);
            stmtT.put(st, Type.VOID);
        } else if (st instanceof Stmt.Loop lp) {
            inferExpr(lp.cond(), s, exprT);
            inferBlock(lp.body(), s, exprT, stmtT);
            stmtT.put(st, Type.VOID);
        } else if (st instanceof Stmt.Exit) {
            stmtT.put(st, Type.VOID);
        } else if (st instanceof Stmt.Return r) {
            Type t = r.value() == null ? Type.VOID : inferExpr(r.value(), s, exprT);
            stmtT.put(st, t);
        }
    }

    private Type inferExpr(Expr e, Scope s, Map<Expr, Type> exprT) {
        if (e instanceof Expr.Literal l) {
            Type t = (l.value() instanceof Long) ? Type.INT : (l.value() instanceof Double ? Type.REAL : Type.TEXT);
            exprT.put(e, t);
            e.setType(t);
            return t;
        }
        if (e instanceof Expr.Variable v) {
            Type t = s.lookupVar(v.name());
            if (t == null)
                t = Type.UNKNOWN;
            exprT.put(e, t);
            e.setType(t);
            return t;
        }
        if (e instanceof Expr.Paren p) {
            Type t = inferExpr(p.inner(), s, exprT);
            exprT.put(e, t);
            e.setType(t);
            return t;
        }
        if (e instanceof Expr.NewArray na) {
            // Infer length expression type as well
            inferExpr(na.length(), s, exprT);
            Type elem = fromName(na.baseType());
            Type t = new Type.Array(elem);
            exprT.put(e, t);
            e.setType(t);
            return t;
        }
        if (e instanceof Expr.Index ix) {
            // Special case: TypeName[len] is array creation of a user-defined type
            if (ix.target() instanceof Expr.Variable v && s.structs.containsKey(v.name())) {
                Type.Struct st = s.structs.get(v.name());
                inferExpr(ix.index(), s, exprT);
                Type t = new Type.Array(st);
                exprT.put(e, t);
                e.setType(t);
                return t;
            }
            Type ct = inferExpr(ix.target(), s, exprT);
            Type t = (ct instanceof Type.Array arr) ? arr.elem() : Type.UNKNOWN;
            inferExpr(ix.index(), s, exprT);
            exprT.put(e, t);
            e.setType(t);
            return t;
        }
        if (e instanceof Expr.Member m) {
            Type tt = inferExpr(m.target(), s, exprT);
            if (tt instanceof Type.Array) {
                exprT.put(e, Type.INT);
                e.setType(Type.INT);
                return Type.INT;
            }
            if (tt instanceof Type.Struct st) {
                Type t = st.fields().getOrDefault(m.name(), Type.UNKNOWN);
                exprT.put(e, t);
                e.setType(t);
                return t;
            }
            exprT.put(e, Type.UNKNOWN);
            e.setType(Type.UNKNOWN);
            return Type.UNKNOWN;
        }
        if (e instanceof Expr.Call c) {
            // if variable function name, use signature (functions or constructors); else
            // unknown
            if (c.target() instanceof Expr.Variable fv) {
                Type.Fun f = s.funcs.get(fv.name());
                if (f != null) {
                    // varargs: last param may be array; result type stays f.ret
                    for (Expr a : c.args())
                        inferExpr(a, s, exprT);
                    exprT.put(e, f.ret());
                    e.setType(f.ret());
                    return f.ret();
                }
                Type.Struct st = s.structs.get(fv.name());
                if (st != null) {
                    for (Expr a : c.args())
                        inferExpr(a, s, exprT);
                    exprT.put(e, st);
                    e.setType(st);
                    return st;
                }
                if ("format".equals(fv.name())) {
                    for (Expr a : c.args())
                        inferExpr(a, s, exprT);
                    exprT.put(e, Type.TEXT);
                    e.setType(Type.TEXT);
                    return Type.TEXT;
                }
            }
            for (Expr a : c.args())
                inferExpr(a, s, exprT);
            exprT.put(e, Type.UNKNOWN);
            e.setType(Type.UNKNOWN);
            return Type.UNKNOWN;
        }
        if (e instanceof Expr.Unary u) {
            Type t = inferExpr(u.expr(), s, exprT);
            exprT.put(e, t);
            return t;
        }
        if (e instanceof Expr.Binary b) {
            Type lt = inferExpr(b.left(), s, exprT);
            Type rt = inferExpr(b.right(), s, exprT);
            Type t;
            switch (b.op()) {
            case "+":
                t = (lt instanceof Type.Real || rt instanceof Type.Real) ? Type.REAL
                        : (lt instanceof Type.Array ? lt : Type.INT);
                break;
            case "-":
            case "*":
            case "/":
            case "%":
                t = (lt instanceof Type.Real || rt instanceof Type.Real) ? Type.REAL : Type.INT;
                break;
            case "<<":
            case ">>":
            case "&":
            case "|":
            case "^":
                t = Type.INT;
                break;
            case "=":
            case "<>":
            case "<":
            case "<=":
            case ">":
            case ">=":
            case "and":
            case "or":
                t = Type.INT;
                break;
            default:
                t = Type.UNKNOWN;
                break;
            }
            exprT.put(e, t);
            e.setType(t);
            return t;
        }
        exprT.put(e, Type.UNKNOWN);
        return Type.UNKNOWN;
    }

    private Type fromName(String n) {
        if (n == null)
            return Type.UNKNOWN;
        if ("int".equals(n))
            return Type.INT;
        if ("real".equals(n))
            return Type.REAL;
        if ("text".equals(n))
            return Type.TEXT;
        if (n.endsWith("[]")) {
            String base = n.substring(0, n.length() - 2);
            return new Type.Array(fromName(base));
        }
        Type.Struct st = structIndex.get(n);
        return st != null ? st : new Type.Struct(n, Map.of());
    }
}
