package org.bau.ena.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bau.ena.ast.Expr;
import org.bau.ena.ast.Stmt;

public final class TypeChecker {
    private Map<String, EnaType.Struct> structIndex = new HashMap<>();

    public static final class Scope {
        final Scope parent;
        final Map<String, EnaType> vars = new HashMap<>();
        final Map<String, EnaType.Struct> structs; // global only
        final Map<String, EnaType.Fun> funcs; // global only

        Scope(Scope parent, Map<String, EnaType.Struct> structs, Map<String, EnaType.Fun> funcs) {
            this.parent = parent;
            this.structs = structs;
            this.funcs = funcs;
        }

        EnaType lookupVar(String n) {
            if (vars.containsKey(n))
                return vars.get(n);
            return parent != null ? parent.lookupVar(n) : null;
        }
    }

    public record Result(Map<Expr, EnaType> exprTypes, Map<Stmt, EnaType> stmtTypes) {
    }

    public Result infer(Stmt.EnaProgram program) {
        Map<Expr, EnaType> exprT = new HashMap<>();
        Map<Stmt, EnaType> stmtT = new HashMap<>();
        Map<String, EnaType.Struct> structs = new HashMap<>();
        Map<String, EnaType.Fun> funcs = new HashMap<>();

        // collect EnaTypes and functions
        for (Stmt item : program.items()) {
            if (item instanceof Stmt.EnaTypeDef td) {
                Map<String, EnaType> fields = new HashMap<>();
                List<EnaType> ctorParams = new ArrayList<>();
                for (Stmt.Param p : td.fields()) {
                    EnaType t = fromName(p.type());
                    fields.put(p.name(), t);
                    ctorParams.add(t);
                }
                EnaType.Struct st = new EnaType.Struct(td.name(), fields);
                structs.put(td.name(), st);
                // Register a constructor-like function signature for the EnaType name
                funcs.put(td.name(), new EnaType.Fun(ctorParams, st));
            } else if (item instanceof Stmt.Function fn) {
                List<EnaType> ps = new ArrayList<>();
                for (Stmt.Param p : fn.params())
                    ps.add(fromName(p.type()));
                EnaType ret = fn.returnType() == null ? EnaType.VOID : fromName(fn.returnType());
                funcs.put(fn.name(), new EnaType.Fun(ps, ret));
            }
        }

        this.structIndex = structs;
        Scope global = new Scope(null, structs, funcs);

        // infer top-level statements (globals)
        for (Stmt item : program.items()) {
            if (item instanceof Stmt.Assign a && a.target() instanceof Expr.Variable v) {
                EnaType vt = inferExpr(a.value(), global, exprT);
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

    private void inferBlock(Stmt.Block b, Scope s, Map<Expr, EnaType> exprT, Map<Stmt, EnaType> stmtT) {
        for (Stmt st : b.statements())
            inferStmt(st, s, exprT, stmtT);
    }

    private void inferStmt(Stmt st, Scope s, Map<Expr, EnaType> exprT, Map<Stmt, EnaType> stmtT) {
        if (st instanceof Stmt.Assign a) {
            // Infer both sides so target expressions (e.g., struct members, array indices)
            // gain EnaTypes
            EnaType t = inferExpr(a.value(), s, exprT);
            inferExpr(a.target(), s, exprT);
            if (a.target() instanceof Expr.Variable v)
                s.vars.put(v.name(), t);
            stmtT.put(st, EnaType.VOID);
        } else if (st instanceof Stmt.ExprStmt e) {
            inferExpr(e.expr(), s, exprT);
            stmtT.put(st, EnaType.VOID);
        } else if (st instanceof Stmt.If iff) {
            for (Expr c : iff.conds())
                inferExpr(c, s, exprT);
            for (Stmt.Block bl : iff.blocks())
                inferBlock(bl, s, exprT, stmtT);
            if (iff.elseBlock() != null)
                inferBlock(iff.elseBlock(), s, exprT, stmtT);
            stmtT.put(st, EnaType.VOID);
        } else if (st instanceof Stmt.Loop lp) {
            inferExpr(lp.cond(), s, exprT);
            inferBlock(lp.body(), s, exprT, stmtT);
            stmtT.put(st, EnaType.VOID);
        } else if (st instanceof Stmt.Exit) {
            stmtT.put(st, EnaType.VOID);
        } else if (st instanceof Stmt.Return r) {
            EnaType t = r.value() == null ? EnaType.VOID : inferExpr(r.value(), s, exprT);
            stmtT.put(st, t);
        }
    }

    private EnaType inferExpr(Expr e, Scope s, Map<Expr, EnaType> exprT) {
        if (e instanceof Expr.Literal l) {
            EnaType t = (l.value() instanceof Long) ? EnaType.INT : (l.value() instanceof Double ? EnaType.REAL : EnaType.TEXT);
            exprT.put(e, t);
            e.setType(t);
            return t;
        }
        if (e instanceof Expr.Variable v) {
            EnaType t = s.lookupVar(v.name());
            if (t == null)
                t = EnaType.UNKNOWN;
            exprT.put(e, t);
            e.setType(t);
            return t;
        }
        if (e instanceof Expr.Paren p) {
            EnaType t = inferExpr(p.inner(), s, exprT);
            exprT.put(e, t);
            e.setType(t);
            return t;
        }
        if (e instanceof Expr.NewArray na) {
            // Infer length expression EnaType as well
            inferExpr(na.length(), s, exprT);
            EnaType elem = fromName(na.baseType());
            EnaType t = new EnaType.Array(elem);
            exprT.put(e, t);
            e.setType(t);
            return t;
        }
        if (e instanceof Expr.Index ix) {
            // Special case: EnaTypeName[len] is array creation of a user-defined EnaType
            if (ix.target() instanceof Expr.Variable v && s.structs.containsKey(v.name())) {
                EnaType.Struct st = s.structs.get(v.name());
                inferExpr(ix.index(), s, exprT);
                EnaType t = new EnaType.Array(st);
                exprT.put(e, t);
                e.setType(t);
                return t;
            }
            EnaType ct = inferExpr(ix.target(), s, exprT);
            EnaType t = (ct instanceof EnaType.Array arr) ? arr.elem() : EnaType.UNKNOWN;
            inferExpr(ix.index(), s, exprT);
            exprT.put(e, t);
            e.setType(t);
            return t;
        }
        if (e instanceof Expr.Member m) {
            EnaType tt = inferExpr(m.target(), s, exprT);
            if (tt instanceof EnaType.Array) {
                exprT.put(e, EnaType.INT);
                e.setType(EnaType.INT);
                return EnaType.INT;
            }
            if (tt instanceof EnaType.Struct st) {
                EnaType t = st.fields().getOrDefault(m.name(), EnaType.UNKNOWN);
                exprT.put(e, t);
                e.setType(t);
                return t;
            }
            exprT.put(e, EnaType.UNKNOWN);
            e.setType(EnaType.UNKNOWN);
            return EnaType.UNKNOWN;
        }
        if (e instanceof Expr.Call c) {
            // if variable function name, use signature (functions or constructors); else
            // unknown
            if (c.target() instanceof Expr.Variable fv) {
                EnaType.Fun f = s.funcs.get(fv.name());
                if (f != null) {
                    // varargs: last param may be array; result EnaType stays f.ret
                    for (Expr a : c.args())
                        inferExpr(a, s, exprT);
                    exprT.put(e, f.ret());
                    e.setType(f.ret());
                    return f.ret();
                }
                EnaType.Struct st = s.structs.get(fv.name());
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
                    exprT.put(e, EnaType.TEXT);
                    e.setType(EnaType.TEXT);
                    return EnaType.TEXT;
                }
            }
            for (Expr a : c.args())
                inferExpr(a, s, exprT);
            exprT.put(e, EnaType.UNKNOWN);
            e.setType(EnaType.UNKNOWN);
            return EnaType.UNKNOWN;
        }
        if (e instanceof Expr.Unary u) {
            EnaType t = inferExpr(u.expr(), s, exprT);
            exprT.put(e, t);
            return t;
        }
        if (e instanceof Expr.Binary b) {
            EnaType lt = inferExpr(b.left(), s, exprT);
            EnaType rt = inferExpr(b.right(), s, exprT);
            EnaType t;
            switch (b.op()) {
            case "+":
                t = (lt instanceof EnaType.Real || rt instanceof EnaType.Real) ? EnaType.REAL
                        : (lt instanceof EnaType.Array ? lt : EnaType.INT);
                break;
            case "-":
            case "*":
            case "/":
            case "%":
                t = (lt instanceof EnaType.Real || rt instanceof EnaType.Real) ? EnaType.REAL : EnaType.INT;
                break;
            case "<<":
            case ">>":
            case "&":
            case "|":
            case "^":
                t = EnaType.INT;
                break;
            case "=":
            case "<>":
            case "<":
            case "<=":
            case ">":
            case ">=":
            case "and":
            case "or":
                t = EnaType.INT;
                break;
            default:
                t = EnaType.UNKNOWN;
                break;
            }
            exprT.put(e, t);
            e.setType(t);
            return t;
        }
        exprT.put(e, EnaType.UNKNOWN);
        return EnaType.UNKNOWN;
    }

    private EnaType fromName(String n) {
        if (n == null)
            return EnaType.UNKNOWN;
        if ("int".equals(n))
            return EnaType.INT;
        if ("real".equals(n))
            return EnaType.REAL;
        if ("text".equals(n))
            return EnaType.TEXT;
        if (n.endsWith("[]")) {
            String base = n.substring(0, n.length() - 2);
            return new EnaType.Array(fromName(base));
        }
        EnaType.Struct st = structIndex.get(n);
        return st != null ? st : new EnaType.Struct(n, Map.of());
    }
}
