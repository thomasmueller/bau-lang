package org.bau.mini.ast;

import org.bau.mini.types.Type;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public sealed interface Expr extends Node permits Expr.Literal, Expr.Variable, Expr.Binary, Expr.Unary, Expr.Call,
        Expr.Member, Expr.Index, Expr.NewArray, Expr.Paren {
    Map<Expr, Type> __TYPES = Collections.synchronizedMap(new WeakHashMap<>());

    default Type type() {
        return __TYPES.get(this);
    }

    default void setType(Type t) {
        __TYPES.put(this, t);
    }

    public record Literal(Object value, int line, int column) implements Expr {
    }

    public record Variable(String name, int line, int column) implements Expr {
    }

    public record Binary(Expr left, String op, Expr right, int line, int column) implements Expr {
    }

    public record Unary(String op, Expr expr, int line, int column) implements Expr {
    }

    public record Call(Expr target, Expr[] args, int line, int column) implements Expr {
    }

    public record Member(Expr target, String name, int line, int column) implements Expr {
    }

    public record Index(Expr target, Expr index, int line, int column) implements Expr {
    }

    public record NewArray(String baseType, Expr length, int line, int column) implements Expr {
    }

    public record Paren(Expr inner, int line, int column) implements Expr {
    }
}
