package org.bau.ena;

import java.util.ArrayList;
import java.util.List;

import org.bau.ena.ast.Expr;
import org.bau.ena.ast.Stmt;
import org.bau.ena.ast.Stmt.EnaProgram;

public final class EnaParser {
    private final List<EnaToken> tokens;
    private int pos = 0;

    private EnaParser(List<EnaToken> tokens) {
        this.tokens = tokens;
    }

    private Stmt.EnaProgram parseProgram() {
        List<Stmt> items = new ArrayList<>();
        while (!check(EnaTokenType.EOF)) {
            if (check(EnaTokenType.NEWLINE)) {
                advance();
                continue;
            }
            if (check(EnaTokenType.DEDENT)) {
                advance();
                continue;
            }
            if (check(EnaTokenType.TYPE))
                items.add(parseTypeDef());
            else if (check(EnaTokenType.FUN))
                items.add(parseFunction());
            else
                items.add(parseStatement());
        }
        consume(EnaTokenType.EOF, "expected EOF");
        Stmt.EnaProgram p = new Stmt.EnaProgram(items.toArray(new Stmt[0]));
        validateNoVoidCallInExpression(p);
        return p;
    }

    private Stmt parseTypeDef() {
        EnaToken kw = consume(EnaTokenType.TYPE, "expected 'type'");
        EnaToken name = consume(EnaTokenType.IDENT, "expected type name");
        consume(EnaTokenType.LPAREN, "expected '('");
        List<Stmt.Param> fields = new ArrayList<>();
        if (!check(EnaTokenType.RPAREN)) {
            do {
                EnaToken fn = consume(EnaTokenType.IDENT, "expected field name");
                String typeName = parseTypeName();
                fields.add(new Stmt.Param(fn.lexeme, typeName));
            } while (match(EnaTokenType.COMMA));
        }
        consume(EnaTokenType.RPAREN, "expected ')'");
        skipOptionalNewlines();
        return new Stmt.EnaTypeDef(name.lexeme, fields.toArray(new Stmt.Param[0]), kw.line, kw.column);
    }

    private String parseTypeName() {
        if (check(EnaTokenType.FUN)) {
            advance();
            return "fun";
        }
        EnaToken base = consume(EnaTokenType.IDENT, "expected type name");
        String typeName = base.lexeme;
        if (check(EnaTokenType.LBRACK)) {
            consume(EnaTokenType.LBRACK, "expected '['");
            consume(EnaTokenType.RBRACK, "expected ']' ");
            typeName += "[]";
        }
        return typeName;
    }

    private Stmt parseFunction() {
        EnaToken kw = consume(EnaTokenType.FUN, "expected 'fun'");
        EnaToken name = consume(EnaTokenType.IDENT, "expected function name");
        consume(EnaTokenType.LPAREN, "expected '('");
        List<Stmt.Param> params = new ArrayList<>();
        if (!check(EnaTokenType.RPAREN)) {
            do {
                EnaToken pn = consume(EnaTokenType.IDENT, "expected parameter name");
                String typeName = parseTypeName();
                params.add(new Stmt.Param(pn.lexeme, typeName));
            } while (match(EnaTokenType.COMMA));
        }
        consume(EnaTokenType.RPAREN, "expected ')'");
        String retType = null;
        if (check(EnaTokenType.IDENT) || check(EnaTokenType.FUN)) {
            retType = parseTypeName();
        }
        Stmt.Block body = parseBlock();
        return new Stmt.Function(name.lexeme, params.toArray(new Stmt.Param[0]), retType, body, kw.line, kw.column);
    }

    private Stmt.Block parseBlock() {
        skipOptionalNewlines();
        consume(EnaTokenType.INDENT, "expected indented block");
        List<Stmt> stmts = new ArrayList<>();
        while (!check(EnaTokenType.DEDENT) && !check(EnaTokenType.EOF)) {
            if (check(EnaTokenType.NEWLINE)) {
                advance();
                continue;
            }
            stmts.add(parseStatement());
        }
        consume(EnaTokenType.DEDENT, "expected end of block");
        return new Stmt.Block(stmts.toArray(new Stmt[0]), currentLine(), currentColumn());
    }

    private Stmt parseStatement() {
        if (check(EnaTokenType.IF))
            return parseIf();
        if (check(EnaTokenType.LOOP))
            return parseLoop();
        if (check(EnaTokenType.EXIT)) {
            EnaToken t = advance();
            skipLine();
            return new Stmt.Exit(t.line, t.column);
        }
        if (check(EnaTokenType.RET)) {
            EnaToken t = advance();
            if (check(EnaTokenType.NEWLINE) || check(EnaTokenType.DEDENT))
                return new Stmt.Return(null, t.line, t.column);
            Expr e = parseExpr();
            skipLine();
            return new Stmt.Return(e, t.line, t.column);
        }
        // assignment or update or expr
        int save = pos;
        Expr lhs = parsePostfix();
        if (match(EnaTokenType.ASSIGN)) {
            Expr rhs = parseExpr();
            skipLine();
            return new Stmt.Assign(lhs, rhs, currentLine(), currentColumn());
        }
        // not assignment → treat lhs as full expr and continue
        pos = save;
        Expr e = parseExpr();
        skipLine();
        return new Stmt.ExprStmt(e, currentLine(), currentColumn());
    }

