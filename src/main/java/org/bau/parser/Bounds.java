package org.bau.parser;

import java.util.ArrayList;

import org.bau.runtime.Value;

public class Bounds {
    ArrayList<Entry> list = new ArrayList<>();
    long offset;

    public Bounds() {

    }

    public Bounds(NumberValue value) {
        Entry e = newEntry("=", value);
        list.add(e);
    }

    private static String getVariable(Expression expr) {
        String var = expr == null ? "" : expr.toString();
        Value v = expr.eval(null);
        if (v != null) {
            var = "";
        } else if (expr instanceof Operation) {
            Operation op = (Operation) expr;
            v = op.right.eval(null);
            if (v != null) {
                if (op.operator.equals("-")) {
                    var = op.left.toString();
                } else if (op.operator.equals("+")) {
                    var = op.left.toString();
                } else {
                    var = "?";
                }
            }
        } else {
            var = expr.toString();
        }
        return var;
    }

    private static long getOffset(Expression expr) {
        Value v = expr.eval(null);
        if (v != null) {
            return v.longValue();
        } else if (expr instanceof Operation) {
            Operation op = (Operation) expr;
            v = op.right.eval(null);
            if (v != null) {
                if (op.operator.equals("-")) {
                    return -v.longValue();
                } else if (op.operator.equals("+")) {
                    return v.longValue();
                }
            }
        }
        return 0;
    }

    public static Entry newEntry(String operation, Expression expr) {
        Entry e = new Entry();

        e.operation = operation;
        e.expr = expr;

        String var = getVariable(expr);
        long offset = getOffset(expr);
        if (">".equals(operation)) {
            e.minVariable = var;
            e.minOffset = offset + 1;
            e.maxVariable = "";
            e.maxOffset = Long.MAX_VALUE;
        } else if (">=".equals(operation)) {
            e.minVariable = var;
            e.minOffset = offset;
            e.maxVariable = "";
            e.maxOffset = Long.MAX_VALUE;
        } else if ("=".equals(operation)) {
            e.minVariable = var;
            e.minOffset = offset;
            e.maxVariable = var;
            e.maxOffset = offset;
        } else if ("<".equals(operation)) {
            e.minVariable = "";
            e.minOffset = Long.MIN_VALUE;
            e.maxVariable = var;
            e.maxOffset = offset - 1;
        } else if ("<=".equals(operation)) {
            e.minVariable = "";
            e.minOffset = Long.MIN_VALUE;
            e.maxVariable = var;
            e.maxOffset = offset;
        } else if ("!=".equals(operation)) {
            e.minVariable = "";
            e.maxVariable = "";
            if (expr instanceof NullValue) {
                e.notNull = true;
            }
        } else {
            throw new IllegalStateException();
        }
        return e;
    }

    public void addCondition(Expression scope, String operation, Expression expr) {
        if (!list.isEmpty() && list.get(list.size() - 1).scope == scope) {
            list.remove(list.size() - 1);
        }
        Entry e = newEntry(operation, expr);
        e.condition = true;
        e.scope = scope;
        list.add(e);
    }

    public void setBoundValue(Expression scope, String modify, Expression value) {
        if (!list.isEmpty() && list.get(list.size() - 1).scope == scope) {
            list.remove(list.size() - 1);
        }
        Entry e = newEntry(modify, value);
        e.scope = scope;
        list.add(e);
    }

    public String toString() {
        StringBuilder buff = new StringBuilder();
        if (offset != 0) {
            buff.append("offset " + offset);
        }
        buff.append("\n");
        for(Entry v : list) {
            buff.append(v).append("\n");
        }
        return buff.toString();
    }

    private boolean inScope(ArrayList<Expression> list, Expression scope) {
        if (scope == null) {
            return true;
        }
        for(Expression e : list) {
            if (e == scope) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return -1 smaller than max, 0 equal or unknown, 1 larger than max
     */
    public int compareTo(Parser p, Expression max) {
        for (Entry e : list) {
            if (inScope(p.getBlockConditions(), e.scope)) {
                String maxVar = getVariable(max);
                long maxOffset = getOffset(max) - offset;
                int result;
                if (maxVar.equals(e.minVariable) && maxOffset < e.minOffset) {
                    result = 1;
                } else if (maxVar.equals(e.maxVariable) && maxOffset > e.maxOffset) {
                    result = -1;
                } else {
                    result = 0;
                }
                return result;
            }
        }
        return 0;
    }

    /**
     * If the exact bounds are known (for example for a constant array), get the value.
     *
     * @return the exact, or null
     */
    public Value eval() {
        for (Entry e : list) {
            if (e.scope == null
                    && e.minVariable.equals("")
                    && e.maxVariable.equals("")
                    && e.minOffset == e.maxOffset) {
                return new Value.ValueInt(e.minOffset);
            }
        }
        return null;
    }

    public boolean largerThan(long value) {
        for (Entry e : list) {
            if (e.scope == null
                    && e.minVariable.equals("")
                    && e.maxVariable.equals("")
                    && e.minOffset > value) {
                return true;
            }
        }
        return false;
    }

    public boolean isNotNull(Parser p) {
        for (Entry e : list) {
            if (inScope(p.getBlockConditions(), e.scope)) {
                if (e.notNull) {
                    return true;
                }
            }
        }
        return false;
    }

    public Bounds plus(long offset) {
        Bounds b = new Bounds();
        b.list = list;
        b.offset = offset;
        return b;
    }

    static class Entry {
        // in which part of the code this condition is valid
        Expression scope;

        public boolean condition;

        String operation;
        Expression expr;

        String minVariable;
        long minOffset;
        String maxVariable;
        long maxOffset;
        boolean notNull;

        public String toString() {
            StringBuilder buff = new StringBuilder();
            buff.append("scope " + scope);
            if (condition) {
                buff.append(", condition");
            } else {
                buff.append(", update");
            }
            if (notNull) {
                buff.append(", isNotNull");
            }
            buff.append(", bounds");
            if (minVariable.isEmpty()) {
                if (minOffset != Long.MIN_VALUE) {
                    buff.append(" " + minOffset);
                }
            } else {
                buff.append(" " + minVariable);
                if (minOffset == 0) {
                    // just the variable
                } else if (minOffset != Long.MIN_VALUE) {
                } else if (minOffset < 0) {
                    buff.append(minOffset);
                } else {
                    buff.append("+" + minOffset);
                }
            }
            buff.append(" ..");
            if (maxVariable.isEmpty()) {
                if (maxOffset != Long.MAX_VALUE) {
                    buff.append(" " + maxOffset);
                }
            } else {
                buff.append(" " + maxVariable);
                if (maxOffset == 0) {
                    // just the variable
                } else if (maxOffset == Long.MAX_VALUE) {
                    // no start
                } else if (maxOffset < 0) {
                    buff.append(maxOffset);
                } else {
                    buff.append("+" + maxOffset);
                }
            }
            buff.append(" (" );
            buff.append("operation '" + operation);
            buff.append("', expr '" + expr + "')");
            return buff.toString();
        }
    }

}
