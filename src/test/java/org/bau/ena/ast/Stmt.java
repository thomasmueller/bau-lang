package org.bau.ena.ast;

public sealed interface Stmt extends Node permits Stmt.Assign, Stmt.ExprStmt, Stmt.If, Stmt.Loop,
        Stmt.Exit, Stmt.Return, Stmt.Block, Stmt.Function, Stmt.EnaTypeDef, Stmt.EnaProgram {
    public record Assign(Expr target, Expr value, int line, int column) implements Stmt {
    }

    public record ExprStmt(Expr expr, int line, int column) implements Stmt {
    }

    public record If(Expr[] conds, Block[] blocks, Block elseBlock, int line, int column) implements Stmt {
    }

    public record Loop(Expr cond, Block body, int line, int column) implements Stmt {
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

    public record EnaTypeDef(String name, Param[] fields, int line, int column) implements Stmt {
    }

    public record EnaProgram(Stmt[] items) implements Stmt {
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
