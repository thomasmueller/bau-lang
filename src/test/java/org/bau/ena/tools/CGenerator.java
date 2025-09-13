package org.bau.ena.tools;

import org.bau.ena.ast.Expr;
import org.bau.ena.ast.Stmt;

public final class CGenerator {
    public String generate(Stmt.Program program) {
        StringBuilder c = new StringBuilder();
        // Preamble: include Boehm GC and std headers
        c.append("#include <gc.h>\n");
        c.append("#include <stdio.h>\n");
        c.append("#include <stdint.h>\n");
        c.append("#include <string.h>\n\n");
        // Forward declarations for functions
        for (Stmt s : program.items()) {
            if (s instanceof Stmt.Function fn) {
                c.append(signature(fn)).append(";\n");
            }
        }
        c.append('\n');
        // Function definitions
        for (Stmt s : program.items()) {
            if (s instanceof Stmt.Function fn) {
                emitFunction(c, fn);
                c.append('\n');
            }
        }
        // Top-level: if there is a main() function, emit C main that calls it; else inline toplevel
        boolean hasMain = false;
        for (Stmt s : program.items())
            if (s instanceof Stmt.Function fn && "main".equals(fn.name())) { hasMain = true; break; }
        c.append("int main() { GC_init(); ");
        if (hasMain) {
            c.append("main_fn(); ");
        } else {
            // emit top-level statements as a synthetic function body
            for (Stmt s : program.items())
                if (!(s instanceof Stmt.Function)) emitStmt(c, s);
        }
        c.append("return 0; }\n");
        return c.toString();
    }

    private String signature(Stmt.Function fn) {
        // Map return type: null -> void, else int for simplicity for now
        String ret = fn.returnType() == null ? "void" : mapType(fn.returnType());
        StringBuilder sb = new StringBuilder();
        String cname = fn.name();
        if ("main".equals(cname)) cname = "main_fn"; // avoid C main clash with preamble main
        sb.append(ret).append(' ').append(cname).append('(');
        for (int i = 0; i < fn.params().length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(mapType(fn.params()[i].type())).append(' ').append(fn.params()[i].name());
        }
        sb.append(')');
        return sb.toString();
    }

    private String mapType(String t) {
        if (t == null) return "void";
        if ("int".equals(t)) return "int64_t";
        if ("real".equals(t)) return "double";
        if ("text".equals(t)) return "const char*";
        if (t.endsWith("[]")) return "void*";
        if ("fun".equals(t)) return "void*";
        return "void*";
    }

    private void emitFunction(StringBuilder c, Stmt.Function fn) {
        c.append(signature(fn)).append(" {\n");
        emitBlock(c, fn.body());
        c.append("}\n");
    }

    private void emitBlock(StringBuilder c, Stmt.Block b) {
        for (Stmt s : b.statements()) emitStmt(c, s);
    }

    private void emitStmt(StringBuilder c, Stmt s) {
        if (s instanceof Stmt.Assign a) {
            String lhs = emitExpr(a.target());
            String rhs = emitExpr(a.value());
            c.append("    ").append(lhs).append(" = ").append(rhs).append(";\n");
        } else if (s instanceof Stmt.ExprStmt e) {
            c.append("    (void)").append(emitExpr(e.expr())).append(";\n");
        } else if (s instanceof Stmt.If iff) {
            for (int i = 0; i < iff.conds().length; i++) {
                String kw = (i == 0 ? "if" : "else if");
                c.append("    ").append(kw).append(" (").append(emitExpr(iff.conds()[i])).append(") {\n");
                emitBlock(c, iff.blocks()[i]);
                c.append("    }\n");
            }
            if (iff.elseBlock() != null) {
                c.append("    else {\n");
                emitBlock(c, iff.elseBlock());
                c.append("    }\n");
            }
        } else if (s instanceof Stmt.Loop lp) {
            c.append("    while (").append(emitExpr(lp.cond())).append(") {\n");
            emitBlock(c, lp.body());
            c.append("    }\n");
        } else if (s instanceof Stmt.Return r) {
            if (r.value() == null) c.append("    return;\n");
            else c.append("    return ").append(emitExpr(r.value())).append(";\n");
        } else if (s instanceof Stmt.Exit) {
            c.append("    break;\n");
        }
    }

    private String emitExpr(Expr e) {
        if (e instanceof Expr.Literal l) {
            Object v = l.value();
            if (v instanceof String s) return '"' + s.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
            if (v instanceof Double d) return Double.toString(d);
            if (v instanceof Long li) return Long.toString(li) + "LL";
            return "0";
        }
        if (e instanceof Expr.Variable v) {
            return v.name();
        }
        if (e instanceof Expr.Binary b) {
            return "(" + emitExpr(b.left()) + " " + b.op() + " " + emitExpr(b.right()) + ")";
        }
        if (e instanceof Expr.Unary u) {
            return "(" + u.op() + emitExpr(u.expr()) + ")";
        }
        if (e instanceof Expr.Index ix) {
            return emitExpr(ix.target()) + "[" + emitExpr(ix.index()) + "]";
        }
        if (e instanceof Expr.Member m) {
            return emitExpr(m.target()) + "." + m.name();
        }
        if (e instanceof Expr.Call c) {
            StringBuilder sb = new StringBuilder();
            sb.append(emitExpr(c.target())).append('(');
            for (int i = 0; i < c.args().length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(emitExpr(c.args()[i]));
            }
            sb.append(')');
            return sb.toString();
        }
        if (e instanceof Expr.Paren p) {
            return "(" + emitExpr(p.inner()) + ")";
        }
        return "0";
    }
}


