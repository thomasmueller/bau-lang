package org.bau.ena.vm.stack;

import org.bau.ena.ast.Expr;
import org.bau.ena.ast.Stmt;
import org.bau.ena.types.Type;
import org.bau.ena.types.TypeChecker;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StackCompiler {
    private final StackBytecode bc = new StackBytecode();
    private final Map<String, Integer> funcIndex = new HashMap<>();
    private final Map<String, Integer> globalIndex = new HashMap<>();
    private Map<String, Integer> currentLocals = null;
    private int currentNextLocal = 0;
    private final Deque<List<Integer>> breakStack = new ArrayDeque<>();
    private final java.util.Set<String> typeNames = new java.util.HashSet<>();
    private Map<Stmt, Type> stmtTypes = java.util.Collections.emptyMap();
    private int loopDepth = 0; // track loop nesting for 'exit'
    private Map<String, java.util.List<Type>> funcParamTypes = new java.util.HashMap<>();

    public StackBytecode compile(Stmt.Program program) {
        // Pass 1: collect type names, functions, and globals (no stubs yet)
        for (Stmt item : program.items()) {
            if (item instanceof Stmt.TypeDef td) {
                typeNames.add(td.name());
            }
        }
        for (Stmt item : program.items()) {
            if (item instanceof Stmt.Function fn) {
                funcIndex.put(fn.name(), bc.functions.size());
                int locals = fn.params().length + 16;
                bc.functions.add(new StackBytecode.Func(fn.name(), locals));
            } else if (item instanceof Stmt.TypeDef td) {
                // record type and constructor stub
                typeNames.add(td.name());
                String[] fields = new String[td.fields().length];
                String[] ftypes = new String[td.fields().length];
                for (int i = 0; i < fields.length; i++) {
                    fields[i] = td.fields()[i].name();
                    ftypes[i] = td.fields()[i].type();
                }
                bc.addType(td.name(), fields, ftypes);
                // register constructor as a function index now
                funcIndex.put(td.name(), bc.functions.size());
                bc.functions.add(new StackBytecode.Func(td.name(), td.fields().length + 4));
            } else
                ensureGlobal(item);
        }
        // No synthetic builtins; format/println are lowered inline at call-sites
        // Pass 2: run lightweight type inference to annotate expressions
        TypeChecker.Result tr = new TypeChecker().infer(program);
        this.stmtTypes = tr.stmtTypes();
        buildFuncParamTypes(program);
        // Pass 3: emit functions now that all names are known
        for (Stmt item : program.items()) {
            if (item instanceof Stmt.Function fn) {
                validateFunctionReturns(fn);
                validateCalls(fn.body());
                validateFields(fn.body());
                validateTypesAndVars(fn.body());
                compileFunction(fn);
            }
        }
        // emit top-level as implicit main if no main
        if (!funcIndex.containsKey("main")) {
            StackBytecode.Func f = new StackBytecode.Func("<toplevel>", 0);
            bc.functions.add(f);
            compileBlockAs(f.code, new Stmt.Block(program.items(), 1, 1));
        }
        return bc;
    }

    private void validateFunctionReturns(Stmt.Function fn) {
        Type expected = (fn.returnType() == null) ? Type.VOID : resolveType(fn.returnType());
        validateReturnsInBlock(fn.body(), expected, fn.name());
    }

    private void buildFuncParamTypes(Stmt.Program program) {
        for (Stmt it : program.items())
            if (it instanceof Stmt.Function fn) {
                java.util.ArrayList<Type> ps = new java.util.ArrayList<>();
                for (Stmt.Param p : fn.params())
                    ps.add(resolveType(p.type()));
                funcParamTypes.put(fn.name(), ps);
            }
        for (Stmt it : program.items())
            if (it instanceof Stmt.TypeDef td) {
                java.util.ArrayList<Type> ps = new java.util.ArrayList<>();
                for (Stmt.Param p : td.fields())
                    ps.add(resolveType(p.type()));
                funcParamTypes.put(td.name(), ps);
            }
        // println(args text[])
        funcParamTypes.put("println", java.util.List.of(new Type.Array(Type.TEXT)));
        // puts(out text)
        funcParamTypes.put("puts", java.util.List.of(Type.TEXT));
        // format is a builtin that accepts any number of arguments (treated in
        // compileExpr)
        funcParamTypes.put("format", java.util.List.of(new Type.Array(Type.TEXT)));
    }

    private void validateCalls(Stmt.Block body) {
        for (Stmt s : body.statements())
            validateCallsInStmt(s);
    }

    private void validateCallsInStmt(Stmt s) {
        if (s instanceof Stmt.Assign a) {
            validateCallsInExpr(a.target());
            validateCallsInExpr(a.value());
        } else if (s instanceof Stmt.ExprStmt e) {
            validateCallsInExpr(e.expr());
        } else if (s instanceof Stmt.If iff) {
            for (Expr c : iff.conds())
                validateCallsInExpr(c);
            for (Stmt.Block b : iff.blocks())
                validateCalls(b);
            if (iff.elseBlock() != null)
                validateCalls(iff.elseBlock());
        } else if (s instanceof Stmt.Loop lp) {
            validateCallsInExpr(lp.cond());
            validateCalls(lp.body());
        } else if (s instanceof Stmt.Return r) {
            if (r.value() != null)
                validateCallsInExpr(r.value());
        }
    }

    private void validateCallsInExpr(Expr e) {
        if (e instanceof Expr.Call c) {
            // target must be a variable referring to function/constructor/builtin
            if (c.target() instanceof Expr.Variable fv) {
                if ("format".equals(fv.name())) {
                    for (Expr a : c.args())
                        validateCallsInExpr(a);
                    return;
                }
                java.util.List<Type> params = funcParamTypes.get(fv.name());
                if ("println".equals(fv.name())) {
                    for (Expr a : c.args())
                        validateCallsInExpr(a);
                    return;
                }
                if (params == null)
                    throw new RuntimeException("unknown function: " + fv.name());
                int provided = c.args().length;
                boolean varargs = !params.isEmpty() && (params.get(params.size() - 1) instanceof Type.Array);
                int fixed = varargs ? params.size() - 1 : params.size();
                if (!varargs) {
                    if (provided != params.size())
                        throw new RuntimeException("arity mismatch for " + fv.name() + ": expected " + params.size()
                                + ", got " + provided);
                } else {
                    if (provided < fixed)
                        throw new RuntimeException("arity mismatch for " + fv.name() + ": expected at least " + fixed
                                + ", got " + provided);
                }
            } else {
                throw new RuntimeException("call target is not callable");
            }
            for (Expr a : c.args())
                validateCallsInExpr(a);
            return;
        }
        if (e instanceof Expr.Binary b) {
            validateCallsInExpr(b.left());
            validateCallsInExpr(b.right());
            return;
        }
        if (e instanceof Expr.Unary u) {
            validateCallsInExpr(u.expr());
            return;
        }
        if (e instanceof Expr.Index ix) {
            validateCallsInExpr(ix.target());
            validateCallsInExpr(ix.index());
            return;
        }
        if (e instanceof Expr.Member m) {
            validateCallsInExpr(m.target());
            return;
        }
        if (e instanceof Expr.Paren p) {
            validateCallsInExpr(p.inner());
            return;
        }
        // literals/variables ok
    }

    private void validateFields(Stmt.Block body) {
        for (Stmt s : body.statements())
            validateFieldsInStmt(s);
    }

    private void validateFieldsInStmt(Stmt s) {
        if (s instanceof Stmt.Assign a) {
            validateFieldsInExpr(a.target());
            validateFieldsInExpr(a.value());
        } else if (s instanceof Stmt.ExprStmt e) {
            validateFieldsInExpr(e.expr());
        } else if (s instanceof Stmt.If iff) {
            for (Expr c : iff.conds())
                validateFieldsInExpr(c);
            for (Stmt.Block b : iff.blocks())
                validateFields(b);
            if (iff.elseBlock() != null)
                validateFields(iff.elseBlock());
        } else if (s instanceof Stmt.Loop lp) {
            validateFieldsInExpr(lp.cond());
            validateFields(lp.body());
        } else if (s instanceof Stmt.Return r) {
            if (r.value() != null)
                validateFieldsInExpr(r.value());
        }
    }

    private void validateFieldsInExpr(Expr e) {
        if (e instanceof Expr.Member m) {
            if ("len".equals(m.name())) {
                validateFieldsInExpr(m.target());
                return;
            }
            Type tt = m.target().type();
            if (!(tt instanceof Type.Struct st))
                throw new RuntimeException("field access on non-struct type");
            int tid = bc.types.indexOf(st.name());
            if (tid < 0)
                throw new RuntimeException("unknown struct type: " + st.name());
            String[] flds = bc.typeFields.get(tid);
            boolean found = false;
            for (String f : flds)
                if (f.equals(m.name())) {
                    found = true;
                    break;
                }
            if (!found)
                throw new RuntimeException("unknown field: " + m.name() + " on " + st.name());
            validateFieldsInExpr(m.target());
            return;
        }
        if (e instanceof Expr.Binary b) {
            validateFieldsInExpr(b.left());
            validateFieldsInExpr(b.right());
            return;
        }
        if (e instanceof Expr.Unary u) {
            validateFieldsInExpr(u.expr());
            return;
        }
        if (e instanceof Expr.Index ix) {
            validateFieldsInExpr(ix.target());
            validateFieldsInExpr(ix.index());
            return;
        }
        if (e instanceof Expr.Paren p) {
            validateFieldsInExpr(p.inner());
            return;
        }
        if (e instanceof Expr.Call c) {
            for (Expr a : c.args())
                validateFieldsInExpr(a);
            if (!(c.target() instanceof Expr.Variable))
                validateFieldsInExpr(c.target());
            return;
        }
        // literals/variables ok
    }

    private void validateTypesAndVars(Stmt.Block body) {
        for (Stmt s : body.statements())
            validateTypesAndVarsInStmt(s);
    }

    private void validateTypesAndVarsInStmt(Stmt s) {
        if (s instanceof Stmt.Assign a) {
            // LHS variable declarations are allowed before being defined; don't flag as
            // undefined
            if (a.target() instanceof Expr.Variable) {
                validateTypesAndVarsInExpr(a.value());
            } else {
                validateTypesAndVarsInExpr(a.target());
                validateTypesAndVarsInExpr(a.value());
            }
        } else if (s instanceof Stmt.ExprStmt e) {
            validateTypesAndVarsInExpr(e.expr());
        } else if (s instanceof Stmt.If iff) {
            for (Expr c : iff.conds())
                validateTypesAndVarsInExpr(c);
            for (Stmt.Block b : iff.blocks())
                validateTypesAndVars(b);
            if (iff.elseBlock() != null)
                validateTypesAndVars(iff.elseBlock());
        } else if (s instanceof Stmt.Loop lp) {
            validateTypesAndVarsInExpr(lp.cond());
            validateTypesAndVars(lp.body());
        } else if (s instanceof Stmt.Return r) {
            if (r.value() != null)
                validateTypesAndVarsInExpr(r.value());
        }
    }

    private void validateTypesAndVarsInExpr(Expr e) {
        if (e instanceof Expr.Variable v) {
            if (v.type() instanceof Type.Unknown)
                throw new RuntimeException("undefined variable: " + v.name());
            return;
        }
        if (e instanceof Expr.Unary u) {
            if (!(u.expr().type() instanceof Type.Int || u.expr().type() instanceof Type.Real))
                throw new RuntimeException("type error: unary '-' on non-number");
            validateTypesAndVarsInExpr(u.expr());
            return;
        }
        if (e instanceof Expr.Binary b) {
            Type lt = b.left().type();
            Type rt = b.right().type();
            String op = b.op();
            boolean lNum = lt instanceof Type.Int || lt instanceof Type.Real;
            boolean rNum = rt instanceof Type.Int || rt instanceof Type.Real;
            boolean anyText = lt instanceof Type.Text || rt instanceof Type.Text;
            switch (op) {
            case "+":
                if (lNum && rNum) {
                    /* ok */ } else if (anyText) {
                    throw new RuntimeException("type error: '+' on text is not supported; use format(...)");
                } else
                    throw new RuntimeException("type error: '+' requires numbers or text");
                break;
            case "-":
            case "*":
            case "/":
                if (!(lNum && rNum))
                    throw new RuntimeException("type error: arithmetic requires numbers");
                break;
            case "%":
            case "<<":
            case ">>":
            case "&":
            case "|":
            case "^":
                if (!(lt instanceof Type.Int && rt instanceof Type.Int))
                    throw new RuntimeException("type error: bitwise/shift require int operands");
                break;
            case "=":
            case "<>":
            case "<":
            case "<=":
            case ">":
            case ">=":
                if (op.equals("=") || op.equals("<>")) {
                    boolean rightZero = (b.right() instanceof Expr.Literal lr
                            && ((lr.value() instanceof Long && ((Long) lr.value()) == 0L)
                                    || (lr.value() instanceof Double && ((Double) lr.value()) == 0.0)));
                    boolean leftZero = (b.left() instanceof Expr.Literal ll
                            && ((ll.value() instanceof Long && ((Long) ll.value()) == 0L)
                                    || (ll.value() instanceof Double && ((Double) ll.value()) == 0.0)));
                    boolean structVsZero = ((lt instanceof Type.Struct) && rightZero)
                            || ((rt instanceof Type.Struct) && leftZero)
                            || ((b.left() instanceof Expr.Member) && rightZero)
                            || ((b.right() instanceof Expr.Member) && leftZero);
                    if (structVsZero) {
                        // allow struct null comparison syntactically
                        break;
                    }
                }
                if (!(lNum && rNum) && !(lt instanceof Type.Text && rt instanceof Type.Text))
                    throw new RuntimeException(
                            "type error: comparison requires numbers or text (or struct compared to 0 with '=' or '<>')");
                break;
            case "and":
            case "or":
                // accept any types (truthiness semantics)
                break;
            default:
                break;
            }
            validateTypesAndVarsInExpr(b.left());
            validateTypesAndVarsInExpr(b.right());
            return;
        }
        if (e instanceof Expr.Index ix) {
            boolean isTypeArrayCtor = (ix.target() instanceof Expr.Variable v) && typeNames.contains(v.name());
            if (!isTypeArrayCtor && !(ix.target().type() instanceof Type.Array)) {
                // be permissive to support TypeName[len]
            }
            if (!isTypeArrayCtor && !(ix.index().type() instanceof Type.Int))
                throw new RuntimeException("type error: array index must be int");
            validateTypesAndVarsInExpr(ix.target());
            validateTypesAndVarsInExpr(ix.index());
            return;
        }
        if (e instanceof Expr.Member m) {
            if ("len".equals(m.name())) {
                Type tt = m.target().type();
                if (!(tt instanceof Type.Array || tt instanceof Type.Text))
                    throw new RuntimeException("type error: len on non-array/text");
                validateTypesAndVarsInExpr(m.target());
                return;
            }
            validateTypesAndVarsInExpr(m.target());
            return;
        }
        if (e instanceof Expr.Call c) {
            // println accepts any types; just validate its args recursively
            if (c.target() instanceof Expr.Variable fv && "println".equals(fv.name())) {
                for (Expr a : c.args())
                    validateTypesAndVarsInExpr(a);
                return;
            }
            for (Expr a : c.args())
                validateTypesAndVarsInExpr(a);
            if (!(c.target() instanceof Expr.Variable))
                validateTypesAndVarsInExpr(c.target());
            return;
        }
        if (e instanceof Expr.Paren p) {
            validateTypesAndVarsInExpr(p.inner());
            return;
        }
        if (e instanceof Expr.NewArray na) {
            if (!(na.length().type() instanceof Type.Int || na.length().type() instanceof Type.Real))
                throw new RuntimeException("type error: array length must be number");
            validateTypesAndVarsInExpr(na.length());
            return;
        }
    }

    private void validateReturnsInBlock(Stmt.Block b, Type expected, String fname) {
        for (Stmt s : b.statements())
            validateReturnsInStmt(s, expected, fname);
    }

    private void validateReturnsInStmt(Stmt s, Type expected, String fname) {
        if (s instanceof Stmt.Return r) {
            Type actual = stmtTypes.getOrDefault(r, Type.UNKNOWN);
            if (expected instanceof Type.Void) {
                if (!(actual instanceof Type.Void))
                    throw new RuntimeException("type error: function " + fname + " should not return a value");
            } else {
                if (actual instanceof Type.Void)
                    throw new RuntimeException(
                            "type error: function " + fname + " must return a value of type " + typeName(expected));
                if (!typesCompatible(actual, expected))
                    throw new RuntimeException("type error: function " + fname + " returns " + typeName(actual)
                            + ", expected " + typeName(expected));
            }
        } else if (s instanceof Stmt.Block bl) {
            validateReturnsInBlock(bl, expected, fname);
        } else if (s instanceof Stmt.If iff) {
            for (Stmt.Block bl : iff.blocks())
                validateReturnsInBlock(bl, expected, fname);
            if (iff.elseBlock() != null)
                validateReturnsInBlock(iff.elseBlock(), expected, fname);
        } else if (s instanceof Stmt.Loop lp) {
            validateReturnsInBlock(lp.body(), expected, fname);
        } else if (s instanceof Stmt.Function || s instanceof Stmt.TypeDef || s instanceof Stmt.Program) {
            // ignore nested defs (not present) / program wrapper
        }
    }

    private boolean typesCompatible(Type a, Type b) {
        if (a instanceof Type.Unknown || b instanceof Type.Unknown)
            return true;
        if (a.getClass() == b.getClass()) {
            if (a instanceof Type.Struct sa && b instanceof Type.Struct sb)
                return sa.name().equals(sb.name());
            if (a instanceof Type.Array aa && b instanceof Type.Array ab)
                return typesCompatible(aa.elem(), ab.elem());
            if (a instanceof Type.Fun fa && b instanceof Type.Fun fb) {
                if (fa.params().size() != fb.params().size())
                    return false;
                for (int i = 0; i < fa.params().size(); i++)
                    if (!typesCompatible(fa.params().get(i), fb.params().get(i)))
                        return false;
                return typesCompatible(fa.ret(), fb.ret());
            }
            return true;
        }
        // allow int to real implicit? Language specifies conversions via functions, not
        // implicit here
        return false;
    }

    private String typeName(Type t) {
        if (t instanceof Type.Int)
            return "int";
        if (t instanceof Type.Real)
            return "real";
        if (t instanceof Type.Text)
            return "text";
        if (t instanceof Type.Void)
            return "void";
        if (t instanceof Type.Unknown)
            return "UNKNOWN";
        if (t instanceof Type.Struct st)
            return st.name();
        if (t instanceof Type.Array ar)
            return typeName(ar.elem()) + "[]";
        if (t instanceof Type.Fun)
            return "fun";
        return t.toString();
    }

    private Type resolveType(String name) {
        if (name == null)
            return Type.VOID;
        if ("int".equals(name))
            return Type.INT;
        if ("real".equals(name))
            return Type.REAL;
        if ("text".equals(name))
            return Type.TEXT;
        if (name.endsWith("[]")) {
            String base = name.substring(0, name.length() - 2);
            return new Type.Array(resolveType(base));
        }
        return new Type.Struct(name, java.util.Map.of());
    }

    private void ensureGlobal(Stmt item) {
        if (item instanceof Stmt.Assign a && a.target() instanceof Expr.Variable v) {
            globalIndex.computeIfAbsent(v.name(), k -> globalIndex.size());
        }
    }

    // no-op

    private void compileFunction(Stmt.Function fn) {
        StackBytecode.Func f = bc.functions.get(funcIndex.get(fn.name()));
        // setup local params map
        Map<String, Integer> saved = currentLocals;
        int savedNext = currentNextLocal;
        currentLocals = new HashMap<>();
        for (int i = 0; i < fn.params().length; i++)
            currentLocals.put(fn.params()[i].name(), i);
        currentNextLocal = fn.params().length;
        compileBlockAs(f.code, fn.body());
        f.code.add(new StackBytecode.Insn(StackBytecode.Op.RET));
        currentLocals = saved;
        currentNextLocal = savedNext;
    }

    private void compileBlockAs(List<StackBytecode.Insn> code, Stmt.Block block) {
        for (Stmt s : block.statements())
            compileStmt(code, s);
    }

    private void compileStmt(List<StackBytecode.Insn> code, Stmt s) {
        if (s instanceof Stmt.Assign a) {
            if (a.target() instanceof Expr.Index ix) {
                // For array store, push arr, idx, then value so ASTORE (val,idx,arr) works
                compileExpr(code, ix.target());
                compileExpr(code, ix.index());
                compileExpr(code, a.value());
                code.add(new StackBytecode.Insn(StackBytecode.Op.ASTORE));
            } else {
                compileExpr(code, a.value());
                assignTarget(code, a.target());
            }
        } else if (s instanceof Stmt.ExprStmt e) {
            compileExpr(code, e.expr());
        } else if (s instanceof Stmt.If iff) {
            List<Integer> endJumps = new ArrayList<>();
            for (int i = 0; i < iff.conds().length; i++) {
                compileExpr(code, iff.conds()[i]);
                int jmpFalseIdx = code.size();
                code.add(new StackBytecode.Insn(StackBytecode.Op.JMPIF, 0)); // jump to next on false
                compileBlockAs(code, iff.blocks()[i]);
                endJumps.add(code.size());
                code.add(new StackBytecode.Insn(StackBytecode.Op.JMP, 0));
                int nextStart = code.size();
                code.get(jmpFalseIdx).a = nextStart;
            }
            if (iff.elseBlock() != null)
                compileBlockAs(code, iff.elseBlock());
            int end = code.size();
            for (int idx : endJumps)
                code.get(idx).a = end;
        } else if (s instanceof Stmt.Loop loop) {
            List<Integer> breaks = new ArrayList<>();
            breakStack.push(breaks);
            loopDepth++;
            int start = code.size();
            compileExpr(code, loop.cond());
            int jmpFalseIdx = code.size();
            code.add(new StackBytecode.Insn(StackBytecode.Op.JMPIF, 0));
            compileBlockAs(code, loop.body());
            code.add(new StackBytecode.Insn(StackBytecode.Op.JMP, start));
            int end = code.size();
            code.get(jmpFalseIdx).a = end;
            for (int bi : breaks)
                code.get(bi).a = end;
            breakStack.pop();
            loopDepth--;
        } else if (s instanceof Stmt.Return r) {
            if (r.value() != null)
                compileExpr(code, r.value());
            code.add(new StackBytecode.Insn(StackBytecode.Op.RET));
        } else if (s instanceof Stmt.Exit) {
            if (loopDepth == 0)
                throw new RuntimeException("compile error: exit outside loop");
            int idx = code.size();
            code.add(new StackBytecode.Insn(StackBytecode.Op.JMP, 0));
            if (breakStack.isEmpty())
                throw new RuntimeException("exit outside loop");
            breakStack.peek().add(idx);
        }
    }

    private void assignTarget(List<StackBytecode.Insn> code, Expr target) {
        if (target instanceof Expr.Variable v) {
            Integer li = currentLocals != null ? currentLocals.get(v.name()) : null;
            if (li == null && currentLocals != null) {
                // Declare a new local for first assignment inside a function (shadow globals)
                li = currentNextLocal++;
                currentLocals.put(v.name(), li);
            }
            if (li != null)
                code.add(new StackBytecode.Insn(StackBytecode.Op.LSTORE, li));
            else {
                int gi = globalIndex.computeIfAbsent(v.name(), k -> globalIndex.size());
                code.add(new StackBytecode.Insn(StackBytecode.Op.GSTORE, gi));
            }
            return;
        }
        if (target instanceof Expr.Index ix) {
            compileExpr(code, ix.target());
            compileExpr(code, ix.index());
            code.add(new StackBytecode.Insn(StackBytecode.Op.ASTORE));
            return;
        }
        if (target instanceof Expr.Member) {
            Expr.Member m = (Expr.Member) target;
            compileExpr(code, m.target());
            code.add(new StackBytecode.Insn(StackBytecode.Op.SETF, bc.addText(m.name())));
            return;
        }
        throw new RuntimeException("Invalid assignment target");
    }

    private void compileExpr(List<StackBytecode.Insn> code, Expr e) {
        if (e instanceof Expr.Literal l) {
            Object v = l.value();
            if (v instanceof Long)
                code.add(new StackBytecode.Insn(StackBytecode.Op.PUSHI, ((Long) v).intValue()));
            else if (v instanceof Double d)
                code.add(new StackBytecode.Insn(StackBytecode.Op.PUSHR, bc.addReal(d)));
            else if (v instanceof String s)
                code.add(new StackBytecode.Insn(StackBytecode.Op.PUSHT, bc.addText(s)));
            return;
        }
        if (e instanceof Expr.Variable v) {
            Integer li = currentLocals != null ? currentLocals.get(v.name()) : null;
            if (li != null)
                code.add(new StackBytecode.Insn(StackBytecode.Op.LLOAD, li));
            else {
                int gi = globalIndex.computeIfAbsent(v.name(), k -> globalIndex.size());
                code.add(new StackBytecode.Insn(StackBytecode.Op.GLOAD, gi));
            }
            return;
        }
        if (e instanceof Expr.Paren p) {
            compileExpr(code, p.inner());
            return;
        }
        if (e instanceof Expr.NewArray na) {
            compileExpr(code, na.length());
            int tcode = switch (na.baseType()) {
            case "int" -> 1;
            case "real" -> 2;
            case "text" -> 3;
            default -> 0;
            };
            code.add(new StackBytecode.Insn(StackBytecode.Op.NEWARR, tcode));
            return;
        }
        if (e instanceof Expr.Index ix) {
            // Special-case: TypeName[len] → create array of user-defined type (backed by
            // generic List)
            if (ix.target() instanceof Expr.Variable v && typeNames.contains(v.name())) {
                compileExpr(code, ix.index());
                int tid = bc.types.indexOf(v.name());
                code.add(new StackBytecode.Insn(StackBytecode.Op.NEWARR, -(tid + 1)));
                return;
            }
            // Push arr then idx so VM pops idx first, then arr
            compileExpr(code, ix.target());
            compileExpr(code, ix.index());
            code.add(new StackBytecode.Insn(StackBytecode.Op.ALOAD));
            return;
        }
        if (e instanceof Expr.Member m) {
            if ("len".equals(m.name())) {
                compileExpr(code, m.target());
                code.add(new StackBytecode.Insn(StackBytecode.Op.LEN));
                return;
            }
            compileExpr(code, m.target());
            code.add(new StackBytecode.Insn(StackBytecode.Op.GETF, bc.addText(m.name())));
            return;
        }
        if (e instanceof Expr.Call c) {
            // Builtin puts handled below; format/println handled in varargs block
            if (c.target() instanceof Expr.Variable pv2 && "puts".equals(pv2.name())) {
                if (c.args().length != 1)
                    throw new RuntimeException("arity mismatch for puts: expected 1, got " + c.args().length);
                compileExpr(code, c.args()[0]);
                code.add(new StackBytecode.Insn(StackBytecode.Op.PRINT));
                return;
            }
            // Handle varargs packing for trailing array parameter
            if (c.target() instanceof Expr.Variable fv) {
                java.util.List<Type> params = funcParamTypes.get(fv.name());
                boolean varargs = params != null && !params.isEmpty()
                        && (params.get(params.size() - 1) instanceof Type.Array) && !typeNames.contains(fv.name());
                if (varargs) {
                    int fixed = params.size() - 1;
                    // If caller provided exactly one array for the vararg slot, just push it
                    if (c.args().length == params.size() && (c.args()[fixed].type() instanceof Type.Array)) {
                        for (int i = 0; i < fixed; i++)
                            compileExpr(code, c.args()[i]);
                        compileExpr(code, c.args()[fixed]);
                    } else {
                        // push fixed args
                        for (int i = 0; i < Math.min(fixed, c.args().length); i++)
                            compileExpr(code, c.args()[i]);
                        // pack extras into an array
                        int extras = Math.max(0, c.args().length - fixed);
                        code.add(new StackBytecode.Insn(StackBytecode.Op.PUSHI, extras));
                        Type.Array arrT = (Type.Array) params.get(params.size() - 1);
                        int tcode = (arrT.elem() instanceof Type.Int) ? 1
                                : (arrT.elem() instanceof Type.Real ? 2 : (arrT.elem() instanceof Type.Text ? 3 : 0));
                        code.add(new StackBytecode.Insn(StackBytecode.Op.NEWARR, tcode));
                        for (int i = 0; i < extras; i++) {
                            // push array first, then index, then value for ASTORE order: arr, idx, val
                            code.add(new StackBytecode.Insn(StackBytecode.Op.DUP)); // duplicate array ref
                            code.add(new StackBytecode.Insn(StackBytecode.Op.PUSHI, i));
                            compileExpr(code, c.args()[fixed + i]);
                            code.add(new StackBytecode.Insn(StackBytecode.Op.ASTORE));
                        }
                    }
                    // After varargs packing: special post-processing for format/println
                    if ("format".equals(fv.name())) {
                        // Top of stack has the packed text[] array; convert to single text
                        code.add(new StackBytecode.Insn(StackBytecode.Op.FORMAT));
                        return;
                    }
                    if ("println".equals(fv.name())) {
                        // Top of stack has text[]; format then print
                        code.add(new StackBytecode.Insn(StackBytecode.Op.FORMAT));
                        code.add(new StackBytecode.Insn(StackBytecode.Op.PRINT));
                        return;
                    }
                } else {
                    for (Expr a : c.args())
                        compileExpr(code, a);
                }
            } else {
                for (Expr a : c.args())
                    compileExpr(code, a);
            }
            if (c.target() instanceof Expr.Variable fv && funcIndex.containsKey(fv.name())) {
                int fi = funcIndex.get(fv.name());
                // if this is a constructor, emit NEWOBJ instead of CALL
                if (typeNames.contains(fv.name())) {
                    int typeId = bc.types.indexOf(fv.name());
                    code.add(new StackBytecode.Insn(StackBytecode.Op.NEWOBJ, typeId, c.args().length));
                } else {
                    // If varargs, actual argc is fixed+1 (packed array). Passthrough if exactly one
                    // array passed.
                    java.util.List<Type> params = funcParamTypes.get(fv.name());
                    boolean varargs = params != null && !params.isEmpty()
                            && (params.get(params.size() - 1) instanceof Type.Array);
                    int argc = varargs ? (params.size()) : c.args().length;
                    code.add(new StackBytecode.Insn(StackBytecode.Op.CALL, fi, argc));
                }
            } else {
                throw new RuntimeException("Unsupported call target");
            }
            return;
        }
        if (e instanceof Expr.Unary u) {
            compileExpr(code, u.expr());
            code.add(new StackBytecode.Insn(StackBytecode.Op.NEG));
            return;
        }
        if (e instanceof Expr.Binary b) {
            // Disallow array concatenation with +
            if ("+".equals(b.op())
                    && (b.left().type() instanceof Type.Array || b.right().type() instanceof Type.Array)) {
                throw new RuntimeException("array concatenation is not supported");
            }
            compileExpr(code, b.left());
            compileExpr(code, b.right());
            switch (b.op()) {
            case "+":
                code.add(new StackBytecode.Insn(StackBytecode.Op.ADD));
                break;
            case "-":
                code.add(new StackBytecode.Insn(StackBytecode.Op.SUB));
                break;
            case "*":
                code.add(new StackBytecode.Insn(StackBytecode.Op.MUL));
                break;
            case "/":
                code.add(new StackBytecode.Insn(StackBytecode.Op.DIV));
                break;
            case "%":
                code.add(new StackBytecode.Insn(StackBytecode.Op.MOD));
                break;
            case "<<":
                code.add(new StackBytecode.Insn(StackBytecode.Op.SHL));
                break;
            case ">>":
                code.add(new StackBytecode.Insn(StackBytecode.Op.SHR));
                break;
            case "&":
                code.add(new StackBytecode.Insn(StackBytecode.Op.BAND));
                break;
            case "|":
                code.add(new StackBytecode.Insn(StackBytecode.Op.BOR));
                break;
            case "^":
                code.add(new StackBytecode.Insn(StackBytecode.Op.BXOR));
                break;
            case "and":
                code.add(new StackBytecode.Insn(StackBytecode.Op.AND));
                break;
            case "or":
                code.add(new StackBytecode.Insn(StackBytecode.Op.OR));
                break;
            case "=": {
                // Allow struct = 0 only; otherwise regular equals
                if (b.left().type() instanceof Type.Struct && b.right() instanceof Expr.Literal lit
                        && (lit.value() instanceof Long) && ((Long) lit.value()) == 0L) {
                    // Compare to zero: emit equality by converting struct truthiness to int then
                    // compare to 0
                    // Reuse existing CMPEQ as both sides are already on stack; semantic is handled
                    // in VM equals
                    code.add(new StackBytecode.Insn(StackBytecode.Op.CMPEQ));
                } else if (b.right().type() instanceof Type.Struct && b.left() instanceof Expr.Literal lit2
                        && (lit2.value() instanceof Long) && ((Long) lit2.value()) == 0L) {
                    code.add(new StackBytecode.Insn(StackBytecode.Op.CMPEQ));
                } else {
                    code.add(new StackBytecode.Insn(StackBytecode.Op.CMPEQ));
                }
                break;
            }
            case "<>": {
                if (b.left().type() instanceof Type.Struct && b.right() instanceof Expr.Literal lit
                        && (lit.value() instanceof Long) && ((Long) lit.value()) == 0L) {
                    code.add(new StackBytecode.Insn(StackBytecode.Op.CMPNE));
                } else if (b.right().type() instanceof Type.Struct && b.left() instanceof Expr.Literal lit2
                        && (lit2.value() instanceof Long) && ((Long) lit2.value()) == 0L) {
                    code.add(new StackBytecode.Insn(StackBytecode.Op.CMPNE));
                } else {
                    code.add(new StackBytecode.Insn(StackBytecode.Op.CMPNE));
                }
                break;
            }
            case "<":
                code.add(new StackBytecode.Insn(StackBytecode.Op.CMPLT));
                break;
            case "<=":
                code.add(new StackBytecode.Insn(StackBytecode.Op.CMPLE));
                break;
            case ">":
                code.add(new StackBytecode.Insn(StackBytecode.Op.CMPGT));
                break;
            case ">=":
                code.add(new StackBytecode.Insn(StackBytecode.Op.CMPGE));
                break;
            default:
                throw new RuntimeException("Unknown op: " + b.op());
            }
            return;
        }
        throw new RuntimeException("Unsupported expression");
    }
}
