package org.bau.mini.ast;

public sealed interface Stmt extends Node permits Stmt.Assign, Stmt.Update, Stmt.ExprStmt, Stmt.If, Stmt.Loop, Stmt.For,
        Stmt.Exit, Stmt.Return, Stmt.Block, Stmt.Function, Stmt.TypeDef, Stmt.Program {
    public record Assign(Expr target, Expr value, int line, int column) implements Stmt {
    }

    public record Update(Expr target, Expr value, int line, int column) implements Stmt {
    }

    public record ExprStmt(Expr expr, int line, int column) implements Stmt {
    }

    public record If(Expr[] conds, Block[] blocks, Block elseBlock, int line, int column) implements Stmt {
    }

    public record Loop(Expr cond, Block body, int line, int column) implements Stmt {
    }

    public record For(String var, Expr limit, Block body, int line, int column) implements Stmt {
    }

    public record Exit(int line, int column) implements Stmt {
    }

    public record Return(Expr value, int line, int column) implements Stmt {
    }

    public record Block(Stmt[] statements, int line, int column) implements Stmt {
    }

    public record Function(String name, Param[] params, String returnType, Block body, int line, int column)
            implements Stmt {
    }

    public record Param(String name, String type) {
    }

    public record TypeDef(String name, Param[] fields, int line, int column) implements Stmt {
    }

    public record Program(Stmt[] items) implements Stmt {
        @Override
        public int line() {
            return 1;
        }

        @Override
        public int column() {
            return 1;
        }
    }
}
