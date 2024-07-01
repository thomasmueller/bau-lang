package org.bau.parser;

import java.util.ArrayList;
import java.util.List;

public class DataType {
    
    public static final String I8 = "i8";
    public static final String I16 = "i16";
    public static final String I32 = "i32";
    public static final String INT = "int";
    public static final String F32 = "f32";
    public static final String F64 = "f64";
    public static final String TYPE = "type";

    final String module;
    private final String name;
    private final int sizeOf;
    private final boolean isSystem;
    private final boolean isArray;
    final boolean isFloatingPoint;
    private final DataType arrayType;
    private final DataType nullableType;
    public final List<Variable> fields;
    private DataType baseType;
    boolean autoClose;
    final boolean valueType;
    private boolean used;
    Expression maxValue;
    private boolean isNullable;
    public ArrayList<String> parameters;
    public String template;

    public DataType(String module, String name, int sizeOf, boolean isSystem, List<Variable> fields) {
        this(module, name, sizeOf, isSystem, false, fields, false);
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
        this.autoClose = isArray;
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
        } else if ("f64".equals(name)) {
            s = "double";
        } else if (name.startsWith("0..")) {
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
    
    public static String getId(String name, ArrayList<DataType> params) {
        StringBuilder buff = new StringBuilder();
        buff.append(name);
        for (DataType t : params) {
            buff.append('_');
            buff.append(t.name.replace("[]", "_array"));
        }
        return buff.toString();
    }
    
    public static String fullName(String module, String name) {
        if (module == null) {
            return name;
        }
        return module + "." + name;
    }

}