    private Stmt parseIf() {
        EnaToken kw = consume(EnaTokenType.IF, "expected 'if'");
        List<Expr> conds = new ArrayList<>();
        List<Stmt.Block> blocks = new ArrayList<>();
        Expr firstCond;
        if (check(EnaTokenType.NEWLINE) || check(EnaTokenType.DEDENT) || check(EnaTokenType.EOF)) {
            firstCond = new Expr.Literal(1L, kw.line, kw.column);
        } else {
            firstCond = parseExpr();
        }
        conds.add(firstCond);
        blocks.add(parseBlock());
        while (match(EnaTokenType.ELIF)) {
            // Keep 'elif' condition required as per spec; only 'if' and 'loop' are optional.
            conds.add(parseExpr());
            blocks.add(parseBlock());
        }
        Stmt.Block elseBlock = null;
        if (match(EnaTokenType.ELSE)) {
            elseBlock = parseBlock();
        }
        return new Stmt.If(conds.toArray(new Expr[0]), blocks.toArray(new Stmt.Block[0]), elseBlock, kw.line,
                kw.column);
    }

    private Stmt parseLoop() {
        EnaToken kw = consume(EnaTokenType.LOOP, "expected 'loop'");
        Expr cond;
        if (check(EnaTokenType.NEWLINE) || check(EnaTokenType.DEDENT) || check(EnaTokenType.EOF)) {
            cond = new Expr.Literal(1L, kw.line, kw.column);
        } else {
            cond = parseExpr();
        }
        Stmt.Block body = parseBlock();
        return new Stmt.Loop(cond, body, kw.line, kw.column);
    }

    private Expr parseExpr() {
        return parseExpression(0);
    }

    private Expr parseExpression(int minBp) {
        Expr left = parsePrefix();
        while (true) {
            // Postfix operators (highest binding power)
            if (check(EnaTokenType.LPAREN)) {
                if (90 < minBp)
                    break;
                advance();
                List<Expr> args = new ArrayList<>();
                if (!check(EnaTokenType.RPAREN)) {
                    do {
                        args.add(parseExpression(0));
                    } while (match(EnaTokenType.COMMA));
                }
                consume(EnaTokenType.RPAREN, "expected ')'");
                left = new Expr.Call(left, args.toArray(new Expr[0]), currentLine(), currentColumn());
                continue;
            }
            if (check(EnaTokenType.LBRACK)) {
                if (90 < minBp)
                    break;
                advance();
                Expr idx = parseExpression(0);
                consume(EnaTokenType.RBRACK, "expected ']'");
                left = new Expr.Index(left, idx, currentLine(), currentColumn());
                continue;
            }
            if (check(EnaTokenType.DOT)) {
                if (90 < minBp)
                    break;
                advance();
                EnaToken id = consume(EnaTokenType.IDENT, "expected member name");
                left = new Expr.Member(left, id.lexeme, currentLine(), currentColumn());
                continue;
            }

            // Infix operators
            Infix inf = currentInfix();
            if (inf == null || inf.bp < minBp)
                break;
            String op = inf.op;
            int nextBp = inf.leftAssoc ? inf.bp + 1 : inf.bp;
            advance(); // consume operator token
            Expr right = parseExpression(nextBp);
            left = new Expr.Binary(left, op, right, currentLine(), currentColumn());
        }
        return left;
    }

    private Expr parsePrefix() {
        if (match(EnaTokenType.MINUS)) {
            Expr rhs = parseExpression(80);
            return new Expr.Unary("-", rhs, currentLine(), currentColumn());
        }
        return parsePrimary();
    }

    private static final class Infix {
        final String op;
        final int bp;
        final boolean leftAssoc;

        Infix(String o, int b, boolean l) {
            op = o;
            bp = b;
            leftAssoc = l;
        }
    }

