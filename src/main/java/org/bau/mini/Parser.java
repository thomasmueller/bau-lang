package org.bau.mini;

import org.bau.mini.ast.Expr;
import org.bau.mini.ast.Stmt;

import java.util.ArrayList;
import java.util.List;

final class Parser {
    private final List<Token> tokens;
    private int pos = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    Stmt.Program parseProgram() {
        List<Stmt> items = new ArrayList<>();
        while (!check(TokenType.EOF)) {
            if (check(TokenType.NEWLINE)) {
                advance();
                continue;
            }
            if (check(TokenType.DEDENT)) {
                advance();
                continue;
            }
            if (check(TokenType.TYPE))
                items.add(parseTypeDef());
            else if (check(TokenType.FUN))
                items.add(parseFunction());
            else
                items.add(parseStatement());
        }
        consume(TokenType.EOF, "expected EOF");
        Stmt.Program p = new Stmt.Program(items.toArray(new Stmt[0]));
        validateNoVoidCallInExpression(p);
        return p;
    }

    private Stmt parseTypeDef() {
        Token kw = consume(TokenType.TYPE, "expected 'type'");
        Token name = consume(TokenType.IDENT, "expected type name");
        consume(TokenType.LPAREN, "expected '('");
        List<Stmt.Param> fields = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            do {
                Token fn = consume(TokenType.IDENT, "expected field name");
                String typeName = parseTypeName();
                fields.add(new Stmt.Param(fn.lexeme, typeName));
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.RPAREN, "expected ')'");
        skipOptionalNewlines();
        return new Stmt.TypeDef(name.lexeme, fields.toArray(new Stmt.Param[0]), kw.line, kw.column);
    }

    private String parseTypeName() {
        if (check(TokenType.FUN)) {
            advance();
            return "fun";
        }
        Token base = consume(TokenType.IDENT, "expected type name");
        String typeName = base.lexeme;
        if (check(TokenType.LBRACK)) {
            consume(TokenType.LBRACK, "expected '['");
            consume(TokenType.RBRACK, "expected ']' ");
            typeName += "[]";
        }
        return typeName;
    }

    private Stmt parseFunction() {
        Token kw = consume(TokenType.FUN, "expected 'fun'");
        Token name = consume(TokenType.IDENT, "expected function name");
        consume(TokenType.LPAREN, "expected '('");
        List<Stmt.Param> params = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            do {
                Token pn = consume(TokenType.IDENT, "expected parameter name");
                String typeName = parseTypeName();
                params.add(new Stmt.Param(pn.lexeme, typeName));
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.RPAREN, "expected ')'");
        String retType = null;
        if (check(TokenType.IDENT) || check(TokenType.FUN)) {
            retType = parseTypeName();
        }
        Stmt.Block body = parseBlock();
        return new Stmt.Function(name.lexeme, params.toArray(new Stmt.Param[0]), retType, body, kw.line, kw.column);
    }

    private Stmt.Block parseBlock() {
        skipOptionalNewlines();
        consume(TokenType.INDENT, "expected indented block");
        List<Stmt> stmts = new ArrayList<>();
        while (!check(TokenType.DEDENT) && !check(TokenType.EOF)) {
            if (check(TokenType.NEWLINE)) {
                advance();
                continue;
            }
            stmts.add(parseStatement());
        }
        consume(TokenType.DEDENT, "expected end of block");
        return new Stmt.Block(stmts.toArray(new Stmt[0]), currentLine(), currentColumn());
    }

    private Stmt parseStatement() {
        if (check(TokenType.IF))
            return parseIf();
        if (check(TokenType.LOOP))
            return parseLoop();
        if (check(TokenType.FOR))
            return parseFor();
        if (check(TokenType.EXIT)) {
            Token t = advance();
            skipLine();
            return new Stmt.Exit(t.line, t.column);
        }
        if (check(TokenType.RET)) {
            Token t = advance();
            if (check(TokenType.NEWLINE) || check(TokenType.DEDENT))
                return new Stmt.Return(null, t.line, t.column);
            Expr e = parseExpr();
            skipLine();
            return new Stmt.Return(e, t.line, t.column);
        }
        // assignment or update or expr
        int save = pos;
        Expr lhs = parsePostfix();
        if (match(TokenType.ASSIGN)) {
            Expr rhs = parseExpr();
            skipLine();
            return new Stmt.Assign(lhs, rhs, currentLine(), currentColumn());
        }
        if (match(TokenType.PLUS_ASSIGN)) {
            Expr rhs = parseExpr();
            skipLine();
            return new Stmt.Update(lhs, rhs, currentLine(), currentColumn());
        }
        // not assignment → treat lhs as full expr and continue
        pos = save;
        Expr e = parseExpr();
        skipLine();
        return new Stmt.ExprStmt(e, currentLine(), currentColumn());
    }

