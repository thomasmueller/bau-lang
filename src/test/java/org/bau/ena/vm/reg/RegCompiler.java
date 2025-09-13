package org.bau.ena.vm.reg;

import org.bau.ena.ast.Expr;
import org.bau.ena.ast.Stmt;
import org.bau.ena.types.Type;
import org.bau.ena.types.TypeChecker;

import static org.bau.ena.vm.reg.RegBytecode.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RegCompiler {
    private static final int LOCAL_BASE = 32;
    private final RegBytecode bc = new RegBytecode();
    private final Map<String, Integer> funcIndex = new HashMap<>();
    private final Map<String, Integer> globalIndex = new HashMap<>();
    private final Map<String, Type> globalTypes = new HashMap<>();
    private Map<String, Integer> currentLocals = null;
    private final Deque<List<Integer>> breakStack = new ArrayDeque<>();
    private int loopDepth = 0;
    private final java.util.Set<String> typeNames = new java.util.HashSet<>();
    private Map<Expr, Type> exprTypes = java.util.Collections.emptyMap();
    private Map<Stmt, Type> stmtTypes = java.util.Collections.emptyMap();
    private Map<String, java.util.List<Type>> funcParamTypes = new java.util.HashMap<>();

    public RegBytecode compile(Stmt.Program program) {
        // Pass 1: collect types and functions
        for (Stmt item : program.items())
            if (item instanceof Stmt.TypeDef td) {
                typeNames.add(td.name());
                String[] fields = new String[td.fields().length];
                String[] fieldTypes = new String[td.fields().length];
                for (int i = 0; i < fields.length; i++) {
                    fields[i] = td.fields()[i].name();
                    fieldTypes[i] = td.fields()[i].type();
                }
                bc.addType(td.name(), fields, fieldTypes);
                // pre-register constructor as function index
                funcIndex.put(td.name(), bc.functions.size());
                bc.functions.add(new Func(td.name(), 64));
            }
        for (Stmt item : program.items())
            if (item instanceof Stmt.Function fn) {
                funcIndex.put(fn.name(), bc.functions.size());
                // add placeholder function to stabilize indices for calls
                bc.functions.add(new Func(fn.name(), 64));
            }
        // Pass 2: annotate expressions with types
        TypeChecker.Result tr = new TypeChecker().infer(program);
        this.exprTypes = tr.exprTypes();
        this.stmtTypes = tr.stmtTypes();
        buildFuncParamTypes(program);
        // Pass 3: emit code
        for (Stmt item : program.items())
            if (item instanceof Stmt.Function fn) {
                validateFunctionReturns(fn);
                validateCalls(fn.body());
                validateFields(fn.body());
                validateTypesAndVars(fn.body());
                compileFunction(fn);
            }
        if (!funcIndex.containsKey("main")) {
            Stmt.Block top = new org.bau.ena.ast.Stmt.Block(program.items(), 1, 1);
            // Run validations so expression types (e.g., array element types) are available
            // at top-level
            validateCalls(top);
            validateFields(top);
            validateTypesAndVars(top);
            Func f = new Func("<toplevel>", 32);
            bc.functions.add(f);
            RegBuilder b = new RegBuilder(f);
            compileBlock(b, top);
            b.emit(Insn.ret(0));
        }
        if (Boolean.getBoolean("rbvm.dump")) {
            System.out.println("== RBVM Bytecode Listing ==");
            for (RegBytecode.Func f : bc.functions) {
                System.out.println("func " + f.name + " (regs=" + f.numRegs + ")");
                int pc = 0;
                for (RegBytecode.Insn ins : f.code) {
                    System.out.printf("  %04d: %s  a=%d b=%d c=%d imm=%d%n", pc++, ins.op.name(), ins.a, ins.b, ins.c,
                            ins.imm);
                }
            }
        }
        return bc;
    }

    // Helper to persist bytecode to file path
    public void writeToFile(RegBytecode bytecode, java.nio.file.Path path) {
        try (java.io.OutputStream os = java.nio.file.Files.newOutputStream(path)) {
            bytecode.writeTo(os);
            // Also dump a human-readable listing to stdout
            System.out.println("== RBVM Bytecode Listing ==");
            for (RegBytecode.Func f : bytecode.functions) {
                System.out.println("func " + f.name + " (regs=" + f.numRegs + ")");
                int pc = 0;
                for (RegBytecode.Insn ins : f.code) {
                    System.out.printf("  %04d: %s  a=%d b=%d c=%d imm=%d%n", pc++, ins.op.name(), ins.a, ins.b, ins.c,
                            ins.imm);
                }
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
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
        // println accepts any number of args; we'll lower by concat before print
        funcParamTypes.put("println", java.util.List.of(new Type.Array(Type.TEXT)));
        funcParamTypes.put("puts", java.util.List.of(Type.TEXT));
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
            // Permit calling void functions only as statements: validate arity/args but
            // skip void-as-expression error
            Expr ex = e.expr();
            while (ex instanceof Expr.Paren p)
                ex = p.inner();
            if (ex instanceof Expr.Call c && c.target() instanceof Expr.Variable fv) {
                if ("format".equals(fv.name())) {
                    for (Expr a : c.args())
                        validateCallsInExpr(a);
                    return;
                }
                if ("println".equals(fv.name())) {
                    for (Expr a : c.args())
                        validateCallsInExpr(a);
                    return;
                }
                java.util.List<Type> params = funcParamTypes.get(fv.name());
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
                for (Expr a : c.args())
                    validateCallsInExpr(a);
                return;
            }
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
                // Disallow use of void-returning function in expression position
                Type callType = exprTypes.getOrDefault(c, Type.UNKNOWN);
                if (callType instanceof Type.Void)
                    throw new RuntimeException("type error: void function used as expression");
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
    }

    private void validateTypesAndVars(Stmt.Block body) {
        for (Stmt s : body.statements())
            validateTypesAndVarsInStmt(s);
    }

    private void validateTypesAndVarsInStmt(Stmt s) {
        if (s instanceof Stmt.Assign a) {
            // Record global variable types early so later expressions have correct types
            if (currentLocals == null && a.target() instanceof Expr.Variable gv) {
                if (a.value() instanceof Expr.NewArray na) {
                    Type elem;
                    switch (na.baseType()) {
                    case "int" -> elem = Type.INT;
                    case "real" -> elem = Type.REAL;
                    case "text" -> elem = Type.TEXT;
                    default -> elem = new Type.Struct(na.baseType(), java.util.Map.of());
                    }
                    globalTypes.put(gv.name(), new Type.Array(elem));
                } else {
                    // Use inferred type of RHS for scalars/text/structs
                    Type rhsType = a.value().type();
                    if (!(rhsType instanceof Type.Unknown)) {
                        globalTypes.put(gv.name(), rhsType);
                    }
                }
            }
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
                        break;
                    }
                }
                if (!(lNum && rNum) && !(lt instanceof Type.Text && rt instanceof Type.Text))
                    throw new RuntimeException("type error: comparison requires numbers or text");
                break;
            case "and":
            case "or":
                // Truthiness accepted for any type
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
                // be permissive; special constructor array
            }
            if (!isTypeArrayCtor) {
                boolean okIndex = ix.index().type() instanceof Type.Int;
                if (!okIndex && ix.index() instanceof Expr.Literal lit && lit.value() instanceof Long)
                    okIndex = true;
                if (!okIndex)
                    throw new RuntimeException("type error: array index must be int");
            }
            validateTypesAndVarsInExpr(ix.target());
            validateTypesAndVarsInExpr(ix.index());
            return;
        }
        if (e instanceof Expr.Member m) {
            if ("len".equals(m.name())) {
                Type tt = m.target().type();
                if (m.target() instanceof Expr.Variable gv) {
                    Type gt = globalTypes.get(gv.name());
                    if (gt != null)
                        tt = gt;
                }
                if (tt instanceof Type.Unknown) {
                    validateTypesAndVarsInExpr(m.target());
                    return;
                }
                if (!(tt instanceof Type.Array || tt instanceof Type.Text)) {
                    // At top-level, allow len on globals even if type not yet fully propagated
                    if (m.target() instanceof Expr.Variable && currentLocals == null) {
                        validateTypesAndVarsInExpr(m.target());
                        return;
                    }
                    throw new RuntimeException("type error: len on non-array/text");
                }
                validateTypesAndVarsInExpr(m.target());
                return;
            }
            validateTypesAndVarsInExpr(m.target());
            return;
        }
        if (e instanceof Expr.Call c) {
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

    private void compileFunction(Stmt.Function fn) {
        // retrieve pre-registered placeholder function and emit code into it
        Func f = bc.functions.get(funcIndex.get(fn.name()));
        f.code.clear();
        RegBuilder b = new RegBuilder(f);
        Map<String, Integer> saved = currentLocals;
        currentLocals = new HashMap<>();
        boolean needsParamRelocation = functionBodyHasCalls(fn.body());
        if (needsParamRelocation) {
            // Move params from R1..Rp into high-numbered local registers so R1.. can be
            // used for call arg passing
            for (int i = 0; i < fn.params().length; i++) {
                int src = i + 1;
                int dst = LOCAL_BASE + i;
                currentLocals.put(fn.params()[i].name(), dst);
                b.reserve(dst);
                b.emit(Insn.mov(dst, src));
            }
        } else {
            // Keep parameters in-place; reserve R1..Rp as locals to avoid accidental reuse
            for (int i = 0; i < fn.params().length; i++) {
                int r = i + 1;
                currentLocals.put(fn.params()[i].name(), r);
                b.reserve(r);
            }
        }
        // No prolog; conditions must guard accesses as compiled
        compileBlock(b, fn.body());
        b.emit(Insn.ret(0));
        currentLocals = saved;
        // Post-pass: trivial MOV elimination within this function
        peepholeRemoveRedundantMovs(f);
        // Remove NOPs and retarget jumps using old->new mapping
        removeNopsAndRetarget(f);
    }

    private boolean functionBodyHasCalls(Stmt.Block b) {
        return blockHasCalls(b);
    }

    private boolean blockHasCalls(Stmt.Block b) {
        for (Stmt s : b.statements())
            if (stmtHasCalls(s))
                return true;
        return false;
    }

    private boolean stmtHasCalls(Stmt s) {
        if (s instanceof Stmt.Assign a) {
            return exprHasCalls(a.target()) || exprHasCalls(a.value());
        }
        if (s instanceof Stmt.ExprStmt e) {
            return exprHasCalls(e.expr());
        }
        if (s instanceof Stmt.If iff) {
            for (Expr c : iff.conds())
                if (exprHasCalls(c))
                    return true;
            for (Stmt.Block bl : iff.blocks())
                if (blockHasCalls(bl))
                    return true;
            return iff.elseBlock() != null && blockHasCalls(iff.elseBlock());
        }
        if (s instanceof Stmt.Loop lp) {
            return exprHasCalls(lp.cond()) || blockHasCalls(lp.body());
        }
        if (s instanceof Stmt.Return r) {
            return r.value() != null && exprHasCalls(r.value());
        }
        return false;
    }

    private boolean exprHasCalls(Expr e) {
        if (e instanceof Expr.Call c) {
            // println/format/puts are inlined; still use arg registers but not CALL.
            if (c.target() instanceof Expr.Variable fv) {
                String n = fv.name();
                if ("println".equals(n) || "format".equals(n) || "puts".equals(n)) {
                    // check args recursively
                    for (Expr a : c.args())
                        if (exprHasCalls(a))
                            return true;
                    return false;
                }
            }
            return true;
        }
        if (e instanceof Expr.Binary b) {
            return exprHasCalls(b.left()) || exprHasCalls(b.right());
        }
        if (e instanceof Expr.Unary u) {
            return exprHasCalls(u.expr());
        }
        if (e instanceof Expr.Index ix) {
            return exprHasCalls(ix.target()) || exprHasCalls(ix.index());
        }
        if (e instanceof Expr.Member m) {
            return exprHasCalls(m.target());
        }
        if (e instanceof Expr.Paren p) {
            return exprHasCalls(p.inner());
        }
        return false;
    }

    private void peepholeRemoveRedundantMovs(Func f) {
        List<Insn> code = f.code;
        for (int i = 0; i < code.size(); i++) {
            Insn ins = code.get(i);
            if (ins.op != Op.MOV)
                continue;
            // Remove self-move
            if (ins.a == ins.b) {
                code.set(i, Insn.nop());
                continue;
            }
            // Fold MOV into RET immediately following
            if (i + 1 < code.size()) {
                Insn nxt = code.get(i + 1);
                if (nxt.op == Op.RET && nxt.a == ins.a) {
                    code.set(i + 1, Insn.ret(ins.b));
                    // neutralize MOV but keep slot to preserve jump targets
                    code.set(i, Insn.nop());
                    continue;
                }
                // Fold MOV into JMPIF condition
                if (nxt.op == Op.JMPIF && nxt.a == ins.a) {
                    code.set(i + 1, Insn.jmpif(ins.b, nxt.b));
                    code.set(i, Insn.nop());
                    continue;
                }
                // If next instruction overwrites the same destination register without reading
                // it, drop MOV
                if (overwritesWithoutRead(nxt, ins.a)) {
                    code.set(i, Insn.nop());
                    continue;
                }
            }
        }
    }

    private boolean overwritesWithoutRead(Insn ins, int r) {
        // if instruction writes to a == r and does not read it as b/c
        boolean writesA = switch (ins.op) {
        case STORE, ASTOREI, ASTORER, ASTORET, ASTORES, SETF, JMP, JMPIF, PRINT, RET -> false;
        default -> true;
        };
        if (!writesA || ins.a != r)
            return false;
        return ins.b != r && ins.c != r;
    }

    private void removeNopsAndRetarget(Func f) {
        List<Insn> code = f.code;
        int n = code.size();
        boolean[] keep = new boolean[n];
        for (int i = 0; i < n; i++)
            keep[i] = code.get(i).op != Op.NOP;
        // Map old kept index -> new index
        int[] oldToNewKept = new int[n];
        List<Insn> newCode = new java.util.ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            if (keep[i]) {
                oldToNewKept[i] = newCode.size();
                newCode.add(code.get(i));
            } else
                oldToNewKept[i] = -1;
        }
        // Build old->new mapping for all old indices: map to next kept at or after i,
        // else end
        int[] oldToNew = new int[n + 1]; // include mapping for end as well
        int nextKeptNewIndex = newCode.size();
        for (int i = n - 1; i >= 0; i--) {
            if (keep[i])
                nextKeptNewIndex = oldToNewKept[i];
            oldToNew[i] = nextKeptNewIndex;
        }
        oldToNew[n] = newCode.size();
        // Retarget jumps in newCode
        for (int i = 0; i < newCode.size(); i++) {
            Insn ins = newCode.get(i);
            switch (ins.op) {
            case JMP -> newCode.set(i, Insn.jmp(safeMap(oldToNew, ins.a)));
            case JMPIF -> newCode.set(i, Insn.jmpif(ins.a, safeMap(oldToNew, ins.b)));
            default -> {
            }
            }
        }
        // Replace function code
        f.code.clear();
        f.code.addAll(newCode);
    }

    private int safeMap(int[] map, int idx) {
        if (idx < 0)
            return 0;
        if (idx >= map.length)
            return map[map.length - 1];
        return map[idx];
    }

    private void validateFunctionReturns(Stmt.Function fn) {
        Type expected = (fn.returnType() == null) ? Type.VOID : resolveType(fn.returnType());
        validateReturnsInBlock(fn.body(), expected, fn.name());
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

    private void compileBlock(RegBuilder b, Stmt.Block block) {
        for (Stmt s : block.statements())
            compileStmt(b, s);
    }

    private void compileStmt(RegBuilder b, Stmt s) {
        if (s instanceof Stmt.Assign a) {
            int rv = compileExpr(b, a.value());
            // Track global variable types at top-level for later array operations
            if (currentLocals == null && a.target() instanceof Expr.Variable gv) {
                if (a.value() instanceof Expr.NewArray na) {
                    Type elem;
                    switch (na.baseType()) {
                    case "int" -> elem = Type.INT;
                    case "real" -> elem = Type.REAL;
                    case "text" -> elem = Type.TEXT;
                    default -> elem = new Type.Struct(na.baseType(), java.util.Map.of());
                    }
                    globalTypes.put(gv.name(), new Type.Array(elem));
                }
            }
            assignTarget(b, a.target(), rv);
        } else if (s instanceof Stmt.ExprStmt e) {
            int v = compileExpr(b, e.expr());
            b.free(v);
        } else if (s instanceof Stmt.Return r) {
            if (r.value() == null) {
                b.emit(Insn.ret(0));
            } else {
                int v = compileExpr(b, r.value());
                b.emit(Insn.ret(v));
            }
        } else if (s instanceof Stmt.If iff) {
            List<Integer> endJumps = new java.util.ArrayList<>();
            for (int i = 0; i < iff.conds().length; i++) {
                int cr = compileExpr(b, iff.conds()[i]);
                if (!(iff.conds()[i].type() instanceof Type.Int))
                    throw new RuntimeException("type error: if condition must be int");
                int jmpToBlock = b.f.code.size();
                b.emit(Insn.jmpif(cr, 0)); // target fix later
                int jmpOverBlock = b.f.code.size();
                b.emit(Insn.jmp(0));
                int blockStart = b.f.code.size();
                b.f.code.set(jmpToBlock, Insn.jmpif(cr, blockStart));
                compileBlock(b, iff.blocks()[i]);
                int endJumpIdx = b.f.code.size();
                b.emit(Insn.jmp(0));
                endJumps.add(endJumpIdx);
                int nextStart = b.f.code.size();
                b.f.code.set(jmpOverBlock, Insn.jmp(nextStart));
                b.free(cr);
            }
            if (iff.elseBlock() != null)
                compileBlock(b, iff.elseBlock());
            int end = b.f.code.size();
            for (int idx : endJumps)
                b.f.code.set(idx, Insn.jmp(end));
        } else if (s instanceof Stmt.Loop loop) {
            List<Integer> breaks = new java.util.ArrayList<>();
            breakStack.push(breaks);
            loopDepth++;
            int start = b.f.code.size();
            int cr = compileExpr(b, loop.cond());
            if (!(loop.cond().type() instanceof Type.Int))
                throw new RuntimeException("type error: loop condition must be int");
            int jmpToBody = b.f.code.size();
            b.emit(Insn.jmpif(cr, 0));
            int jmpEnd = b.f.code.size();
            b.emit(Insn.jmp(0));
            int bodyStart = b.f.code.size();
            b.f.code.set(jmpToBody, Insn.jmpif(cr, bodyStart));
            compileBlock(b, loop.body());
            b.emit(Insn.jmp(start));
            int end = b.f.code.size();
            b.f.code.set(jmpEnd, Insn.jmp(end));
            for (int bi : breaks)
                b.f.code.set(bi, Insn.jmp(end));
            breakStack.pop();
            loopDepth--;
            b.free(cr);
        } else if (s instanceof Stmt.Exit) {
            if (loopDepth == 0)
                throw new RuntimeException("compile error: exit outside loop");
            int idx = b.f.code.size();
            b.emit(Insn.jmp(0));
            breakStack.peek().add(idx);
        }
    }

    private void assignTarget(RegBuilder b, Expr target, int rv) {
        if (target instanceof Expr.Variable v) {
            Integer li = currentLocals != null ? currentLocals.get(v.name()) : null;
            if (li == null && currentLocals != null) {
                // First assignment to an unknown name inside a function declares a local
                li = LOCAL_BASE + currentLocals.size();
                currentLocals.put(v.name(), li);
                b.reserve(li);
            }
            if (li != null)
                b.emit(Insn.mov(li, rv));
            else {
                int gi = globalIndex.computeIfAbsent(v.name(), k -> globalIndex.size());
                b.emit(Insn.store(gi, rv));
            }
            return;
        }
        if (target instanceof Expr.Index ix) {
            int a = compileExpr(b, ix.target());
            int i = compileExpr(b, ix.index());
            Type t = ix.target().type();
            if (t instanceof Type.Unknown && ix.target() instanceof Expr.Variable gv) {
                Type gt = globalTypes.get(gv.name());
                if (gt != null)
                    t = gt;
            }
            if (!(t instanceof Type.Array arr)) {
                b.free(a);
                b.free(i);
                throw new RuntimeException("compile error: index assignment on non-array or unknown element type");
            }
            if (arr.elem() instanceof Type.Int)
                b.emit(Insn.astorei(a, i, rv));
            else if (arr.elem() instanceof Type.Real)
                b.emit(Insn.astorer(a, i, rv));
            else if (arr.elem() instanceof Type.Text)
                b.emit(Insn.astoret(a, i, rv));
            else
                b.emit(Insn.asstores(a, i, rv));
            b.free(a);
            b.free(i);
            return;
        }
        if (target instanceof Expr.Member m) {
            // resolve field index using struct type; support array element member via index
            // fallback
            Type tt = m.target().type();
            if (!(tt instanceof Type.Struct) && m.target() instanceof Expr.Index ix) {
                Type at = ix.target().type();
                if (at instanceof Type.Array arr && arr.elem() instanceof Type.Struct)
                    tt = arr.elem();
            }
            if (!(tt instanceof Type.Struct st))
                throw new RuntimeException("field access on non-struct type");
            int tid = bc.types.indexOf(st.name());
            if (tid < 0)
                throw new RuntimeException("unknown struct type: " + st.name());
            String[] flds = bc.typeFields.get(tid);
            int idx = -1;
            for (int i = 0; i < flds.length; i++)
                if (flds[i].equals(m.name())) {
                    idx = i;
                    break;
                }
            if (idx < 0)
                throw new RuntimeException("unknown field: " + m.name());
            int robj = compileExpr(b, m.target());
            b.emit(Insn.setf(robj, idx, rv));
            b.free(robj);
            return;
        }
        throw new RuntimeException("Unsupported lvalue");
    }

    private int compileExpr(RegBuilder b, Expr e) {
        if (e instanceof Expr.Literal lit) {
            Object v = lit.value();
            int rd = b.alloc();
            if (v instanceof Long longVal)
                b.emit(Insn.loadi(rd, longVal.intValue()));
            else if (v instanceof Double d) {
                int k = bc.addReal(d);
                b.emit(Insn.loadr(rd, k));
            } else if (v instanceof String s)
                b.emit(Insn.loads(rd, bc.addText(s)));
            return rd;
        }
        if (e instanceof Expr.Variable v) {
            Integer li = currentLocals != null ? currentLocals.get(v.name()) : null;
            if (li != null) {
                // Use local register directly; caller free() will be ignored for reserved
                // locals
                return li;
            } else {
                int rd = b.alloc();
                int gi = globalIndex.computeIfAbsent(v.name(), k -> globalIndex.size());
                b.emit(Insn.load(rd, gi));
                // If this variable is a known global type, attach it for downstream type checks
                Type gt = globalTypes.get(v.name());
                if (gt != null) {
                    v.setType(gt);
                }
                return rd;
            }
        }
        if (e instanceof Expr.Paren p)
            return compileExpr(b, p.inner());
        if (e instanceof Expr.NewArray na) {
            int rd = b.alloc();
            int len = compileExpr(b, na.length());
            int elemTypeCode = switch (na.baseType()) {
            case "int" -> 1;
            case "real" -> 2;
            case "text" -> 3;
            default -> -(bc.types.indexOf(na.baseType()) + 1);
            };
            b.emit(Insn.newarr(rd, elemTypeCode, len));
            b.free(len);
            return rd;
        }
        if (e instanceof Expr.Index ix) {
            // Special-case: TypeName[len] → allocate array for user-defined type; encode
            // struct id in b as negative
            if (ix.target() instanceof Expr.Variable v && typeNames.contains(v.name())) {
                int rd = b.alloc();
                int len = compileExpr(b, ix.index());
                int tid = bc.types.indexOf(v.name());
                b.emit(Insn.newarr(rd, -(tid + 1), len));
                b.free(len);
                return rd;
            }
            int rd = b.alloc();
            int a = compileExpr(b, ix.target());
            int i = compileExpr(b, ix.index());
            Type t = resolvedExprType(ix.target());
            if (t instanceof Type.Unknown && ix.target() instanceof Expr.Variable gv) {
                Type gt = globalTypes.get(gv.name());
                if (gt != null)
                    t = gt;
            }
            if (!(t instanceof Type.Array arr)) {
                b.free(a);
                b.free(i);
                b.free(rd);
                throw new RuntimeException("compile error: indexing non-array or unknown element type");
            }
            if (arr.elem() instanceof Type.Int)
                b.emit(Insn.aloadi(rd, a, i));
            else if (arr.elem() instanceof Type.Real)
                b.emit(Insn.aloadr(rd, a, i));
            else if (arr.elem() instanceof Type.Text)
                b.emit(Insn.aloadt(rd, a, i));
            else
                b.emit(Insn.aloads(rd, a, i));
            b.free(a);
            b.free(i);
            return rd;
        }
        if (e instanceof Expr.Member m) {
            if ("len".equals(m.name())) {
                int rd = b.alloc();
                int a = compileExpr(b, m.target());
                Type tt = resolvedExprType(m.target());
                if (tt instanceof Type.Unknown) {
                    // try resolve from globalTypes if it's a global variable
                    if (m.target() instanceof Expr.Variable gv && globalTypes.get(gv.name()) instanceof Type.Array arr2)
                        tt = arr2;
                }
                if (tt instanceof Type.Array arr) {
                    if (arr.elem() instanceof Type.Int)
                        b.emit(Insn.leni(rd, a));
                    else if (arr.elem() instanceof Type.Real)
                        b.emit(Insn.lenf(rd, a));
                    else if (arr.elem() instanceof Type.Text)
                        b.emit(Insn.lent(rd, a));
                    else
                        b.emit(Insn.lens(rd, a));
                } else if (tt instanceof Type.Text) {
                    b.emit(Insn.lent(rd, a));
                } else {
                    // default to LENS for unsupported kinds
                    b.emit(Insn.lens(rd, a));
                }
                b.free(a);
                return rd;
            }
            // Resolve field index from the struct type
            Type tt = resolvedExprType(m.target());
            if (!(tt instanceof Type.Struct) && m.target() instanceof Expr.Index ix) {
                Type at = ix.target().type();
                if (at instanceof Type.Array arr && arr.elem() instanceof Type.Struct)
                    tt = arr.elem();
            }
            int fieldIndex = 0;
            if (tt instanceof Type.Struct st) {
                int tid = bc.types.indexOf(st.name());
                if (tid < 0)
                    throw new RuntimeException("unknown struct type: " + st.name());
                String[] flds = bc.typeFields.get(tid);
                int idx = -1;
                for (int i = 0; i < flds.length; i++)
                    if (flds[i].equals(m.name())) {
                        idx = i;
                        break;
                    }
                if (idx < 0)
                    throw new RuntimeException("unknown field: " + m.name() + " on " + st.name());
                fieldIndex = idx;
            } else {
                throw new RuntimeException("field access on non-struct type");
            }
            int rd = b.alloc();
            int robj = compileExpr(b, m.target());
            b.emit(Insn.getf(rd, robj, fieldIndex));
            b.free(robj);
            return rd;
        }
        if (e instanceof Expr.Call c) {
            List<Integer> args = new ArrayList<>();
            for (Expr a : c.args())
                args.add(compileExpr(b, a));
            // Handle builtin varargs regardless of funcIndex presence
            if (c.target() instanceof Expr.Variable bvBuilt) {
                if ("format".equals(bvBuilt.name())) {
                    int out = b.alloc();
                    if (args.isEmpty()) {
                        b.emit(Insn.loads(out, bc.addText("")));
                        return out;
                    }
                    int acc = args.get(0);
                    if (!(c.args()[0].type() instanceof Type.Text)) {
                        int t0 = b.alloc();
                        if (c.args()[0].type() instanceof Type.Real)
                            b.emit(Insn.r2s(t0, acc));
                        else
                            b.emit(Insn.i2s(t0, acc));
                        acc = t0;
                    }
                    for (int i = 1; i < args.size(); i++) {
                        int cur = args.get(i);
                        if (!(c.args()[i].type() instanceof Type.Text)) {
                            int ti = b.alloc();
                            if (c.args()[i].type() instanceof Type.Real)
                                b.emit(Insn.r2s(ti, cur));
                            else
                                b.emit(Insn.i2s(ti, cur));
                            cur = ti;
                        }
                        int tmp = b.alloc();
                        b.emit(Insn.concat(tmp, acc, cur));
                        b.free(acc);
                        acc = tmp;
                    }
                    b.emit(Insn.mov(out, acc));
                    b.free(acc);
                    for (int v : args)
                        if (v != out)
                            b.free(v);
                    return out;
                }
                if ("println".equals(bvBuilt.name())) {
                    if (args.isEmpty()) {
                        int s = b.alloc();
                        b.emit(Insn.loads(s, bc.addText("")));
                        b.emit(Insn.print(s));
                        b.free(s);
                        return 0;
                    }
                    int acc = args.get(0);
                    if (!(c.args()[0].type() instanceof Type.Text)) {
                        int t0 = b.alloc();
                        if (c.args()[0].type() instanceof Type.Real)
                            b.emit(Insn.r2s(t0, acc));
                        else
                            b.emit(Insn.i2s(t0, acc));
                        acc = t0;
                    }
                    for (int i = 1; i < args.size(); i++) {
                        int cur = args.get(i);
                        if (!(c.args()[i].type() instanceof Type.Text)) {
                            int ti = b.alloc();
                            if (c.args()[i].type() instanceof Type.Real)
                                b.emit(Insn.r2s(ti, cur));
                            else
                                b.emit(Insn.i2s(ti, cur));
                            cur = ti;
                        }
                        int tmp = b.alloc();
                        b.emit(Insn.concat(tmp, acc, cur));
                        b.free(acc);
                        acc = tmp;
                    }
                    b.emit(Insn.print(acc));
                    b.free(acc);
                    return 0;
                }
            }
            if (c.target() instanceof Expr.Variable fv && funcIndex.containsKey(fv.name())) {
                // println handled as normal function call via funcIndex stub; no special
                // lowering here
                if ("puts".equals(fv.name())) {
                    if (args.size() != 1)
                        throw new RuntimeException("arity mismatch for puts: expected 1, got " + args.size());
                    int v = args.get(0);
                    if (!(c.args()[0].type() instanceof Type.Text)) {
                        int t = b.alloc();
                        if (c.args()[0].type() instanceof Type.Real)
                            b.emit(Insn.r2s(t, v));
                        else
                            b.emit(Insn.i2s(t, v));
                        v = t;
                    }
                    b.emit(Insn.print(v));
                    b.free(v);
                    return 0;
                }
                // Handle varargs: last param is array -> pack extra args into array
                java.util.List<Type> params = funcParamTypes.get(fv.name());
                boolean varargs = params != null && !params.isEmpty()
                        && (params.get(params.size() - 1) instanceof Type.Array);
                int rd;
                if (varargs && !typeNames.contains(fv.name())) {
                    // Special-cases: implement format/println via direct concatenation of provided
                    // args
                    if ("format".equals(fv.name())) {
                        int out = b.alloc();
                        if (args.isEmpty()) {
                            b.emit(Insn.loads(out, bc.addText("")));
                            return out;
                        }
                        int acc = args.get(0);
                        for (int i = 1; i < args.size(); i++) {
                            int tmp = b.alloc();
                            b.emit(Insn.concat(tmp, acc, args.get(i)));
                            b.free(acc);
                            acc = tmp;
                        }
                        b.emit(Insn.mov(out, acc));
                        b.free(acc);
                        for (int v : args)
                            if (v != out)
                                b.free(v);
                        return out;
                    }
                    if ("println".equals(fv.name())) {
                        if (args.isEmpty()) {
                            int s = b.alloc();
                            b.emit(Insn.loads(s, bc.addText("")));
                            b.emit(Insn.print(s));
                            b.free(s);
                            return 0;
                        }
                        int acc = args.get(0);
                        for (int i = 1; i < args.size(); i++) {
                            int tmp = b.alloc();
                            b.emit(Insn.concat(tmp, acc, args.get(i)));
                            b.free(acc);
                            acc = tmp;
                        }
                        b.emit(Insn.print(acc));
                        b.free(acc);
                        return 0;
                    }
                    // Build array from provided args starting at index (params.size()-1)
                    int fixed = params.size() - 1;
                    // Passthrough if caller provided exactly one array for the vararg slot
                    if (args.size() == params.size()) {
                        if (c.args()[fixed].type() instanceof Type.Array) {
                            for (int i = 0; i < args.size(); i++)
                                b.emit(Insn.mov(i + 1, args.get(i)));
                            rd = b.alloc();
                            b.emit(Insn.call(rd, funcIndex.get(fv.name()), args.size()));
                            for (int v : args)
                                b.free(v);
                            return rd;
                        }
                    }
                    Type.Array arrT = (Type.Array) params.get(params.size() - 1);
                    int arrReg = b.alloc();
                    int lenReg = b.alloc();
                    b.emit(Insn.loadi(lenReg, Math.max(0, args.size() - fixed)));
                    int elemTypeCode;
                    Type elemT = arrT.elem();
                    if (elemT instanceof Type.Int)
                        elemTypeCode = 1;
                    else if (elemT instanceof Type.Real)
                        elemTypeCode = 2;
                    else if (elemT instanceof Type.Text)
                        elemTypeCode = 3;
                    else if (elemT instanceof Type.Struct st)
                        elemTypeCode = -(bc.types.indexOf(st.name()) + 1);
                    else
                        elemTypeCode = 0;
                    b.emit(Insn.newarr(arrReg, elemTypeCode, lenReg));
                    b.free(lenReg);
                    // store extra args into array indices 0..k-1
                    for (int i = fixed; i < args.size(); i++) {
                        int idx = b.alloc();
                        b.emit(Insn.loadi(idx, i - fixed));
                        Type elemT2 = arrT.elem();
                        if (elemT2 instanceof Type.Int)
                            b.emit(Insn.astorei(arrReg, idx, args.get(i)));
                        else if (elemT2 instanceof Type.Real)
                            b.emit(Insn.astorer(arrReg, idx, args.get(i)));
                        else if (elemT2 instanceof Type.Text)
                            b.emit(Insn.astoret(arrReg, idx, args.get(i)));
                        else
                            b.emit(Insn.asstores(arrReg, idx, args.get(i)));
                        b.free(idx);
                    }
                    // Move fixed args
                    for (int i = 0; i < fixed; i++)
                        b.emit(Insn.mov(i + 1, args.get(i)));
                    // Move packed array as last arg
                    b.emit(Insn.mov(fixed + 1, arrReg));
                    // After packing, call normally for other functions
                    rd = b.alloc();
                    b.emit(Insn.call(rd, funcIndex.get(fv.name()), fixed + 1));
                    b.free(arrReg);
                    for (int v : args)
                        b.free(v);
                    return rd;
                } else {
                    // Regular call or constructor
                    for (int i = 0; i < args.size(); i++)
                        b.emit(Insn.mov(i + 1, args.get(i)));
                    rd = b.alloc();
                    if (typeNames.contains(fv.name())) {
                        int tid = bc.types.indexOf(fv.name());
                        b.emit(Insn.newobj(rd, tid, args.size()));
                    } else {
                        b.emit(Insn.call(rd, funcIndex.get(fv.name()), args.size()));
                    }
                    for (int v : args)
                        b.free(v);
                    return rd;
                }
            }
            throw new RuntimeException("Unsupported call target");
        }
        if (e instanceof Expr.Unary u) {
            int x = compileExpr(b, u.expr());
            int rd = b.alloc();
            if (u.expr().type() instanceof Type.Real)
                b.emit(Insn.op3(Op.FNEG, rd, x, 0));
            else
                b.emit(Insn.op3(Op.NEG, rd, x, 0));
            b.free(x);
            return rd;
        }
        if (e instanceof Expr.Binary br) {
            int a = compileExpr(b, br.left());
            int c = compileExpr(b, br.right());
            int rd = b.alloc();
            boolean forceReal = (br.left().type() instanceof Type.Real) || (br.right().type() instanceof Type.Real);
            if ("+".equals(br.op())) {
                Type lt = br.left().type();
                Type rt = br.right().type();
                boolean isText = (lt instanceof Type.Text) || (rt instanceof Type.Text);
                boolean isArray = (lt instanceof Type.Array) || (rt instanceof Type.Array);
                boolean isInt = (lt instanceof Type.Int) && (rt instanceof Type.Int);
                boolean isReal = (lt instanceof Type.Real) && (rt instanceof Type.Real);
                boolean mixedIntReal = ((lt instanceof Type.Real) && (rt instanceof Type.Int))
                        || ((lt instanceof Type.Int) && (rt instanceof Type.Real));
                if (isText) {
                    throw new RuntimeException("type error: '+' on text is not supported; use format(...)");
                } else if (isArray) {
                    throw new RuntimeException("array concatenation is not supported");
                } else if (isInt) {
                    b.emit(Insn.op3(Op.ADD, rd, a, c));
                } else if (isReal) {
                    b.emit(Insn.op3(Op.FADD, rd, a, c));
                } else if (mixedIntReal) {
                    // convert int operand to real then ALUR ADD
                    if (lt instanceof Type.Int && rt instanceof Type.Real) {
                        int ar = b.alloc();
                        b.emit(Insn.i2r(ar, a));
                        b.emit(Insn.op3(Op.FADD, rd, ar, c));
                        b.free(ar);
                    } else { // lt real, rt int
                        int cr = b.alloc();
                        b.emit(Insn.i2r(cr, c));
                        b.emit(Insn.op3(Op.FADD, rd, a, cr));
                        b.free(cr);
                    }
                }
            } else {
                String op = br.op();
                boolean isCmp = op.equals("=") || op.equals("<>") || op.equals("<") || op.equals("<=") || op.equals(">")
                        || op.equals(">=");
                if (isCmp) {
                    Type lt = br.left().type();
                    Type rt = br.right().type();
                    boolean bothInt = (lt instanceof Type.Int) && (rt instanceof Type.Int);
                    boolean bothText = (lt instanceof Type.Text) && (rt instanceof Type.Text);
                    // Allow struct = 0 and struct <> 0 only; forbid other numeric comparisons with
                    // struct
                    boolean leftStructRightZero = (lt instanceof Type.Struct) && (br.right() instanceof Expr.Literal lit
                            && lit.value() instanceof Long && ((Long) lit.value()) == 0L);
                    boolean rightStructLeftZero = (rt instanceof Type.Struct) && (br.left() instanceof Expr.Literal lit2
                            && lit2.value() instanceof Long && ((Long) lit2.value()) == 0L);
                    if ((lt instanceof Type.Struct || rt instanceof Type.Struct)
                            && !(leftStructRightZero || rightStructLeftZero) && (op.equals("=") || op.equals("<>")
                                    || op.equals("<") || op.equals("<=") || op.equals(">") || op.equals(">="))) {
                        throw new RuntimeException(
                                "type error: struct comparisons only allowed with 0 using '=' or '<>'");
                    }
                    // Also allow member-vs-zero (struct field)
                    boolean leftMemberRightZero = (br.left() instanceof Expr.Member)
                            && (br.right() instanceof Expr.Literal lr2
                                    && ((lr2.value() instanceof Long && ((Long) lr2.value()) == 0L)
                                            || (lr2.value() instanceof Double && ((Double) lr2.value()) == 0.0)));
                    boolean rightMemberLeftZero = (br.right() instanceof Expr.Member)
                            && (br.left() instanceof Expr.Literal ll2
                                    && ((ll2.value() instanceof Long && ((Long) ll2.value()) == 0L)
                                            || (ll2.value() instanceof Double && ((Double) ll2.value()) == 0.0)));
                    boolean isStructZeroCmp = leftStructRightZero || rightStructLeftZero || leftMemberRightZero
                            || rightMemberLeftZero;
                    // Lower struct null checks into ISNULL and integer compare
                    if (isStructZeroCmp && (op.equals("=") || op.equals("<>"))) {
                        int isr;
                        if (leftStructRightZero || leftMemberRightZero) {
                            isr = b.alloc();
                            b.emit(Insn.isnull(isr, a));
                        } else {
                            isr = b.alloc();
                            b.emit(Insn.isnull(isr, c));
                        }
                        if (op.equals("=")) {
                            int one = b.alloc();
                            b.emit(Insn.loadi(one, 1));
                            b.emit(Insn.op3(Op.EQ, rd, isr, one));
                            b.free(one);
                        } else {
                            int zero = b.alloc();
                            b.emit(Insn.loadi(zero, 0));
                            b.emit(Insn.op3(Op.EQ, rd, isr, zero));
                            b.free(zero);
                        }
                        b.free(isr);
                    } else if (forceReal) {
                        int tmp = b.alloc();
                        b.emit(Insn.op3(Op.FCMP, tmp, a, c));
                        int z = b.alloc();
                        b.emit(Insn.loadi(z, 0));
                        Op cmpOp = switch (op) {
                        case "=" -> Op.EQ;
                        case "<>" -> Op.NE;
                        case "<" -> Op.LT;
                        case "<=" -> Op.LE;
                        case ">" -> Op.GT;
                        case ">=" -> Op.GE;
                        default -> Op.EQ;
                        };
                        b.emit(Insn.op3(cmpOp, rd, tmp, z));
                        b.free(tmp);
                        b.free(z);
                    } else if (bothText) {
                        int tmp = b.alloc();
                        b.emit(Insn.op3(Op.TCMP, tmp, a, c));
                        int z = b.alloc();
                        b.emit(Insn.loadi(z, 0));
                        Op cmpOp = switch (op) {
                        case "=" -> Op.EQ;
                        case "<>" -> Op.NE;
                        case "<" -> Op.LT;
                        case "<=" -> Op.LE;
                        case ">" -> Op.GT;
                        case ">=" -> Op.GE;
                        default -> Op.EQ;
                        };
                        b.emit(Insn.op3(cmpOp, rd, tmp, z));
                        b.free(tmp);
                        b.free(z);
                    } else if (bothInt) {
                        Op opInt = switch (op) {
                        case "=" -> Op.EQ;
                        case "<>" -> Op.NE;
                        case "<" -> Op.LT;
                        case "<=" -> Op.LE;
                        case ">" -> Op.GT;
                        case ">=" -> Op.GE;
                        default -> Op.EQ;
                        };
                        b.emit(Insn.op3(opInt, rd, a, c));
                    } else {
                        throw new RuntimeException("type error: comparison restricted to numbers or text of same kind");
                    }
                } else {
                    Op opInt = switch (op) {
                    case "-" -> Op.SUB;
                    case "*" -> Op.MUL;
                    case "/" -> Op.DIV;
                    case "%" -> Op.MOD;
                    case "&" -> Op.BAND;
                    case "|" -> Op.BOR;
                    case "^" -> Op.XOR;
                    case "<<" -> Op.SHL;
                    case ">>" -> Op.SHR;
                    case "=" -> Op.EQ;
                    case "<>" -> Op.NE;
                    case "<" -> Op.LT;
                    case "<=" -> Op.LE;
                    case ">" -> Op.GT;
                    case ">=" -> Op.GE;
                    case "and" -> Op.AND;
                    case "or" -> Op.OR;
                    default -> throw new RuntimeException("op:" + op);
                    };
                    if (forceReal) {
                        // For real arithmetic, ensure both operands are real by converting the int one
                        Op opReal = switch (op) {
                        case "+" -> Op.FADD;
                        case "-" -> Op.FSUB;
                        case "*" -> Op.FMUL;
                        case "/" -> Op.FDIV;
                        default -> throw new RuntimeException("real op:" + op);
                        };
                        Type lt2 = br.left().type();
                        Type rt2 = br.right().type();
                        if (lt2 instanceof Type.Int && rt2 instanceof Type.Real) {
                            int ar = b.alloc();
                            b.emit(Insn.i2r(ar, a));
                            b.emit(Insn.op3(opReal, rd, ar, c));
                            b.free(ar);
                        } else if (lt2 instanceof Type.Real && rt2 instanceof Type.Int) {
                            int cr = b.alloc();
                            b.emit(Insn.i2r(cr, c));
                            b.emit(Insn.op3(opReal, rd, a, cr));
                            b.free(cr);
                        } else {
                            // both already real
                            b.emit(Insn.op3(opReal, rd, a, c));
                        }
                    } else {
                        // Integer path; ensure logical ops have int operands by enforcing type check
                        if ("and".equals(op) || "or".equals(op)) {
                            if (!(br.left().type() instanceof Type.Int) || !(br.right().type() instanceof Type.Int))
                                throw new RuntimeException("type error: logical and/or require int operands");
                            b.emit(Insn.op3(opInt, rd, a, c));
                        } else {
                            b.emit(Insn.op3(opInt, rd, a, c));
                        }
                    }
                }
            }
            b.free(a);
            b.free(c);
            return rd;
        }
        throw new RuntimeException("Unsupported expression");
    }

    private Type resolvedExprType(Expr e) {
        Type t = e.type();
        if (!(t instanceof Type.Unknown))
            return t;
        if (e instanceof Expr.Variable v) {
            // If it's a global (no current local with that name), prefer recorded
            // globalTypes
            boolean isLocal = currentLocals != null && currentLocals.containsKey(v.name());
            if (!isLocal) {
                Type gt = globalTypes.get(v.name());
                if (gt != null)
                    return gt;
            }
        }
        return t;
    }

    private static final class RegBuilder {
        final Func f;
        final Deque<Integer> free = new ArrayDeque<>();
        final java.util.Set<Integer> reserved = new java.util.HashSet<>();

        RegBuilder(Func f) {
            this.f = f;
            for (int i = 1; i < f.numRegs; i++)
                free.push(i);
        }

        int alloc() {
            if (free.isEmpty())
                throw new RuntimeException("out of regs");
            int r = free.pop();
            // Skip reserved registers
            while (reserved.contains(r)) {
                if (free.isEmpty())
                    throw new RuntimeException("out of regs");
                r = free.pop();
            }
            return r;
        }

        void free(int r) {
            if (r != 0 && !reserved.contains(r))
                free.push(r);
        }

        void emit(Insn i) {
            f.code.add(i);
        }

        void reserve(int r) {
            // Remove r from free pool if present to mark it as occupied by a named local
            java.util.ArrayDeque<Integer> tmp = new java.util.ArrayDeque<>();
            boolean removed = false;
            while (!free.isEmpty()) {
                int v = free.pop();
                if (!removed && v == r) {
                    removed = true;
                    continue;
                }
                tmp.push(v);
            }
            while (!tmp.isEmpty())
                free.push(tmp.pop());
            reserved.add(r);
        }
    }

}
