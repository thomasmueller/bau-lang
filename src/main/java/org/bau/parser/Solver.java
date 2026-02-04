package org.bau.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * A constraint-based abstract interpreter for proving safety properties.
 * A symbolic static analyzer used to eliminate redundant runtime checks.
 * A lightweight symbolic reasoning engine for program safety verification.
 *
 * Keywords:
 * symbolic static analysis engine,
 * constraint-based abstract interpreter,
 * symbolic constraint solver,
 * equational reasoning engine,
 * small polyhedral engine,
 * a numeric abstract domain,
 * linear integer arithmetic,
 * symbolic substitution,
 * interval domain,
 * octagon domain (x - y <= c),
 * polyhedra domain
 *
 * In Java, the JIT removes the checks if it can prove they are unnecessary.
 * In Rust, the LLVM removes some of the checks.
 * C and C++ do not do any checking.
 */
public class Solver {

    // parent solver, for global rules
    private Solver parentSolver;

    private ArrayList<Rule> rules = new ArrayList<>();

    public Solver(Solver parentSolver) {
        this.parentSolver = parentSolver;
        if (parentSolver != null) {
            rules.addAll(parentSolver.rules);
        }
    }

    public Solver() {
        this(null);
    }

    public boolean isTrue(Rule rule) {
        rule.left = rule.left.simplify();
        rule.right = rule.right.simplify();
        if (rule.left instanceof Variable) {
            Variable v = (Variable) rule.left;
            Expr exp = rule.right;
            return isTrue(v, rule.type, exp);
        } else if (rule.right instanceof Variable) {
            Variable v = (Variable) rule.right;
            Expr exp = rule.left;
            if (isTrue(v, reverse(rule.type), exp)) {
                return true;
            }
        }
        Result r = rule.left.compareTo(rule.right);
        if (r == Result.UNKNOWN) {
            ArrayList<Variable> target = new ArrayList<Variable>();
            collectVariables(rule.left, target);
            for (Variable v : target) {
                Rule r2 = trySolveFor(rule, v);
                if (r2 != null) {
                    if (isTrue(r2)) {
                        return true;
                    }
                }
            }
        }
        if (r == Result.UNKNOWN && rule.left.complexity() < rule.right.complexity()) {
            Rule r2 = rule(rule.right, reverse(rule.type), rule.left);
            return isTrue(r2);
        }
        switch (rule.type) {
        case "=":
            return r == Result.EQUAL;
        case ">=":
            return r == Result.EQUAL || r == Result.LARGER;
        case "<=":
            return r == Result.EQUAL || r == Result.SMALLER;
        case ">":
            return r == Result.LARGER;
        case "<":
            return r == Result.SMALLER;
        }
        throw new IllegalArgumentException();
    }

    public void clear() {
        // remove all rules that don't always apply
        for (int i = 0; i < rules.size(); i++) {
            Solver.Rule r = rules.get(i);
            if (r.global) {
                continue;
            }
            rules.remove(i);
            i--;
        }
    }

    public void addRule(Rule r) {
        if (!r.isComplete()) {
            return;
        }
        r.left = r.left.simplify();
        r.right = r.right.simplify();
        if (isAlwaysTrue(r, 0)) {
            return;
        }
        if (r.global && parentSolver != null) {
            parentSolver.addRule(r);
        }
//        if (r.type.equals("=") || r.type.equals("<>")) {
//            // remove previous assignments, if they are values
//            if (r.right instanceof Number) {
//                for (int i = 0; i < rules.size(); i++) {
//                    Rule r0 = rules.get(i);
//                    if (r0.left.equals(r.left) || r0.right.equals(r.left)) {
////                        if (r0.always || (!r0.type.equals("<>") && !r0.type.equals("="))) {
////                            continue;
////                        }
//                        rules.remove(i);
//                        i--;
//                    }
//                }
//            }
//        }
        rules.add(r);
    }

