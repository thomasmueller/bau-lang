package org.bau.parser;

import java.util.ArrayList;
import java.util.List;

import org.bau.runtime.Memory;

public class While implements Statement {
    int continueId;
    ArrayList<Statement> list = new ArrayList<>();
    ArrayList<Statement> listContinue = new ArrayList<>();
    List<Statement> autoClose;
    Expression condition;
    
    @Override
    public Statement replace(Variable old, Expression with) {
        While c = new While();
        c.condition = condition.replace(old, with);
        c.list = new ArrayList<Statement>();
        for (Statement s : list) {
            c.list.add(s.replace(old, with));
        }
        return c;
    }
    
    @Override
    public StatementResult run(Memory m) {
        // TODO this is likely quite slow
        ArrayList<Statement> l2 = new ArrayList<>();
        l2.addAll(list);
        int continuePoint = l2.size();
        l2.addAll(listContinue);
        if (autoClose != null) {
            l2.addAll(autoClose);
        }
        outer:
        while (true) {
            long v = condition.eval(m).longValue();
            if (v != 1) {
                break;
            }
            for (int i = 0; i < l2.size(); i++) {
                Statement s = l2.get(i);
                StatementResult n = s.run(m);
                if (m.tick()) {
                    return StatementResult.TIMEOUT;
                }
                if (n == StatementResult.OK) {
                    // ok
                } else if (n == StatementResult.BREAK) {
                    break outer;
                } else if (n == StatementResult.CONTINUE) {
                    i = continuePoint - 1;
                } else if (n == StatementResult.RETURN) {
                    return n;
                } else if (n == StatementResult.THROW) {
                    for (; i < list.size(); i++) {
                        s = list.get(i);
                        if (s instanceof Catch) {
                            i--;
                            break;
                        }
                    }
                    if (i == list.size()) {
                        return n;
                    }
                } else if (n == StatementResult.PANIC) {
                    return n;
                }
            }
        }
        return StatementResult.OK;
    }

    public String toC(ProgramContext context) {
        StringBuilder buff = new StringBuilder();
        buff.append("while (" + condition.toC() + ") {\n");
        for(Statement s : list) {
            buff.append(Statement.indent(s.toC(context)));
        }
        StringBuilder buffContinue = new StringBuilder();
        for(Statement s : listContinue) {
            buffContinue.append(Statement.indent(s.toC(context)));
        }
        if (autoClose != null) {
            for(Statement s : autoClose) {
                buffContinue.append(Statement.indent(s.toC(context)));
            }
        }
        if (buffContinue.length() > 0) {
            buff.append(Statement.indent("continue" + continueId + ":;\n"));
            buff.append(buffContinue);
        }
        buff.append("}\n");
        return buff.toString();
    }

    public String toString() {
        // TODO this is not correct if continue is used
        StringBuilder buff = new StringBuilder();
        buff.append("while " + condition + "\n");
        for(Statement s : list) {
            buff.append(Statement.indent(s.toString()));
        }
        for(Statement s : listContinue) {
            buff.append(Statement.indent(s.toString()));
        }        
        return buff.toString();
    }

    public void autoClose(List<Statement> autoClose) {
        this.autoClose = autoClose;
    }

}
