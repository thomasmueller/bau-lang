package org.bau.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import org.bau.runtime.Value;
import org.bau.runtime.Value.ValueNull;

public class DataType {

    public static final String I8 = "i8";
    public static final String I16 = "i16";
    public static final String I32 = "i32";
    public static final String INT = "int";
    public static final String F32 = "f32";
    public static final String FLOAT = "float";
    public static final String TYPE = "type";

    // we only define INT_TYPE because the types have a "used" flag -
    // and we don't want to set it if not needed
    // TODO this means for int array, this is always set
    public static final DataType INT_TYPE = new DataType(null, DataType.INT, 8, true, Collections.emptyList());
    static {
        INT_TYPE.used();
        INT_TYPE.arrayType.used();
    }

    final String module;
    private final String name;
    private final int sizeOf;
    private final boolean isSystem;
    private final boolean isArray;
    final boolean isFloatingPoint;
    private final DataType arrayType;
    private final DataType nullableType;
    public final List<Variable> fields;
    public LinkedHashMap<String, Long> enumValues;
    private DataType baseType;
    FunctionDefinition autoClose;
    final boolean valueType;
    private boolean used;
    Expression maxValue;
    private boolean isNullable;
    public ArrayList<String> parameters;
    public String template;

    public DataType(String module, String name, int sizeOf, boolean isSystem, List<Variable> fields) {
        this(module, name, sizeOf, isSystem, false, fields, false);
    }

    public static boolean isGenericTypeName(String token) {
        return token != null && !token.isEmpty() &&
                token.charAt(0) >= 'A' && token.charAt(0) <= 'Z' &&
                token.toUpperCase().equals(token);
    }

    void used() {
        this.used = true;
    }

    public DataType(String module, String name, int sizeOf, boolean isSystem, boolean isArray, List<Variable> fields, boolean isNullable) {
        this.isNullable = isNullable;
        this.module = module;
        this.name = name;
        this.sizeOf = sizeOf;
        this.isSystem = isSystem;
        this.isArray = isArray;
        this.fields = fields;
        if (!isArray) {
            arrayType = new DataType(module, name + "[]", sizeOf, false, true, fields, isNullable);
            arrayType.baseType = this;
        } else {
            arrayType = this;
            if (!name.endsWith("[]")) {
                throw new IllegalStateException();
            }
        }
        // lowercase is value type
        valueType = name.startsWith("0..") || name.charAt(0) > 'Z';
        if (isSystem) {
            isFloatingPoint = name.charAt(0) == 'f';
        } else {
            isFloatingPoint = false;
        }
        baseType = this;
        if (!isArray && !valueType && !isSystem && !isNullable) {
            nullableType = new DataType(module, name, sizeOf, false, false, fields, true);
        } else {
            nullableType = null;
        }
    }

    public boolean isSystem() {
        return isSystem;
    }

    public String fullName() {
        return fullName(module, name);
    }

    public String name() {
        return name;
    }

    public int sizeOf() {
        return sizeOf;
    }

    public DataType baseType() {
        return baseType;
    }

    public DataType arrayType() {
        if (isArray) {
            throw new IllegalStateException();
        }
        return arrayType;
    }

    public String toString() {
        StringBuilder buff = new StringBuilder();
        buff.append(name);
        if (parameters != null) {
            buff.append('(');
            int i = 0;
            for (String p : parameters) {
                if (i > 0) {
                    buff.append(", ");
                }
                i++;
                buff.append(p);
            }
            buff.append(')');
        }
        if (isNullable) {
            buff.append("?");
        }
        return buff.toString();
    }

    public String nameC() {
        String s;
        if ("i8".equals(name)) {
            s = "char";
        } else if ("i16".equals(name)) {
            s = "int16_t";
        } else if ("i32".equals(name)) {
            s = "int32_t";
        } else if ("int".equals(name)) {
            s = "int64_t";
        } else if ("f32".equals(name)) {
            s = "float";
        } else if ("float".equals(name)) {
            s = "double";
        } else if (name.startsWith("0..")) {
            s = "int64_t";
        } else if (enumValues != null) {
            s = "int64_t";
        } else {
            if (module != null) {
                s = module.replace(".", "_") + "_" + name;
            } else {
                s = name;
            }
        }
        if (isArray()) {
            // replace "[]" with "_array"
            return s.substring(0, s.length() - 2) + "_array";
        }
        return s;
    }

    public String toC() {
        String s = nameC();
        return (isPointer() || isArray()) ? s + "*" : s;
    }

    public DataType getFieldDataType(String f) {
        // TODO use a map, not a loop
        for (Variable v : fields) {
            if (v.name.equals(f)) {
                return v.type;
            }
        }
        return null;
    }

    public boolean isPointer() {
        return !valueType;
    }

    public boolean isArray() {
        return isArray;
    }

    public boolean isUsed() {
        return used;
    }

    public DataType orNull() {
        return nullableType;
    }

    public boolean isNullable() {
        return isNullable;
    }

    public String getModule() {
        return module;
    }

    public static String getId(String name, ArrayList<DataType> params) {
        StringBuilder buff = new StringBuilder();
        buff.append(name);
        for (DataType t : params) {
            buff.append('_');
            String fullName = t.fullName();
            buff.append(fullName.replace('.', '_').replace("[]", "_array"));
        }
        return buff.toString();
    }

    public static String fullName(String module, String name) {
        if (module == null || name.indexOf('.') > 0) {
            return name;
        }
        return module + "." + name;
    }

    public DataType resolveEnumType() {
        if (enumValues != null) {
            return DataType.INT_TYPE;
        }
        return this;
    }

    public Value getZeroValue() {
        switch(name) {
        case I8:
            return new Value.ValueI8((byte) 0);
        case I16:
            return new Value.ValueI16((short) 0);
        case I32:
            return new Value.ValueI32(0);
        case INT:
            return new Value.ValueInt(0);
        case F32:
            return new Value.ValueFloat(0);
        case FLOAT:
            return new Value.ValueFloat(0);
        }
        return ValueNull.INSTANCE;
    }

}