    private Stmt parseIf() {
        Token kw = consume(TokenType.IF, "expected 'if'");
        List<Expr> conds = new ArrayList<>();
        List<Stmt.Block> blocks = new ArrayList<>();
        conds.add(parseExpr());
        blocks.add(parseBlock());
        while (match(TokenType.ELIF)) {
            conds.add(parseExpr());
            blocks.add(parseBlock());
        }
        Stmt.Block elseBlock = null;
        if (match(TokenType.ELSE)) {
            elseBlock = parseBlock();
        }
        return new Stmt.If(conds.toArray(new Expr[0]), blocks.toArray(new Stmt.Block[0]), elseBlock, kw.line,
                kw.column);
    }

    private Stmt parseLoop() {
        Token kw = consume(TokenType.LOOP, "expected 'loop'");
        Expr cond = parseExpr();
        Stmt.Block body = parseBlock();
        return new Stmt.Loop(cond, body, kw.line, kw.column);
    }

    private Stmt parseFor() {
        Token kw = consume(TokenType.FOR, "expected 'for'");
        Token var = consume(TokenType.IDENT, "expected loop variable");
        consume(TokenType.LT, "expected '<'");
        Expr limit = parseExpr();
        Stmt.Block body = parseBlock();
        return new Stmt.For(var.lexeme, limit, body, kw.line, kw.column);
    }

    // Pratt parser for expressions
    private Expr parseExpr() {
        return parseExpression(0);
    }

