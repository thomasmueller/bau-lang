package org.bau.at;

import java.util.ArrayList;
import java.util.HashMap;

public class At {

    private final HashMap<String, ArrayList<Double>> global = new HashMap<>();
    private final HashMap<String, Expr> functions = new HashMap<>();
    private final ArrayList<HashMap<String, ArrayList<Double>>> stack = new ArrayList<>();
    private StringBuilder out = new StringBuilder();
    private HashMap<String, ArrayList<Double>> local = new HashMap<>();
    private String code;
    private String token;
    private ArrayList<Double> value;
    private int pos;
    private ArrayList<Double> returnValue;
    private long counter;
    private boolean inOperator;

    public String run(String programCode) {
        ArrayList<Expr> program = new ArrayList<>();
        code = programCode;
        pos = 0;
        counter = 100000;
        read();
        while (!"".equals(token)) {
            if (match("+", "fun")) {
                String n = token;
                inOperator = true;
                functions.put(n, new Expr(Expr.BLOCK, null, null));
                Expr c = parseExpr(0);
                c.list.add(parseExpr(0));
                inOperator = false;
                functions.put(n, c);
            } else {
                program.add(parseExpr(0));
            }
            match(",", ";");
        }
        out = new StringBuilder();
        ArrayList<Double> result = null;
        for (Expr e : program) {
            result = run(e);
        }
        if (out.isEmpty() && result != null) {
            out.append(convertToString(result));
        }
        return out.toString();
    }

    private Expr parseExpr(int min) {
        Expr prim = parsePrimary();
        while (true) {
            int power = -1;
            if ("".equals(token)) {
                return prim;
            }
            Expr op = new Expr(Expr.OPERATION, token, null);
            op.list.add(prim);
            int right = 1;
            if (":".equals(token)) {
                if (op.list.get(0).type != Expr.VARIABLE && !".".equals(op.list.get(0).name)) {
                    return prim;
                }
                power = 10;
                right = -1;
            } else if (".".equals(token)) {
                power = 60;
            } else if (token.charAt(0) < 'a' || token.charAt(0) > 'z') {
                power = (int) operator(token, true, 0, 0);
            }
            if (power < min) {
                return prim;
            }
            read();
            op.list.add(parseExpr(power + right));
            prim = op;
        }
    }

    private Expr parsePrimary() {
        ArrayList<Double> v = value;
        if (token.length() == 0) {
            return new Expr(Expr.LITERAL, null, valueOf(0));
        } else if (match("@", "while")) {
            Expr e = new Expr(Expr.LOOP, null, null);
            e.list.add(parseExpr(0));
            e.list.add(parseExpr(0));
            return e;
        } else if (match("*", "repeat")) {
            Expr e = new Expr(Expr.REPEAT, null, null);
            e.list.add(parseExpr(0));
            e.list.add(parseExpr(0));
            return e;
        } else if (match("?", "if")) {
            Expr e = new Expr(Expr.IF, null, null);
            e.list.add(parseExpr(0));
            e.list.add(parseExpr(0));
            if (match(":", "else")) {
                e.list.add(parseExpr(0));
            }
            return e;
        } else if (match("=", "return")) {
            Expr e = new Expr(Expr.RETURN, null, null);
            e.list.add(parseExpr(0));
            return e;
        } else if (match("-", "-")) {
            Expr e = new Expr(Expr.OPERATION, "-", null);
            e.list.add(new Expr(Expr.LITERAL, null, valueOf(0)));
            e.list.add(parsePrimary());
            return e;
        } else if (match("(", "(")) {
            Expr e = new Expr(Expr.LIST, null, null);
            while (!match(")", ")") && pos < code.length()) {
                e.list.add(parseExpr(0));
                match(",", ",");
            }
            return e;
        } else if (match("{", "{")) {
            Expr e = new Expr(Expr.BLOCK, null, null);
            while (!match("}", "}") && pos < code.length()) {
                e.list.add(parseExpr(0));
                match(";", ";");
            }
            return e;
        } else if (match("0", "\'")) {
            return new Expr(Expr.LITERAL, null, v);
        }
        String id = token;
        if (match(">", "print")) {
            id = ">";
        } else {
            read();
        }
        if ((functions.containsKey(id) || id.equals(">")) && match("(", "(")) {
            Expr e = new Expr(Expr.CALL, id, null);
            while (!match(")", ")") && pos < code.length()) {
                e.list.add(parseExpr(0));
                match(",", ",");
            }
            return e;
        }
        return new Expr(Expr.VARIABLE, id, null);
    }

