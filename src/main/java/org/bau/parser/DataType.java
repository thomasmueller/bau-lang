package org.bau.parser;

import java.util.ArrayList;
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
    public static final DataType INT_TYPE = newNumberType(DataType.INT, 8);
    static {
        INT_TYPE.used();
        INT_TYPE.arrayType.used();
    }

    final String module;
    private final String name;
    private final int sizeOf;

    // int, float, type, enum
    private final boolean isNumber;
    private final boolean isFloatingPoint;

    private final MemoryType memoryType;

    private final DataType nullableType;
    private final boolean isNullable;

    // for non-array: the array type
    private final DataType arrayType;

    // for array: the base type
    private final DataType arrayBaseType;

    public List<Variable> fields = new ArrayList<>();

    private DataType ownerType;
    private DataType borrowType;

    public LinkedHashMap<String, Long> enumValues;
    FunctionDefinition autoClose;
    private boolean used;
    Expression maxValue;
    public ArrayList<String> parameters;
    public String template;

    public static boolean isGenericTypeName(String token) {
        return token != null && !token.isEmpty() &&
                token.charAt(0) >= 'A' && token.charAt(0) <= 'Z' &&
                token.toUpperCase().equals(token);
    }

    public static DataType newNumberType(String name, int sizeOf) {
        return new DataType(null, name, sizeOf, true, null, false, MemoryType.COPY);
    }

    public static DataType newBuiltIn(String name, int sizeOf) {
        return newNonArray(null, name, sizeOf, MemoryType.COPY);
    }

    public static DataType newEnumType(String module, String name) {
        return new DataType(null, name, 8, true, null, false, MemoryType.COPY);
    }

    public static DataType newEmptyType(String module, String name) {
        return newNonArray(module, name, 0,MemoryType.REF_COUNT);
    }

    public static DataType newRegularType(String module, String name, int sizeOf, MemoryType memoryType) {
        return newNonArray(module, name, sizeOf, memoryType);
    }

    private static DataType newNonArray(String module, String name, int sizeOf, MemoryType memoryType) {
        return new DataType(module, name, sizeOf, false, null, false, memoryType);
    }

    private DataType(String module, String name, int sizeOf, boolean isNumber, DataType arrayBaseType, boolean isNullable, MemoryType memoryType) {
        this.isNullable = isNullable;
        this.module = module;
        this.name = name;
        this.sizeOf = sizeOf;
        this.isNumber = isNumber;
        this.arrayBaseType = arrayBaseType;
        this.memoryType = memoryType;
        if (isNumber) {
            isFloatingPoint = name.charAt(0) == 'f';
        } else {
            isFloatingPoint = false;
        }
        if (!isArray() && memoryType != MemoryType.COPY && !isNullable) {
            nullableType = new DataType(module, name, sizeOf, false, null, true, memoryType);
            nullableType.fields = fields;
        } else {
            nullableType = null;
        }
        if (!isArray()) {
            arrayType = new DataType(module, name + "[]", sizeOf, false, this, isNullable, MemoryType.REF_COUNT);
        } else {
            arrayType = this;
        }
    }

    void addFields(List<Variable> fields) {
        this.fields.addAll(fields);
    }

    void used() {
        if (DataType.isGenericTypeName(name)) {
            return;
        }
        if (fields.isEmpty() && !isArray() && isPointer()) {
            // eg. List(T)
            return;
        }
        this.used = true;
    }

    public boolean isNumber() {
        return isNumber;
    }

    public boolean isFloatingPoint() {
        return isFloatingPoint;
    }

    public boolean isCopyType() {
        return memoryType == MemoryType.COPY;
    }

    public String fullName() {
        return fullName(module, id());
    }

    public String id() {
        String n = name;
        if (memoryType == MemoryType.OWNER || memoryType == MemoryType.BORROW) {
            n += "+";
        }
        return n;
    }

    public String idC() {
        String n = name;
        if (memoryType == MemoryType.OWNER || memoryType == MemoryType.BORROW) {
            n += "_owned";
        }
        return n;
    }

    public String name() {
        return name;
    }

    public int sizeOf() {
        return sizeOf;
    }

    public DataType baseType() {
        if (!isArray()) {
            throw new IllegalStateException();
        }
        return arrayBaseType;
    }

    public DataType arrayType() {
        if (isArray()) {
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
        if (memoryType == MemoryType.OWNER || memoryType == MemoryType.BORROW) {
            s += "_owned";
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

    public boolean needFree() {
        return !isNumber;
    }

    public boolean needIncDec() {
        return isPointer() | isArray();
    }

    public boolean isPointer() {
        return memoryType != MemoryType.COPY;
    }

    public boolean isArray() {
        return arrayBaseType != null;
    }

    public boolean isUsed() {
        return used;
    }

    public DataType orNull() {
        return nullableType;
    }

    public MemoryType memoryType() {
        return memoryType;
    }

    public DataType ownerType() {
        if (ownerType == null) {
            ownerType = new DataType(module, name, sizeOf, false, null, false, MemoryType.OWNER);
            ownerType.fields = fields;
        }
        return ownerType;
    }

    public DataType borrowType() {
        if (borrowType == null) {
            borrowType = new DataType(module, name, sizeOf, false, null, false, MemoryType.BORROW);
            borrowType.fields = fields;
        }
        return borrowType;
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