    private Expr parseExpression(int minBp) {
        Expr left = parsePrefix();
        while (true) {
            // Postfix operators (highest binding power)
            if (check(TokenType.LPAREN)) {
                if (90 < minBp)
                    break;
                advance();
                List<Expr> args = new ArrayList<>();
                if (!check(TokenType.RPAREN)) {
                    do {
                        args.add(parseExpression(0));
                    } while (match(TokenType.COMMA));
                }
                consume(TokenType.RPAREN, "expected ')'");
                left = new Expr.Call(left, args.toArray(new Expr[0]), currentLine(), currentColumn());
                continue;
            }
            if (check(TokenType.LBRACK)) {
                if (90 < minBp)
                    break;
                advance();
                Expr idx = parseExpression(0);
                consume(TokenType.RBRACK, "expected ']'");
                left = new Expr.Index(left, idx, currentLine(), currentColumn());
                continue;
            }
            if (check(TokenType.DOT)) {
                if (90 < minBp)
                    break;
                advance();
                Token id = consume(TokenType.IDENT, "expected member name");
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
        if (match(TokenType.MINUS)) {
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
        TokenType t = peek().type;
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
            if (match(TokenType.LPAREN)) {
                List<Expr> args = new ArrayList<>();
                if (!check(TokenType.RPAREN)) {
                    do {
                        args.add(parseExpression(0));
                    } while (match(TokenType.COMMA));
                }
                consume(TokenType.RPAREN, "expected ')'");
                e = new Expr.Call(e, args.toArray(new Expr[0]), currentLine(), currentColumn());
            } else if (match(TokenType.LBRACK)) {
                Expr idx = parseExpression(0);
                consume(TokenType.RBRACK, "expected ']'");
                e = new Expr.Index(e, idx, currentLine(), currentColumn());
            } else if (match(TokenType.DOT)) {
                Token id = consume(TokenType.IDENT, "expected member name");
                e = new Expr.Member(e, id.lexeme, currentLine(), currentColumn());
            } else
                break;
        }
        return e;
    }

    private Expr parsePrimary() {
        if (match(TokenType.INT))
            return new Expr.Literal(Long.parseLong(prev().lexeme), prev().line, prev().column);
        if (match(TokenType.REAL))
            return new Expr.Literal(Double.parseDouble(prev().lexeme), prev().line, prev().column);
        if (match(TokenType.TEXT))
            return new Expr.Literal(prev().lexeme, prev().line, prev().column);
        if (check(TokenType.FUN)) {
            Token t = advance();
            if (match(TokenType.LBRACK)) {
                Expr len = parseExpr();
                consume(TokenType.RBRACK, "expected ']'");
                return new Expr.NewArray("fun", len, t.line, t.column);
            }
            return new Expr.Variable("fun", t.line, t.column);
        }
        if (check(TokenType.IDENT)) {
            int save = pos;
            Token t = advance();
            if (("int".equals(t.lexeme) || "real".equals(t.lexeme) || "text".equals(t.lexeme))
                    && match(TokenType.LBRACK)) {
                Expr len = parseExpr();
                consume(TokenType.RBRACK, "expected ']'");
                return new Expr.NewArray(t.lexeme, len, t.line, t.column);
            }
            pos = save;
        }
        if (match(TokenType.IDENT))
            return new Expr.Variable(prev().lexeme, prev().line, prev().column);
        if (match(TokenType.LPAREN)) {
            Expr e = parseExpr();
            consume(TokenType.RPAREN, "expected ')'");
            return new Expr.Paren(e, currentLine(), currentColumn());
        }
        throw error("expected expression");
    }

    // Utility
    private void skipLine() {
        while (!check(TokenType.NEWLINE) && !check(TokenType.DEDENT) && !check(TokenType.EOF))
            advance();
        if (check(TokenType.NEWLINE))
            advance();
    }

    private void skipOptionalNewlines() {
        while (match(TokenType.NEWLINE))
            ;
    }

    private boolean match(TokenType t) {
        if (check(t)) {
            advance();
            return true;
        }
        return false;
    }

    private boolean check(TokenType t) {
        return peek().type == t;
    }

    private Token advance() {
        if (!isAtEnd())
            pos++;
        return prev();
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(pos);
    }

    private Token prev() {
        return tokens.get(pos - 1);
    }

    private Token consume(TokenType t, String msg) {
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
    private void validateNoVoidCallInExpression(Stmt.Program program) {
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
        } else if (s instanceof Stmt.Update u) {
            validateExpr(u.target(), false, returnsValue);
            validateExpr(u.value(), false, returnsValue);
        } else if (s instanceof Stmt.ExprStmt e) {
            validateExpr(e.expr(), true, returnsValue);
        } else if (s instanceof Stmt.If iff) {
            for (org.bau.mini.ast.Expr c : iff.conds())
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
        } else if (s instanceof Stmt.For fr) {
            validateExpr(fr.limit(), false, returnsValue);
            for (Stmt is : fr.body().statements())
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

    private void validateLvalue(org.bau.mini.ast.Expr e, java.util.Map<String, Boolean> returnsValue) {
        if (e instanceof org.bau.mini.ast.Expr.Index ix) {
            validateExpr(ix.target(), false, returnsValue);
            validateExpr(ix.index(), false, returnsValue);
        } else if (e instanceof org.bau.mini.ast.Expr.Member m) {
            validateExpr(m.target(), false, returnsValue);
        }
    }

    private void validateExpr(org.bau.mini.ast.Expr e, boolean allowVoidAtTop,
            java.util.Map<String, Boolean> returnsValue) {
        if (e instanceof org.bau.mini.ast.Expr.Call c) {
            boolean isVoid = false;
            if (c.target() instanceof org.bau.mini.ast.Expr.Variable v && returnsValue.containsKey(v.name()))
                isVoid = !returnsValue.get(v.name());
            // arguments must always be value-producing
            for (org.bau.mini.ast.Expr a : c.args())
                validateExpr(a, false, returnsValue);
            if (isVoid) {
                if (!allowVoidAtTop)
                    throw error("void function used as expression");
            }
            // also validate the target expression if complex
            if (!(c.target() instanceof org.bau.mini.ast.Expr.Variable))
                validateExpr(c.target(), false, returnsValue);
            return;
        }
        if (e instanceof org.bau.mini.ast.Expr.Binary b) {
            validateExpr(b.left(), false, returnsValue);
            validateExpr(b.right(), false, returnsValue);
            return;
        }
        if (e instanceof org.bau.mini.ast.Expr.Unary u) {
            validateExpr(u.expr(), false, returnsValue);
            return;
        }
        if (e instanceof org.bau.mini.ast.Expr.Index ix) {
            validateExpr(ix.target(), false, returnsValue);
            validateExpr(ix.index(), false, returnsValue);
            return;
        }
        if (e instanceof org.bau.mini.ast.Expr.Member m) {
            validateExpr(m.target(), false, returnsValue);
            return;
        }
        if (e instanceof org.bau.mini.ast.Expr.Paren p) {
            validateExpr(p.inner(), allowVoidAtTop, returnsValue);
            return;
        }
        // literals and variables are fine
    }
}