    private boolean isAlwaysTrue(Rule r, int recursionDepth) {
        if (recursionDepth > 10) {
            return false;
        }
        // eg. "x + 1 > x + 0"
        r.left = r.left.simplify();
        r.right = r.right.simplify();
        if ((r.left instanceof Number) && (r.right instanceof Number)) {
            long ln = ((Number) r.left).value;
            long rn = ((Number) r.right).value;
            switch (r.type) {
            case "=":
                return ln == rn;
            case "<":
                return ln < rn;
            case ">":
                return ln > rn;
            case ">=":
                return ln >= rn;
            case "<=":
                return ln <= rn;
            case "<>":
                return ln != rn;
            }
            throw new IllegalArgumentException();
        }
        if ((r.left instanceof Operation) && (r.right instanceof Operation)) {
            Operation lo = (Operation) r.left;
            Operation ro = (Operation) r.right;
            if (lo.left.equals(ro.left)) {
                Rule r2 = new Rule();
                r2.type = r.type;
                switch (lo.type) {
                case "+":
                    r2.left = lo.right;
                    break;
                case "-":
                    r2.left = lo.right.negate();
                    break;
                default:
                    throw new IllegalArgumentException();
                }
                switch (ro.type) {
                case "+":
                    r2.right = ro.right;
                    break;
                case "-":
                    r2.right = ro.right.negate();
                    break;
                default:
                    throw new IllegalArgumentException();
                }
                return isAlwaysTrue(r2, recursionDepth + 1);
            }
        }
        if (r.left instanceof Variable && r.right instanceof Operation) {
            Variable v = (Variable) r.left;
            Operation op = (Operation) r.right;
            if (v.equals(op.left)) {
                return isAlwaysTrue(rule(number(0), r.type, operation(number(0), op.type, op.right).simplify()), recursionDepth + 1);
            }
        } else if (r.right instanceof Variable && r.left instanceof Operation) {
            return isAlwaysTrue(rule(r.right, reverse(r.type), r.left), recursionDepth + 1);
        }
        return false;
    }

    public void removeRulesFor(Expr expr) {
        for (int i = 0; i < rules.size(); i++) {
            Solver.Rule r = rules.get(i);
            if (r.left.equals(expr) || r.right.equals(expr)) {
                rules.remove(i);
                i--;
            }
        }
    }

    public void removeNotAlwaysRules() {
        // remove all rules that don't always apply
        for (int i = 0; i < rules.size(); i++) {
            Solver.Rule r = rules.get(i);
            if (r.always) {
                continue;
            }
            if (r.left.toString().endsWith(".len")) {
                // TODO special case, because array length don't change
                continue;
            }
            rules.remove(i);
            i--;
        }
    }

    public void removeDeeperRules(int depth) {
        for (int i = 0; i < rules.size(); i++) {
            Solver.Rule r = rules.get(i);
            if (r.depth <= depth) {
                continue;
            }
            rules.remove(i);
            i--;
        }
    }

    public void reversRulesOfDepth(int depth) {
        List<Solver.Rule> list = new ArrayList<>();
        for (int i = 0; i < rules.size(); i++) {
            Solver.Rule r = rules.get(i);
            if (r.depth < depth) {
                continue;
            }
            list.add(r);
            rules.remove(i);
            i--;
        }
        for (Solver.Rule r : list) {
            Rule r2 = r.reverse();
            // rules for constants stay constant:
            // if constant > 0
            //    ... now we know constant > 0
            // while x
            //    ... now we know constant <= 0
            r2.always = r.always;
            r2.depth = depth - 1;
            addRule(r2);
        }
    }

    private void addAllEqualExpr(Expr expr, ArrayList<Expr> target) {
        if (expr instanceof Variable) {
            Variable ve = (Variable) expr;
            for (Rule r : rules) {
                Rule r2 = trySolveFor(r, ve);
                if (r2 == null) {
                    continue;
                }
                if (r2.type.equals("=")) {
                    if (!target.contains(r2.right)) {
                        target.add(r2.right);
                        addAllEqualExpr(r2.right, target);
                    }
                }
            }
        }
    }

    private void collectVariables(Expr expr, ArrayList<Variable> target) {
        if (expr instanceof Variable) {
            Variable v = (Variable) expr;
            // we expect very few entries, so performance should be fine
            if (!target.contains(v)) {
                target.add((Variable) expr);
            }
        } else if (expr instanceof Operation) {
            Operation op = (Operation) expr;
            collectVariables(op.left, target);
            collectVariables(op.right, target);
        }
    }

    public boolean isTrue(Variable var, String type, Expr expr) {
        return isTrue(var, type, expr, 0);
    }