    private Infix currentInfix() {
        EnaTokenType t = peek().type;
        return switch (t) {
        // Go-like precedence (higher bp means tighter binding in our Pratt setup)
        // 1: or (lowest)
        case OR -> new Infix("or", 10, true);
        // 2: and
        case AND -> new Infix("and", 20, true);
        // 3: comparison: = <> < <= > >=
        case EQ -> new Infix("=", 30, true);
        case NE -> new Infix("<>", 30, true);
        case LT -> new Infix("<", 30, true);
        case LE -> new Infix("<=", 30, true);
        case GT -> new Infix(">", 30, true);
        case GE -> new Infix(">=", 30, true);
        // 4: add: + - | ^
        case PLUS -> new Infix("+", 40, true);
        case MINUS -> new Infix("-", 40, true);
        case BAR -> new Infix("|", 40, true);
        case CARET -> new Infix("^", 40, true);
        // 5: mul: * / % << >> &
        case STAR -> new Infix("*", 50, true);
        case SLASH -> new Infix("/", 50, true);
        case PERCENT -> new Infix("%", 50, true);
        case SHL -> new Infix("<<", 50, true);
        case SHR -> new Infix(">>", 50, true);
        case AMP -> new Infix("&", 50, true);
        default -> null;
        };
    }

    // Restricted postfix-only parser for assignment LHS detection
    private Expr parsePostfix() {
        Expr e = parsePrimary();
        while (true) {
            if (match(EnaTokenType.LPAREN)) {
                List<Expr> args = new ArrayList<>();
                if (!check(EnaTokenType.RPAREN)) {
                    do {
                        args.add(parseExpression(0));
                    } while (match(EnaTokenType.COMMA));
                }
                consume(EnaTokenType.RPAREN, "expected ')'");
                e = new Expr.Call(e, args.toArray(new Expr[0]), currentLine(), currentColumn());
            } else if (match(EnaTokenType.LBRACK)) {
                Expr idx = parseExpression(0);
                consume(EnaTokenType.RBRACK, "expected ']'");
                e = new Expr.Index(e, idx, currentLine(), currentColumn());
            } else if (match(EnaTokenType.DOT)) {
                EnaToken id = consume(EnaTokenType.IDENT, "expected member name");
                e = new Expr.Member(e, id.lexeme, currentLine(), currentColumn());
            } else
                break;
        }
        return e;
    }

    private Expr parsePrimary() {
        if (match(EnaTokenType.INT))
            return new Expr.Literal(Long.parseLong(prev().lexeme), prev().line, prev().column);
        if (match(EnaTokenType.REAL))
            return new Expr.Literal(Double.parseDouble(prev().lexeme), prev().line, prev().column);
        if (match(EnaTokenType.TEXT))
            return new Expr.Literal(prev().lexeme, prev().line, prev().column);
        if (check(EnaTokenType.FUN)) {
            EnaToken t = advance();
            if (match(EnaTokenType.LBRACK)) {
                Expr len = parseExpr();
                consume(EnaTokenType.RBRACK, "expected ']'");
                return new Expr.NewArray("fun", len, t.line, t.column);
            }
            return new Expr.Variable("fun", t.line, t.column);
        }
        if (check(EnaTokenType.IDENT)) {
            int save = pos;
            EnaToken t = advance();
            if (("int".equals(t.lexeme) || "real".equals(t.lexeme) || "text".equals(t.lexeme))
                    && match(EnaTokenType.LBRACK)) {
                Expr len = parseExpr();
                consume(EnaTokenType.RBRACK, "expected ']'");
                return new Expr.NewArray(t.lexeme, len, t.line, t.column);
            }
            pos = save;
        }
        if (match(EnaTokenType.IDENT))
            return new Expr.Variable(prev().lexeme, prev().line, prev().column);
        if (match(EnaTokenType.LPAREN)) {
            Expr e = parseExpr();
            consume(EnaTokenType.RPAREN, "expected ')'");
            return new Expr.Paren(e, currentLine(), currentColumn());
        }
        throw error("expected expression");
    }

    // Utility
    private void skipLine() {
        while (!check(EnaTokenType.NEWLINE) && !check(EnaTokenType.DEDENT) && !check(EnaTokenType.EOF))
            advance();
        if (check(EnaTokenType.NEWLINE))
            advance();
    }

    private void skipOptionalNewlines() {
        while (match(EnaTokenType.NEWLINE))
            ;
    }

    private boolean match(EnaTokenType t) {
        if (check(t)) {
            advance();
            return true;
        }
        return false;
    }

    private boolean check(EnaTokenType t) {
        return peek().type == t;
    }

    private EnaToken advance() {
        if (!isAtEnd())
            pos++;
        return prev();
    }

    private boolean isAtEnd() {
        return peek().type == EnaTokenType.EOF;
    }

    private EnaToken peek() {
        return tokens.get(pos);
    }

    private EnaToken prev() {
        return tokens.get(pos - 1);
    }

    private EnaToken consume(EnaTokenType t, String msg) {
        if (check(t))
            return advance();
        throw error(msg + ", got " + peek());
    }

    private int currentLine() {
        return isAtEnd() ? prev().line : peek().line;
    }

