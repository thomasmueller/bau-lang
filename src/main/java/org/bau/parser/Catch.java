package org.bau.parser;

import java.util.ArrayList;
import java.util.List;

import org.bau.runtime.Memory;

public class Catch implements Statement {
    ArrayList<Statement> list = new ArrayList<>();
    Variable var;
    List<Statement> autoClose;
    
    @Override
    public Statement replace(Variable old, Expression with) {
        Catch c = new Catch();
        c.var = (Variable) var.replace(old, with);
        return c;
    }
    
    public String toString() {
        StringBuilder buff = new StringBuilder();
        buff.append("catch " + var + "\n");
        for(Statement s : list) {
            buff.append(Statement.indent(s.toString()));
        }
        return buff.toString();
    }
    
    @Override
    public boolean run(Memory m) {
        // TODO Auto-generated method stub
        return false;
    }
    @Override
    public String toC(ProgramContext context) {
        StringBuilder buff = new StringBuilder();
        String nextSkipLabel = "skip" + context.nextSkipLabel++;
        String catchLabel = "catch" + context.nextCatchLabel++;
        buff.append("goto " + nextSkipLabel + ";\n");
        buff.append(catchLabel + ":;\n");
        buff.append(var.type.toC() + " " + var.name + " = _x.exception;\n");
        for(Statement s : list) {
            buff.append(Statement.indent(s.toC(context)));
        }
        if (autoClose != null) {
            for(Statement s : autoClose) {
                buff.append(Statement.indent(s.toC(context)));
            }
        }        
        context.needToCatch = null;
        buff.append(nextSkipLabel + ":;\n");
        return buff.toString();
    }

    public void autoClose(List<Statement> autoClose) {
        this.autoClose = autoClose;
    }

}