    private boolean match(String a, String b) {
        if (a.equals(token) || b.equals(token)) {
            read();
            return true;
        }
        return false;
    }

    private void read() {
        token = "";
        while (true) {
            if (pos >= code.length()) {
                return;
            }
            char c = code.charAt(pos);
            if (c == ' ' || c == '\n') {
                pos++;
            } else {
                break;
            }
        }
        int start = pos;
        char c = code.charAt(pos);
        if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_') {
            pos++;
            while (pos < code.length()) {
                c = code.charAt(pos);
                if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || (c >= '0' && c <= '9')) {
                    pos++;
                } else {
                    break;
                }
            }
            token = code.substring(start, pos);
        } else if (c >= '0' && c <= '9') {
            pos++;
            double v = c - '0';
            double div = 0;
            while (pos < code.length()) {
                c = code.charAt(pos);
                if ((c >= '0' && c <= '9')) {
                    v = v * 10 + (c - '0');
                    div *= 10;
                } else if (c == '.' && div == 0) {
                    div = 1;
                } else {
                    break;
                }
                pos++;
            }
            if (div > 0) {
                v /= div;
            }
            value = valueOf(v);
            token = "0";
        } else if (c == '\'') {
            pos++;
            value = new ArrayList<>(token.length());
            while (true) {
                if (pos >= code.length() || code.charAt(pos) == '\'') {
                    pos++;
                    if (pos >= code.length() || code.charAt(pos) != '\'') {
                        break;
                    }
                }
                value.add((double) code.charAt(pos));
                pos++;
            }
            value.add((double) 0);
            token = "\'";
        } else {
            char first = c;
            pos++;
            if (pos < code.length()) {
                c = code.charAt(pos);
                if ((first == '<' && (c == '<' || c == '=' || c == '>'))
                        || (first == '*' && c == '*')
                        || (first == '>' && (c == '=' || c == '>'))) {
                    pos++;
                }
            }
            token = code.substring(start, pos);
        }
    }

    private static ArrayList<Double> valueOf(double value) {
        ArrayList<Double> array = new ArrayList<>(1);
        array.add(value);
        return array;
    }

    private static String convertToString(ArrayList<Double> array) {
        if (array.size() == 1) {
            double x = array.get(0);
            if (x == (long) x) {
                return Long.toString((long) x);
            }
            return Double.toString(x);
        }
        StringBuilder buff = new StringBuilder();
        for (double d : array) {
            if (d > 0) {
                buff.append((char) d);
            }
        }
        return buff.toString();
    }

    static class Expr {
        final static int BLOCK = 1, CALL = 2, IF = 3, LITERAL = 4, REPEAT = 5, LOOP = 6, OPERATION = 7, VARIABLE = 8,
                RETURN = 9, LIST = 10;

        int type;
        String name;
        ArrayList<Double> value;
        ArrayList<Expr> list = new ArrayList<>();

        public Expr(int type, String name, ArrayList<Double> value) {
            this.type = type;
            this.name = name;
            this.value = value;
        }
    }

    public ArrayList<Double> run(Expr expr) {
        ArrayList<Double> result = valueOf(0);
        if (expr.type == Expr.LITERAL) {
            result = expr.value;
        } else if (expr.type == Expr.VARIABLE) {
            HashMap<String, ArrayList<Double>> map = global;
            if (expr.name.charAt(0) > 'Z') {
                map = local;
            }
            result = map.get(expr.name);
            if (result == null) {
                result = valueOf(0);
            }
        } else if (expr.type == Expr.IF) {
            if (run(expr.list.get(0)).get(0).intValue() != 0) {
                result = run(expr.list.get(1));
            } else if (expr.list.size() > 2) {
                result = run(expr.list.get(2));
            }
        } else if (expr.type == Expr.REPEAT) {
            long c = run(expr.list.get(0)).get(0).longValue();
            for (int i = 0; i < c && returnValue == null && counter-- > 0; i++) {
                local.put("_", valueOf(i));
                result = run(expr.list.get(1));
            }
        } else if (expr.type == Expr.LOOP) {
            while (run(expr.list.get(0)).get(0).longValue() != 0 && returnValue == null && counter-- > 0) {
                result = run(expr.list.get(1));
            }
        } else if (expr.type == Expr.RETURN) {
            result = run(expr.list.get(0));
            returnValue = result;
        } else if (expr.type == Expr.CALL) {
            if (expr.name.equals(">")) {
                for (Expr e : expr.list) {
                    out.append(convertToString(run(e)));
                }
                out.append('\n');
            } else {
                result = call(expr.name, expr.list);
            }
        } else if (expr.type == Expr.BLOCK) {
            for (Expr e : expr.list) {
                result = run(e);
                if (returnValue != null) {
                    break;
                }
            }
        } else if (expr.type == Expr.LIST) {
            result = new ArrayList<>();
            for (Expr e : expr.list) {
                result.addAll(run(e));
                if (returnValue != null) {
                    break;
                }
            }
            if (result.size() == 0) {
                result.add(0.0);
            }
        } else if (expr.type == Expr.OPERATION) {
            Expr left = expr.list.get(0);
            Expr right = expr.list.get(1);
            if (":".equals(expr.name)) {
                String name = "";
                Expr arrayIndex = null;
                if (left.type == Expr.VARIABLE) {
                    name = left.name;
                } else if (left.type == Expr.OPERATION && left.name.equals(".")) {
                    name = left.list.get(0).name;
                    arrayIndex = left.list.get(1);
                }
                HashMap<String, ArrayList<Double>> map = global;
                if (name != null && name.length() > 0 && name.charAt(0) > 'Z') {
                    map = local;
                }
                ArrayList<Double> v = run(right);
                if (arrayIndex != null) {
                    ArrayList<Double> o = map.get(name);
                    if (o == null) {
                        o = valueOf(0.0);
                        map.put(name, o);
                    }
                    int index = run(arrayIndex).get(0).intValue();
                    if (index >= 0) {
                        while (o.size() <= index) {
                            o.add(0.0);
                        }
                        o.set(index, v.get(0));
                    }
                } else {
                    map.put(name, v);
                }
                return v;
            }
            ArrayList<Double> lo = run(left);
            double r = run(right).get(0);
            if (".".equals(expr.name)) {
                if (r < 0 || r >= lo.size()) {
                    if (r == -1) {
                        return valueOf(lo.size());
                    }
                    return valueOf(0);
                }
                return valueOf(lo.get((int) r));
            }
            result = valueOf(operator(expr.name, false, lo.get(0), r));
        }
        return result;
    }

    private ArrayList<Double> call(String name, ArrayList<Expr> list) {
        Expr m = functions.get(name);
        HashMap<String, ArrayList<Double>> newLocal = new HashMap<>();
        for (int i = 0; i < m.list.size() - 1 && i < list.size(); i++) {
            newLocal.put(m.list.get(i).name, run(list.get(i)));
        }
        stack.add(local);
        local = newLocal;
        ArrayList<Double> result = run(m.list.get(m.list.size() - 1));
        if (returnValue != null) {
            result = returnValue;
            returnValue = null;
        }
        local = stack.removeLast();
        return result;
    }

    private double operator(String op, boolean precedence, double l, double r) {
        if (functions.containsKey(op) && !inOperator) {
            if (precedence) {
                return 40;
            }
            inOperator = true;
            ArrayList<Expr> list = new ArrayList<>();
            list.add(new Expr(Expr.LITERAL, null, valueOf(l)));
            list.add(new Expr(Expr.LITERAL, null, valueOf(r)));
            double result = call(op, list).get(0);
            inOperator = false;
            return result;
        }
        double res = 0;
        long power = -1;
        if ("&".equals(op)) {
            res = (long) l & (long) r;
            power = 20;
        } else if ("|".equals(op)) {
            res = (long) l | (long) r;
            power = 20;
        } else if ("=".equals(op)) {
            res = (l == r) ? 1 : 0;
            power = 30;
        } else if ("<>".equals(op)) {
            res = (l != r) ? 1 : 0;
            power = 30;
        } else if (">".equals(op)) {
            res = (l > r) ? 1 : 0;
            power = 30;
        } else if ("<".equals(op)) {
            res = (l < r) ? 1 : 0;
            power = 30;
        } else if (">=".equals(op)) {
            res = (l >= r) ? 1 : 0;
            power = 30;
        } else if ("<=".equals(op)) {
            res = (l <= r) ? 1 : 0;
            power = 30;
        } else if ("+".equals(op)) {
            res = l + r;
            power = 40;
        } else if ("-".equals(op)) {
            res = l - r;
            power = 40;
        } else if ("*".equals(op)) {
            res = l * r;
            power = 50;
        } else if ("/".equals(op)) {
            res = l / r;
            power = 50;
        } else if ("%".equals(op)) {
            res = l % r;
            power = 50;
        }
        if (precedence) {
            return power;
        }
        return res;
    }

}