    private int currentColumn() {
        return isAtEnd() ? prev().column : peek().column;
    }

    private RuntimeException error(String msg) {
        return new RuntimeException("Parse error @" + currentLine() + ":" + currentColumn() + ": " + msg);
    }

    // Semantic validation: void functions cannot be used as expressions
    private void validateNoVoidCallInExpression(Stmt.EnaProgram program) {
        java.util.Map<String, Boolean> returnsValue = new java.util.HashMap<>();
        for (Stmt s : program.items())
            if (s instanceof Stmt.Function fn)
                returnsValue.put(fn.name(), fn.returnType() != null);
        for (Stmt s : program.items())
            validateStmt(s, returnsValue);
    }

    private void validateStmt(Stmt s, java.util.Map<String, Boolean> returnsValue) {
        if (s instanceof Stmt.Assign a) {
            validateExpr(a.value(), false, returnsValue);
            validateLvalue(a.target(), returnsValue);
        } else if (s instanceof Stmt.ExprStmt e) {
            validateExpr(e.expr(), true, returnsValue);
        } else if (s instanceof Stmt.If iff) {
            for (org.bau.ena.ast.Expr c : iff.conds())
                validateExpr(c, false, returnsValue);
            for (Stmt.Block b : iff.blocks())
                for (Stmt is : b.statements())
                    validateStmt(is, returnsValue);
            if (iff.elseBlock() != null)
                for (Stmt is : iff.elseBlock().statements())
                    validateStmt(is, returnsValue);
        } else if (s instanceof Stmt.Loop lp) {
            validateExpr(lp.cond(), false, returnsValue);
            for (Stmt is : lp.body().statements())
                validateStmt(is, returnsValue);
        } else if (s instanceof Stmt.Return r) {
            if (r.value() != null)
                validateExpr(r.value(), false, returnsValue);
        } else if (s instanceof Stmt.Function fn) {
            for (Stmt is : fn.body().statements())
                validateStmt(is, returnsValue);
        }
        // TypeDef/Function handled elsewhere
    }

    private void validateLvalue(org.bau.ena.ast.Expr e, java.util.Map<String, Boolean> returnsValue) {
        if (e instanceof org.bau.ena.ast.Expr.Index ix) {
            validateExpr(ix.target(), false, returnsValue);
            validateExpr(ix.index(), false, returnsValue);
        } else if (e instanceof org.bau.ena.ast.Expr.Member m) {
            validateExpr(m.target(), false, returnsValue);
        }
    }

    private void validateExpr(org.bau.ena.ast.Expr e, boolean allowVoidAtTop,
            java.util.Map<String, Boolean> returnsValue) {
        if (e instanceof org.bau.ena.ast.Expr.Call c) {
            boolean isVoid = false;
            if (c.target() instanceof org.bau.ena.ast.Expr.Variable v && returnsValue.containsKey(v.name()))
                isVoid = !returnsValue.get(v.name());
            // arguments must always be value-producing
            for (org.bau.ena.ast.Expr a : c.args())
                validateExpr(a, false, returnsValue);
            if (isVoid) {
                if (!allowVoidAtTop)
                    throw error("void function used as expression");
            }
            // also validate the target expression if complex
            if (!(c.target() instanceof org.bau.ena.ast.Expr.Variable))
                validateExpr(c.target(), false, returnsValue);
            return;
        }
        if (e instanceof org.bau.ena.ast.Expr.Binary b) {
            validateExpr(b.left(), false, returnsValue);
            validateExpr(b.right(), false, returnsValue);
            return;
        }
        if (e instanceof org.bau.ena.ast.Expr.Unary u) {
            validateExpr(u.expr(), false, returnsValue);
            return;
        }
        if (e instanceof org.bau.ena.ast.Expr.Index ix) {
            validateExpr(ix.target(), false, returnsValue);
            validateExpr(ix.index(), false, returnsValue);
            return;
        }
        if (e instanceof org.bau.ena.ast.Expr.Member m) {
            validateExpr(m.target(), false, returnsValue);
            return;
        }
        if (e instanceof org.bau.ena.ast.Expr.Paren p) {
            validateExpr(p.inner(), allowVoidAtTop, returnsValue);
            return;
        }
        // literals and variables are fine
    }

    public static EnaProgram parse(String src) {
        src = """
def +:
    @{left}: (@{left}) + (@{rest})

def -:
    @{left}: (@{left}) - (@{rest})

def break condition
    if condition
        exit

def for var, from, until
    if 1
        var: from
        loop var < (until)
            @{body}
            var : var + 1

                """+ src;
        src = EnaPreprocessor.convert(src);
        EnaLexer lx = new EnaLexer(src);
        List<EnaToken> toks = lx.tokenize();
        EnaParser p = new EnaParser(toks);
        return p.parseProgram();
    }
}
