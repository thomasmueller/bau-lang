package org.bau.parser.stmt;

import java.util.ArrayList;
import java.util.HashSet;

import org.bau.parser.DataType;
import org.bau.parser.FunctionContext;
import org.bau.parser.MemoryType;
import org.bau.parser.Program;
import org.bau.parser.ProgramContext;
import org.bau.parser.Section;
import org.bau.parser.expr.Expression;
import org.bau.parser.expr.Variable;
import org.bau.runtime.Memory;

public class Comment implements Statement, Section {

    private ArrayList<String> list = new ArrayList<>();

    public Comment(String text) {
        if (text != null) {
            list.add(text);
        }
    }

    public void add(String additional) {
        if (additional == null) {
            System.out.println("??");
        }
        list.add(additional);
    }

    public String getText() {
        StringBuilder buff = new StringBuilder();
        for(String c : list) {
            if (buff.length() > 0) {
                buff.append("\n");
            }
            buff.append(trim(c));
        }
        return buff.toString();
    }

    private static String trim(String comment) {
        int i = 0;
        for (; i < comment.length(); i++) {
            if (comment.charAt(i) != '#') {
                break;
            }
        }
        String raw = comment.trim();
        if (i > 1) {
            String prefix = comment.substring(0, i);
            if (comment.trim().endsWith(prefix)) {
                raw = raw.substring(i, raw.length() - i);
            }
        }
        return raw;
    }

    @Override
    public Statement replace(Variable old, Expression with) {
        return this;
    }

    @Override
    public StatementResult run(Memory m) {
        return StatementResult.OK;
    }

    @Override
    public void optimize(ProgramContext context) {
    }

    @Override
    public String toC() {
        String text = getText();
        if (text.indexOf("*/") >= 0) {
            text = text.replace("*/", "* /");
        }
        return "/* " + text + " */";
    }

    @Override
    public String format() {
        return formatSource();
    }

    @Override
    public String formatSource() {
        StringBuilder buff = new StringBuilder();
        for (String c : list) {
            buff.append(c);
            buff.append("\n");
        }
        return buff.toString();
    }

    @Override
    public void collectTypes(HashSet<DataType> set, MemoryType memoryType) {
    }

    @Override
    public void used(Program program) {
    }

    @Override
    public DataType canThrowException() {
        return null;
    }

    @Override
    public void setVariableVersions(String name, int oldVersion, int newVersion) {
    }

    @Override
    public void resolveTypesForStatement(FunctionContext context) {
    }

    public void add(Comment comment) {
        list.addAll(comment.list);
    }

}