    private boolean isTrue(Variable var, String type, Expr expr, int recursionDepth) {
        if (recursionDepth > 10) {
            return false;
        }
        ArrayList<Expr> altExpr = new ArrayList<>();
        altExpr.add(expr);
        addAllEqualExpr(expr, altExpr);
        for (Expr e : altExpr) {
            ArrayList<Variable> alternativeVars = new ArrayList<>();
            for (Rule r : rules) {
                Rule r2 = trySolveFor(r, var);
                if (r2 == null) {
                    continue;
                }
                if (isTrue(type, e, r2.type, r2.right)) {
                    return true;
                }
                // equal or same comparison direction
                if ("=".equals(r2.type) ||
                        type.equals(r2.type) ||
                        (type + "=").equals(r2.type)) {
                    if (r2.right instanceof Variable) {
                        alternativeVars.add((Variable) r2.right);
                    } else if (r2.right instanceof Operation) {
                        if (recursionDepth < 1) {
                            // if we test for, "b >= ..." and find "b = (a + 1)"
                            // then we can test for "(a + 1) >= ..." instead,
                            // solving for "a".
                            // this is "a >= -1", and then test for that.
                            // this will cover the case "a >= 0"
                            Rule r3 = rule(r2.right, type, expr);
                            if (isAlwaysTrue(r3, recursionDepth + 1)) {
                                return true;
                            }
                            ArrayList<Variable> target = new ArrayList<Variable>();
                            collectVariables(r2.right, target);
                            for (Variable a : target) {
                                Rule r4 = trySolveFor(r3, a);
                                if (r4 != null && isTrue(a, type, r4.right, recursionDepth + 1)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
            for (Variable a : alternativeVars) {
                for (Rule r : rules) {
                    Rule r2 = trySolveFor(r, a);
                    if (r2 == null) {
                        continue;
                    }
                    if (isTrue(type, e, r2.type, r2.right)) {
                        return true;
                    }
                }
            }
        }
        if (isAlwaysTrue(rule(var, type, expr), 0)) {
            return true;
        }
        return false;
    }

    public static boolean isTrue(String t1, Expr e1, String t2, Expr e2) {
        if (t1.equals(">")) {
            e1 = operation(e1, "+", number(1)).simplify();
            t1 = ">=";
        } else if (t1.equals("<")) {
            e1 = operation(e1, "+", number(-1)).simplify();
            t1 = "<=";
        }
        if (t2.equals(">")) {
            e2 = operation(e2, "+", number(1)).simplify();
            t2 = ">=";
        } else if (t2.equals("<")) {
            e2 = operation(e2, "+", number(-1)).simplify();
            t2 = "<=";
        }
        Result r = e1.compareTo(e2);
        if (t1.equals(t2)) {
            switch (t1) {
            case "=":
                return r == Result.EQUAL;
            case ">":
                return r == Result.SMALLER || r == Result.EQUAL;
            case "<":
                return r == Result.LARGER || r == Result.EQUAL;
            case ">=":
                return r == Result.SMALLER || r == Result.EQUAL;
            case "<=":
                return r == Result.LARGER || r == Result.EQUAL;
            case "<>":
                return r == Result.EQUAL;
            }
            throw new IllegalArgumentException();
        }
        switch (t1) {
        case ">=":
            if (t2 == null) {
                throw new IllegalArgumentException();
            }
            switch (t2) {
            case ">":
                return r == Result.EQUAL;
            case "=":
                return r == Result.SMALLER || r == Result.EQUAL;
            }
            break;
        case "<=":
            switch (t2) {
            case "<":
                return r == Result.EQUAL;
            case "=":
                return r == Result.LARGER || r == Result.EQUAL;
            }
            break;
        case ">":
            switch (t2) {
            case ">=":
                int todoVerify;
                return r == Result.SMALLER;
            case "=":
                return r == Result.SMALLER;
            }
            break;
        case "<":
            switch (t2) {
            case "<=":
                int todoVerify;
                return r == Result.LARGER;
            case "=":
                return r == Result.LARGER;
            }
            break;
        }
        return false;
    }

    public static Rule trySolveFor(Rule r, Variable var) {
        // first, move the variable to the left
        if (r.left == null) {
            throw new IllegalArgumentException();
        }
        if (r.left.contains(var)) {
            // ok
        } else if (r.right.contains(var)) {
            r = rule(r.right, reverse(r.type), r.left);
        } else {
            return null;
        }
        if (r.left.equals(var)) {
            return r;
        }
        if (r.right.contains(var)) {
            // eg. "x + 1 > x + 0"
            r.left = r.left.simplify();
            r.right = r.right.simplify();
            if ((r.left instanceof Operation) && (r.right instanceof Operation)) {
                Operation lo = (Operation) r.left;
                Operation ro = (Operation) r.right;
                if (lo.left.equals(ro.left)) {
                    Rule r2 = new Rule();
                    r2.type = r.type;
                    switch (lo.type) {
                    case "+":
                        r2.left = lo.right;
                        break;
                    case "-":
                        r2.left = lo.right.negate();
                        break;
                    default:
                        throw new IllegalArgumentException();
                    }
                    switch (ro.type) {
                    case "+":
                        r2.right = ro.right;
                        break;
                    case "-":
                        r2.right = ro.right.negate();
                        break;
                    default:
                        throw new IllegalArgumentException();
                    }
                    r = r2;
                } else {
                    return null;
                }
            }
        }
        // x + 1 = y  =>  x = y - 1
        // x - 1 = y  =>  x = y + 1
        while (r.left instanceof Operation) {
            Operation op = (Operation) r.left;
            if (op.right.contains(var)) {
                if ("-".equals(op.type)) {
                    // (end - start) = len  ==>  +start, -len  ==>  (end - len) = start
                    Rule r2 = rule(operation(op.left, "-", r.right), r.type, op.right);
                    return trySolveFor(r2, var);
                }
                op = op.swap();
            }
            if (op.right.contains(var)) {
                throw new IllegalArgumentException();
            }
            if (!op.left.equals(var)) {
                return null;
            }
            Rule r2 = new Rule();
            r2.type = r.type;
            r2.left = var;
            Operation op2 = new Operation();
            op2.left = r.right;
            op2.right = op.right;
            switch (op.type) {
            case "+":
                op2.type = "-";
                break;
            case "-":
                op2.type = "+";
                break;
            default:
                throw new IllegalArgumentException();
            }
            r2.right = op2.simplify();
            r = r2;
        }
        return r;
    }

    public static String reverse(String type) {
        switch (type) {
        case "=":
        case "<>":
            return type;
        case ">":
            return "<";
        case "<":
            return ">";
        case ">=":
            return "<=";
        case "<=":
            return ">=";
        }
        throw new IllegalArgumentException(type);
    }

    public static Rule rule(Expr left, String type, Expr right) {
        Rule r = new Rule();
        r.left = left;
        r.type = type;
        r.right = right;
        return r;
    }

    public static Number number(long value) {
        Number n = new Number();
        n.value = value;
        return n;
    }

    public static Operation operation(Expr left, String type, Expr right) {
        Operation o = new Operation();
        o.left = left;
        o.type = type;
        o.right = right;
        return o;
    }

    public static Variable variable(String name) {
        Variable v = new Variable();
        v.name = name;
        return v;
    }

    public static class Rule {
        Expr left, right;
        String type;
        boolean always;
        boolean global;
        int depth;

        public String toString() {
            return left + " " + type + " " + right;
        }

        public boolean isComplete() {
            return left != null && right != null && type != null && left.isComplete() && right.isComplete();
        }

        public Rule reverse() {
            Rule result = new Rule();
            result.left = left;
            result.right = right;
            switch (type) {
            case "=":
                result.type = "<>";
                break;
            case "<":
                result.type = ">=";
                break;
            case ">":
                result.type = "<=";
                break;
            case ">=":
                result.type = "<";
                break;
            case "<=":
                result.type = ">";
                break;
            case "<>":
                result.type = "=";
                break;
            default:
                throw new IllegalArgumentException();
            }
            return result;
        }
    }

    public static abstract class Expr {
        abstract Result compareTo(Expr other);
        abstract boolean isComplete();
        abstract boolean contains(Variable var);
        abstract Expr negate();
        abstract int complexity();
        abstract Expr simplify();
    }

    public static class Number extends Expr {
        long value;

        static final Number ZERO = number(0);

        public String toString() {
            return "" + value;
        }

        public boolean equals(Object other) {
            if (other instanceof Number) {
                Number o = (Number) other;
                return value == o.value;
            }
            return false;
        }

        @Override
        Result compareTo(Expr other) {
            if (other instanceof Number) {
                Number o = (Number) other;
                if (value == o.value) {
                    return Result.EQUAL;
                } else if (value > o.value) {
                    return Result.LARGER;
                } else if (value < o.value) {
                    return Result.SMALLER;
                }
                throw new IllegalArgumentException();
            }
            return Result.UNKNOWN;
        }

        @Override
        boolean contains(Variable var) {
            return false;
        }

        @Override
        Expr negate() {
            Number result = new Number();
            result.value = -value;
            return result;
        }

        @Override
        boolean isComplete() {
            return true;
        }

        @Override
        int complexity() {
            return 0;
        }

        @Override
        Expr simplify() {
            return this;
        }
    }

    public static class Variable extends Expr {
        String name;

        public String toString() {
            return name;
        }

        public boolean equals(Object other) {
            if (other instanceof Variable) {
                return ((Variable) other).name.equals(name);
            }
            return false;
        }

        @Override
        boolean contains(Variable var) {
            return var.name.equals(name);
        }

        @Override
        Result compareTo(Expr other) {
            if (other instanceof Variable) {
                Variable o = (Variable) other;
                if (name.equals(o.name)) {
                    return Result.EQUAL;
                }
                return Result.UNKNOWN;
            } else if (other instanceof Operation) {
                Operation o = (Operation) other;
                if (o.left.equals(this)) {
                    switch(o.type) {
                    case "+":
                        return Number.ZERO.compareTo(o.right);
                    case "-":
                        return o.right.compareTo(Number.ZERO);
                    default:
                        throw new IllegalArgumentException();
                    }
                } else if (o.right.equals(this)) {
                    throw new IllegalArgumentException();
                }
                return Result.UNKNOWN;
            }
            return Result.UNKNOWN;
        }

        @Override
        Expr negate() {
            return operation(number(0), "-", this);
        }

        @Override
        boolean isComplete() {
            return name != null;
        }

        @Override
        int complexity() {
            return 1;
        }

        @Override
        Expr simplify() {
            return this;
        }

    }

    public static class Operation extends Expr {
        String type;
        Expr left, right;

        public String toString() {
            return "(" + left + " " + type + " " + right + ")";
        }

        @Override
        public Expr simplify() {
            if (left.complexity() < right.complexity() && type.equals("+")) {
                return swap().simplify();
            }
            if (right instanceof Number) {
                long x = ((Number) right).value;
                if (type.equals("-")) {
                    Operation result = new Operation();
                    result.left = left;
                    result.type = "+";
                    result.right = number(-x);
                    return result.simplify();
                }
            }
            if (left instanceof Number && right instanceof Number) {
                long l = ((Number) left).value;
                long r = ((Number) right).value;
                switch(type) {
                case "+":
                    return number(l + r);
                case "-":
                    return number(l - r);
                default:
                    throw new IllegalArgumentException();
                }
            }
            Expr l = left.simplify();
            Expr r = right.simplify();
            // (p + 1) + 1 ==> p + 2
            if (l instanceof Operation) {
                Operation l2 = (Operation) l;
                Expr b = l2.right;
                if (b instanceof Number && r instanceof Number) {
                    long x = 0;
                    switch(l2.type) {
                    case "+":
                        x = ((Number) b).value;
                        break;
                    case "-":
                        x = -((Number) b).value;
                        break;
                    default:
                        throw new IllegalArgumentException();
                    }
                    switch(type) {
                    case "+":
                        x += ((Number) r).value;
                        break;
                    case "-":
                        x -= -((Number) r).value;
                        break;
                    default:
                        throw new IllegalArgumentException();
                    }
                    Operation result = new Operation();
                    result.left = l2.left;
                    result.type = "+";
                    result.right = number(x);
                    return result;
                }
            }
            return this;
        }

        public Operation swap() {
            Operation result = new Operation();
            switch(type) {
            case "+":
                result.left = right;
                result.right = left;
                result.type = type;
                break;
            case "-":
                // 5 - x => -x + 5
                result.left = right.negate();
                result.right = left;
                result.type = "+";
                break;
            default:
                throw new IllegalArgumentException();
            }
            return result;
        }

        public boolean equals(Object other) {
            if (other instanceof Operation) {
                Operation o = (Operation) other;
                return type.equals(o.type) && left.equals(o.left) && right.equals(o.right);
            }
            return false;
        }

        @Override
        Result compareTo(Expr other) {
            if (other instanceof Operation) {
                Operation o = (Operation) other;
                if (left.equals(o.left) && type.equals(o.type)) {
                    return right.compareTo(o.right);
                }
            }
            return Result.UNKNOWN;
        }

        @Override
        boolean contains(Variable var) {
            return left.contains(var) || right.contains(var);
        }

        @Override
        Expr negate() {
            return operation(number(0), "-", this);
        }

        @Override
        boolean isComplete() {
            return left != null && right != null && type != null && left.isComplete() && right.isComplete();
        }

        @Override
        int complexity() {
            return 2;
        }

    }

    public static enum Result {
        LARGER,
        EQUAL,
        SMALLER,
        UNKNOWN
    }

}
