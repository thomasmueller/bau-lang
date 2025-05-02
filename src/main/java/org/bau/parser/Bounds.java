package org.bau.parser;

import java.util.ArrayList;

import org.bau.runtime.Value;

public class Bounds {
    ArrayList<Entry> list = new ArrayList<>();
    long offset;

    public Bounds() {

    }

    public Bounds(NumberValue value) {
        Entry e = newEntry(null, "=", value);
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

    public static Entry newEntry(Expression scope, String operation, Expression expr) {
        Entry e = new Entry();

        e.scope = scope;
        e.operation = operation;
        e.expr = expr;

        String var = getVariable(expr);
        long offset = getOffset(expr);
        if (">".equals(operation)) {
            e.minVariable = var;
            e.minOffset = offset + 1;
            e.maxVariable = "";
            e.maxExcludingOffset = Long.MAX_VALUE;
        } else if (">=".equals(operation)) {
            e.minVariable = var;
            e.minOffset = offset;
            e.maxVariable = "";
            e.maxExcludingOffset = Long.MAX_VALUE;
        } else if ("=".equals(operation)) {
            e.minVariable = var;
            e.minOffset = offset;
            e.maxVariable = var;
            e.maxExcludingOffset = offset + 1;
        } else if ("<".equals(operation)) {
            e.minVariable = "";
            e.minOffset = Long.MIN_VALUE;
            e.maxVariable = var;
            e.maxExcludingOffset = offset;
        } else if ("<=".equals(operation)) {
            e.minVariable = "";
            e.minOffset = Long.MIN_VALUE;
            e.maxVariable = var;
            e.maxExcludingOffset = offset + 1;
        } else if ("<>".equals(operation)) {
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
        if (operation == null && expr == null) {
            // this means type is "undo"
            return;
        }
        Entry e = newEntry(scope, operation, expr);
        e.condition = true;
        list.add(e);
    }

    public void setBoundValue(Expression scope, String modify, Expression value) {
        if (!list.isEmpty() && list.get(list.size() - 1).scope == scope) {
            list.remove(list.size() - 1);
        }
        Entry e = newEntry(scope, modify, value);
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
     * @return -1 smaller than max, 0 equal, 1 larger than max, Integer.MAX_VALUE unknown.
     */
    public CompareResult compareTo(Parser p, Expression max) {
        for (int i = list.size() - 1; i >= 0; i--) {
            Entry e = list.get(i);
            if (e.operation.equals("<>")) {
                continue;
            }
            if (inScope(p.getBlockConditions(), e.scope)) {
                String maxVar = getVariable(max);
                long maxOffset = getOffset(max) - offset;
                if (maxVar.equals(e.minVariable) && maxOffset < e.minOffset) {
                    return CompareResult.LARGER;
                } else if (maxVar.equals(e.maxVariable) && maxOffset >= e.maxExcludingOffset) {
                    return CompareResult.SMALLER;
                } else if (maxVar.equals(e.minVariable) && maxOffset == e.minOffset
                        && maxVar.equals(e.maxVariable) && maxOffset == e.maxExcludingOffset) {
                    return CompareResult.EQUAL;
                }
            }
        }
        return CompareResult.UNKNOWN;
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
                    && e.minOffset == e.maxExcludingOffset - 1) {
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

    public boolean largerThan(Expression scope, long value) {
        for (Entry e : list) {
            if (e.scope == scope
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
        // in which part of the code this condition is valid,
        // null = always; or the condition of the if / elif / while statement
        Expression scope;

        public boolean condition;

        String operation;
        Expression expr;

        String minVariable;
        long minOffset;
        String maxVariable;
        long maxExcludingOffset;
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
            if (!operation.equals("<>")) {
                buff.append(", bounds ");
                if (minVariable.isEmpty()) {
                    if (minOffset != Long.MIN_VALUE) {
                        buff.append(minOffset);
                    }
                } else {
                    buff.append(minVariable);
                    if (minOffset == 0) {
                        // just the variable
                    } else if (minOffset != Long.MIN_VALUE) {
                    } else if (minOffset < 0) {
                        buff.append(minOffset);
                    } else {
                        buff.append("+" + minOffset);
                    }
                }
                buff.append("..");
                if (maxVariable.isEmpty()) {
                    if (maxExcludingOffset != Long.MAX_VALUE) {
                        buff.append(maxExcludingOffset);
                    }
                } else {
                    buff.append(maxVariable);
                    if (maxExcludingOffset == 0) {
                        // just the variable
                    } else if (maxExcludingOffset == Long.MAX_VALUE) {
                        // no start
                    } else if (maxExcludingOffset < 0) {
                        buff.append(maxExcludingOffset);
                    } else {
                        buff.append("+" + maxExcludingOffset);
                    }
                }
            }
            buff.append(" (" );
            buff.append("operation '" + operation);
            buff.append("', expr '" + expr + "')");
            return buff.toString();
        }
    }

    enum ApplyType {
        // positive condition, e.g. "if d.len > 0"
        POSITIVE,
        // negative condition, e.g. "else" of the above
        NEGATIVE,
        // undo a condition, e.g. after the "if" or "else" ended
        UNDO
    }

    enum CompareResult {
        SMALLER, EQUAL, LARGER, UNKNOWN
    }

}
