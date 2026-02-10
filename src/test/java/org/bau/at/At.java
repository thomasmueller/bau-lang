package org.bau.at;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class At {

    private final HashMap<String, Value> global = new HashMap<>();
    private final HashMap<String, Expr> methods = new HashMap<>();
    private final ArrayList<HashMap<String, Value>> stack = new ArrayList<>();
    private StringBuilder out = new StringBuilder();
    private HashMap<String, Value> local = new HashMap<>();
    private String code;
    private String token;
    private int pos;
    private Value returnValue;
    private long counter;

    public String run(String programCode) {
        ArrayList<Expr> program = new ArrayList<>();
        this.code = programCode;
        pos = 0;
        counter = 100000;
        read();
        while (!"".equals(token)) {
            if (match("+", "fun")) {
                String n = token;
                methods.put(n, new Expr());
                Expr c = parseExpr();
                c.list.add(parseExpr());
                methods.put(n, c);
            } else {
                Expr expr = parseExpr();
                program.add(expr);
            }
            match(",", ";");
        }
        out = new StringBuilder();
        Value result = null;
        for (Expr e : program) {
            result = e.run(this);
        }
        if (out.isEmpty()) {
            out.append(result);
        }
        return out.toString();
    }

    private Expr parseExpr() {
        return parseExprBp(0);
    }

    private Expr parseExprBp(int min) {
        Expr prim = parsePrimary();
        while (true) {
            int power = -1;
            if ("".equals(token)) {
                return prim;
            }
            Expr op = new Expr();
            op.type = Expr.OPERATION;
            op.list.add(prim);
            op.name = token;
            int right = 1;
            switch (op.name) {
            case ":":
                if (op.list.get(0).type != Expr.VARIABLE
                && !".".equals(op.list.get(0).name)) {
                    return op.list.get(0);
                }
                power = 10;
                right = -1;
                break;
            case "&":
            case "|":
                power = 15;
                break;
            case "<":
            case ">":
            case "=":
            case "<=":
            case ">=":
            case "<>":
                power = 20;
                break;
            case "+":
            case "-":
                power = 30;
                break;
            case "*":
            case "/":
            case "%":
                power = 40;
                break;
            case ".":
                power = 50;
                break;
            }
            if (power < min) {
                return prim;
            }
            read();
            op.list.add(parseExprBp(power + right));
            prim = op;
        }
    }

    private Expr parsePrimary() {
        if (token.length() == 0) {
            Expr l = new Expr();
            l.type = Expr.LITERAL;
            l.value = Value.ZERO;
            return l;
        }
        char first = token.charAt(0);
        if (match("@", "while")) {
            Expr l = new Expr();
            l.type = Expr.LOOP;
            l.list.add(parseExpr());
            l.list.add(parseExpr());
            return l;
        } else if (match("*", "repeat")) {
            Expr l = new Expr();
            l.type = Expr.REPEAT;
            l.list.add(parseExpr());
            l.list.add(parseExpr());
            return l;
        } if (match("?", "if")) {
            Expr i = new Expr();
            i.type = Expr.IF;
            i.list.add(parseExpr());
            i.list.add(parseExpr());
            if (match(":", "else")) {
                i.list.add(parseExpr());
            }
            return i;
        } else if (match("=", "return")) {
            Expr op = new Expr();
            op.type = Expr.RETURN;
            op.list.add(parseExpr());
            return op;
        } else if (match("-", "-")) {
            Expr op = new Expr();
            op.type = Expr.OPERATION;
            Expr lit = new Expr();
            lit.type = Expr.LITERAL;
            lit.value = Value.ZERO;
            op.list.add(lit);
            op.name = "-";
            op.list.add(parsePrimary());
            return op;
        } else if (match("(", "{")) {
            Expr n = new Expr();
            n.type = Expr.BLOCK;
            while (!match(")", "}") && pos < code.length()) {
                n.list.add(parseExpr());
                match(",", ";");
            }
            return n;
        } else if (first >= '0' && first <= '9') {
            Expr l = new Expr();
            l.type = Expr.LITERAL;
            double d;
            try {
                d = Double.parseDouble(token);
            } catch (NumberFormatException e) {
                d = 0;
            }
            l.value = Value.valueOf(d);
            read();
            return l;
        } else if (first == '\'') {
            Expr l = new Expr();
            l.type = Expr.LITERAL;
            l.value = new Value();
            l.value.array = new ArrayList<>(token.length());
            for (int i = 1; i < token.length(); i++) {
                l.value.array.add((double) token.charAt(i));
            }
            l.value.array.add((double) 0);
            read();
            return l;
        } else {
            String id = token;
            if (match(">", "print")) {
                id = ">";
            } else {
                read();
            }
            if ((methods.containsKey(id) || id.equals(">")) && match("(", "(")) {
                Expr c = new Expr();
                c.type = Expr.CALL;
                c.name = id;
                while (!match(")", ")") && pos < code.length()) {
                    c.list.add(parseExpr());
                    match(",", ",");
                }
                return c;
            }
            Expr v = new Expr();
            v.type = Expr.VARIABLE;
            v.name = id;
            return v;
        }
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
            while (pos < code.length()) {
                c = code.charAt(pos);
                if ((c >= '0' && c <= '9') || c == '.') {
                    pos++;
                } else {
                    break;
                }
            }
            token = code.substring(start, pos);
        } else if (c == '\'') {
            pos++;
            StringBuilder buff = new StringBuilder();
            buff.append('\'');
            while (true) {
                if (pos >= code.length() || code.charAt(pos) == '\'') {
                    pos++;
                    if (pos >= code.length() || code.charAt(pos) != '\'') {
                        break;
                    }
                }
                buff.append(code.charAt(pos));
                pos++;
            }
            token = buff.toString();
        } else {
            pos++;
            if (pos < code.length()) {
                c = code.charAt(pos);
                if (c == '=') {
                    pos++;
                }
            }
            token = code.substring(start, pos);
        }
    }

    static class Value {
        final static Value ZERO = new Value();
        static {
            ZERO.array.add(0.0);
        }
        ArrayList<Double> array = new ArrayList<>();

        static Value valueOf(double value) {
            Value obj = new Value();
            obj.array.add(value);
            return obj;
        }

        public String toString() {
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

        public int intValue() {
            return array.get(0).intValue();
        }
    }

    static class Expr {
        final static int BLOCK = 1, CALL = 2, IF = 3, LITERAL = 4, REPEAT = 5, LOOP = 6,
                OPERATION = 7, VARIABLE = 8, RETURN = 9;

        int type;
        String name;
        Value value;
        ArrayList<Expr> list = new ArrayList<>();

        public Value run(At state) {
            Value result = Value.ZERO;
            switch (type) {
            case BLOCK:
                for (Expr e : list) {
                    result = e.run(state);
                    if (state.returnValue != null) {
                        break;
                    }
                }
                return result;
            case LITERAL:
                return value;
            case VARIABLE: {
                HashMap<String, Value> map;
                if (name.charAt(0) <= 'Z') {
                    map = state.global;
                } else {
                    map = state.local;
                }
                Value o = map.get(name);
                if (o == null) {
                    return Value.ZERO;
                }
                return o;
            }
            case IF:
                if (list.get(0).run(state).intValue() != 0) {
                    result = list.get(1).run(state);
                } else if (list.size() > 2) {
                    result = list.get(2).run(state);
                }
                return result;
            case REPEAT:
                long c = list.get(0).run(state).intValue();
                for (int i = 0; i < c; i++) {
                    state.local.put("_", Value.valueOf(i));
                    if (state.returnValue != null || state.counter-- <= 0) {
                        break;
                    }
                    result = list.get(1).run(state);
                }
                return result;
            case LOOP:
                while (list.get(0).run(state).intValue() != 0
                        && state.returnValue == null
                        && state.counter-- > 0) {
                    result = list.get(1).run(state);
                }
                return result;
            case RETURN:
                result = list.get(0).run(state);
                state.returnValue = result;
                return result;
            case CALL:
                if (name.equals(">")) {
                    for (Expr e : list) {
                        state.out.append(e.run(state));
                    }
                    state.out.append('\n');
                } else {
                    Expr m = state.methods.get(name);
                    HashMap<String, Value> vars = new HashMap<>();
                    for (int i = 0; i < m.list.size() - 1 && i < list.size(); i++) {
                        Expr v = m.list.get(i);
                        Expr a = list.get(i);
                        vars.put(v.name, a.run(state));
                    }
                    state.stack.add(state.local);
                    state.local = vars;
                    result = m.list.get(m.list.size() - 1).run(state);
                    if (state.returnValue != null) {
                        result = state.returnValue;
                        state.returnValue = null;
                    }
                    state.local = state.stack.removeLast();
                    return result;
                }
                return Value.ZERO;
            case OPERATION: {
                Expr left = list.get(0);
                Expr right = list.get(1);
                if (":".equals(name)) {
                  HashMap<String, Value> map;
                  String name = "";
                  Expr arrayIndex = null;
                  if (left.type == Expr.VARIABLE) {
                      name = left.name;
                  } else if (left.type == Expr.OPERATION && left.name.equals(".")) {
                      name = left.list.get(0).name;
                      arrayIndex = left.list.get(1);
                  }
                  if (name != null && name.length() > 0 && name.charAt(0) <= 'Z') {
                      map = state.global;
                  } else {
                      map = state.local;
                  }
                  Value o = map.get(name);
                  if (o == null) {
                      o = Value.valueOf(0.0);
                      map.put(name, o);
                  }
                  Value v = right.run(state);
                  if (arrayIndex != null) {
                      int index = arrayIndex.run(state).intValue();
                      if (index >= 0) {
                          while (o.array.size() <= index) {
                              o.array.add(0.0);
                          }
                          o.array.set(index, v.array.get(0));
                      }
                  } else {
                      o.array = new ArrayList<>(v.array);
                  }
                  return v;
              }
              Value lo = left.run(state);
              Value ro = right.run(state);
              double r = ro.array.get(0);
              if (".".equals(name)) {
                  List<Double> array = lo.array;
                  if (r < 0 || r >= array.size()) {
                      return Value.ZERO;
                  }
                  return Value.valueOf(array.get((int) r));
              }
              double l = lo.array.get(0);
              double res;
              switch (name) {
              case "+":
                  res = l + r;
                  break;
              case "-":
                  res = l - r;
                  break;
              case "*":
                  res = l * r;
                  break;
              case "/":
                  res = r == 0 ? 0 : l / r;
                  break;
              case "%":
                  res = r == 0 ? 0 : l % r;
                  break;
              case "=":
                  res = (l == r) ? 1 : 0;
                  break;
              case "<>":
                  res = (l != r) ? 1 : 0;
                  break;
              case ">":
                  res = (l > r) ? 1 : 0;
                  break;
              case "<":
                  res = (l < r) ? 1 : 0;
                  break;
              case ">=":
                  res = (l >= r) ? 1 : 0;
                  break;
              case "<=":
                  res = (l <= r) ? 1 : 0;
                  break;
              case "|":
                  res = (long) l | (long) r;
                  break;
              case "&":
                  res = (long) l & (long) r;
                  break;
              default:
                  res = 0;
              }
              return Value.valueOf(res);
            }
            }
            return result;
        }
    }

}
