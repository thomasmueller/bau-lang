package org.bau.parser;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.bau.runtime.Memory;
import org.bau.runtime.Value;
import org.bau.runtime.Value.ValueI8Array;

public class StringLiteral implements Expression {

    String value;
    ValueI8Array array;
    DataType type;
    long reference;
    private boolean used;

    public StringLiteral(String value, DataType type, Program program) {
        this.value = value;
        this.type = type;
        this.reference = program.addStringConstant(value, this);
        byte[] data = value.getBytes(StandardCharsets.UTF_8);
        this.array = new ValueI8Array(data);
    }

    @Override
    public Value eval(Memory memory) {
        if (memory == null) {
            return null;
        }
        long heapId = memory.putHeap(array, true);
        return new Value.ValueRef(heapId);
    }

    @Override
    public DataType type() {
        return type;
    }

    public DataType canThrowException() {
        return null;
    }

    public String toC() {
        return "string_" + reference;
    }

    @Override
    public void setOwnedBoundsToNull(Expression scope) {
    }

    public Expression replace(Variable old, Expression with) {
        return this;
    }

    public static String escape(String s) {
        byte[] utf8 = s.getBytes(StandardCharsets.UTF_8);
        StringBuilder buff = new StringBuilder();
        for(byte b : utf8) {
            int c = b & 0xff;
            if (c < ' ' || c >= 127) {
                if (c == '\n') {
                    buff.append("\\n");
                } else if (c == '\t') {
                    buff.append("\\t");
                } else {
                    buff.append(String.format("\\x%02x", (int) c));
                }
            } else if (c == '\"') {
                buff.append("\\\"");
            } else if (c == '\'') {
                buff.append("\\'");
            } else if (c == '\\') {
                buff.append("\\\\");
            } else {
                buff.append((char) c);
            }
        }
        return buff.toString();
    }

    public String toString() {
        StringBuilder buff = new StringBuilder();
        buff.append('\'');
        buff.append(escape(value));
        buff.append('\'');
        return buff.toString();
    }

    @Override
    public boolean isEasyToRead() {
        return true;
    }

    @Override
    public Bounds getBounds() {
        return null;
    }

    @Override
    public Expression simplify() {
        return this;
    }

    @Override
    public boolean isSimple() {
        return true;
    }

    @Override
    public Expression writeStatements(Parser parser, boolean assignment, ArrayList<Statement> target) {
        return this;
    }

    public static String unindentRawMultiLineString(String text) {
        if (text.isEmpty() || text.charAt(0) != '\n') {
            return text;
        }
        int indent = 0;
        while (text.charAt(text.length() - indent - 1) != '\n') {
            indent++;
        }
        StringBuilder buff = new StringBuilder();
        int begin = 1;
        int start = 1;
        for (int i = 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                if (buff.length() > 0) {
                    buff.append('\n');
                }
                buff.append(text.substring(start, i));
                begin = i + 1;
                start = begin;
            } else if (c == ' ' && i - begin < indent) {
                start++;
            }
        }
        return buff.toString();
    }

    @Override
    public void used(Program program) {
        used = true;
        type.used(program);
    }

    public boolean isUsed() {
        return used;
    }

}
