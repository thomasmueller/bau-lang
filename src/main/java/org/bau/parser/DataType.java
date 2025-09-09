package org.bau.parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import org.bau.runtime.Value;
import org.bau.runtime.Value.ValueInt;
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

    // for owner and borrow: base type

    public List<Variable> fields = new ArrayList<>();

    private final DataType refCountBaseType;
    private DataType ownerType;
    private DataType borrowType;

    public LinkedHashMap<String, Long> enumValues;
    FunctionDefinition autoClose;
    private boolean used;
    Expression maxValue;
    public ArrayList<String> parameters;
    public String template;
    public int lineOffset;

    public boolean isFunctionPointer;
    public ArrayList<DataType> functionPointerArgs;
    public DataType functionPointerReturnType;
    public ArrayList<String> traits = new ArrayList<>();
    public ArrayList<DataType> traitTypes = new ArrayList<>();
    public Trait traitDefinition;
    public HashSet<DataType> implementingTypes = new HashSet<>();
    public int traitSlot = -1;

    public static boolean isGenericTypeName(String token) {
        return token != null && !token.isEmpty() &&
                token.charAt(0) >= 'A' && token.charAt(0) <= 'Z' &&
                token.toUpperCase().equals(token);
    }

    public static DataType newTraitType(String module, String name) {
        return new DataType(module, name, 0, false, null, null, false, MemoryType.REF_COUNT);
    }

    public static DataType newNumberType(String name, int sizeOf) {
        return new DataType(null, name, sizeOf, true, null, null, false, MemoryType.COPY);
    }

    public static DataType newBuiltIn(String name, int sizeOf) {
        return newNonArray(null, name, sizeOf, MemoryType.COPY);
    }

    public static DataType newEnumType(String module, String name) {
        return new DataType(module, name, 8, true, null, null, false, MemoryType.COPY);
    }

    public static DataType newEmptyType(String module, String name) {
        return newNonArray(module, name, 0, MemoryType.REF_COUNT);
    }

    public static DataType newRegularType(String module, String name, int sizeOf, MemoryType memoryType) {
        if (memoryType == MemoryType.BORROW || memoryType == MemoryType.OWNER) {
            throw new IllegalArgumentException();
        }
        return newNonArray(module, name, sizeOf, memoryType);
    }

    private static DataType newNonArray(String module, String name, int sizeOf, MemoryType memoryType) {
        if (memoryType == MemoryType.BORROW || memoryType == MemoryType.OWNER) {
            throw new IllegalArgumentException();
        }
        return new DataType(module, name, sizeOf, false, null, null, false, memoryType);
    }

    public static DataType newFunctionPointer(String module, ArrayList<DataType> params, DataType returnType) {
        DataType r = newNonArray(module, "fun", 0, MemoryType.COPY);
        r.isFunctionPointer = true;
        r.functionPointerArgs = params;
        r.functionPointerReturnType = returnType;
        return r;
    }

    public int hashCode() {
        return toString().hashCode();
    }

    public boolean equals(DataType other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        // function pointer types are not equal
        return this.toString().equals(other.toString());
    }

    public Expression nullExpression() {
        if (isNumber) {
            return new NumberValue(ValueInt.ZERO, this, false);
        } else if (!isPointer()) {
            return new New(this, null);
        } else {
            return new NullValue(this);
        }
    }

    private DataType(String module, String name, int sizeOf, boolean isNumber, DataType arrayBaseType, DataType refCountBaseType, boolean isNullable, MemoryType memoryType) {
        this.isNullable = isNullable;
        this.module = module;
        this.name = name;
        this.sizeOf = sizeOf;
        this.isNumber = isNumber;
        this.arrayBaseType = arrayBaseType;
        this.refCountBaseType = refCountBaseType;
        this.memoryType = memoryType;
        if (isNumber) {
            isFloatingPoint = name.charAt(0) == 'f';
        } else {
            isFloatingPoint = false;
        }
        if (!isArray() && memoryType != MemoryType.COPY && !isNullable) {
            nullableType = new DataType(module, name, sizeOf, false, null, refCountBaseType, true, memoryType);
            nullableType.fields = fields;
        } else {
            nullableType = null;
        }
        if (!isArray()) {
            arrayType = new DataType(module, name + "[]", sizeOf, false, this, null, isNullable, MemoryType.REF_COUNT);
        } else {
            arrayType = this;
        }
    }

    void addFields(List<Variable> fields) {
        this.fields.addAll(fields);
    }

    void used(Program program) {
        if (DataType.isGenericTypeName(name)) {
            throw new IllegalStateException();
        }
        this.used = true;
        if (!traits.isEmpty()) {
            if (traitTypes.isEmpty()) {
                for (String t : traits) {
                    int todo;
                    // traits can be in modules...
                    DataType tm = program.getType(null, t);
                    tm.implementingTypes.add(this);
                    traitTypes.add(tm);
                }
            }
        }
        if (isArray()) {
            arrayBaseType.used(program);
        }
        if (autoClose != null) {
            program.getFunctionById(autoClose.getFunctionId()).used(program);
        }
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
        if (isArray()) {
            n = arrayBaseType.id() + "_array";
        }
        return n;
    }

    public String getCamelCaseName() {
        String result = name;
        char firstChar = name.charAt(0);
        if (firstChar >= 'a' && firstChar <= 'z') {
            result = (char)(firstChar - 'a' + 'A') + result.substring(1);
        }
        if (result.endsWith("[]")) {
            result = result.substring(0, result.length() - 2) + "Array";
        }
        return result;
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

    public DataType refCountBaseType() {
        return refCountBaseType;
    }

    public String toString() {
        StringBuilder buff = new StringBuilder();
        if (traitDefinition != null) {
            buff.append(name);
            return buff.toString();
        }
        if (isFunctionPointer) {
            buff.append("fun(");
            for (int i = 0; i < functionPointerArgs.size(); i++) {
                if (i > 0) {
                    buff.append(", ");
                }
                buff.append(functionPointerArgs.get(i).toString());
            }
            buff.append(")");
            if (functionPointerReturnType != null) {
                buff.append(' ').append(functionPointerReturnType);
            }
            return buff.toString();
        }
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
            s = "int8_t";
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
            String n = name;
            if (module != null) {
                s = Program.esc(module).replace(".", "_") + "_" + n;
            } else {
                s = n;
            }
            if (isArray()) {
                // replace "[]" with "_array"
                s = s.substring(0, s.length() - 2) + "_array";
            }
        }
        if (memoryType == MemoryType.OWNER || memoryType == MemoryType.BORROW) {
            s += "_owned";
        }
        return s;
    }

    public String toC() {
        if (isFunctionPointer) {
            throw new IllegalStateException();
        }
        String s = nameC();
        return (isPointer() || isArray()) ? s + "*" : s;
    }

    public DataType getFieldDataType(String f) {
        // TODO use a map, not a loop
        for (Variable v : fields) {
            if (v.name.equals(f)) {
                return v.type();
            }
        }
        return null;
    }

    public boolean needFree() {
        if (isFunctionPointer) {
            return false;
        }
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

    public boolean isRange() {
        return maxValue != null;
    }

    public DataType ownerType() {
        if (memoryType == MemoryType.OWNER) {
            return this;
        } else if (memoryType == MemoryType.BORROW) {
            return refCountBaseType.ownerType();
        }
        if (ownerType == null) {
            ownerType = new DataType(module, name, sizeOf, false, null, this, false, MemoryType.OWNER);
            ownerType.fields = fields;
        }
        return ownerType;
    }

    public DataType borrowType() {
        if (memoryType == MemoryType.BORROW) {
            return this;
        } else if (memoryType == MemoryType.OWNER) {
            return refCountBaseType.borrowType();
        }
        if (borrowType == null) {
            borrowType = new DataType(module, name, sizeOf, false, null, this, false, MemoryType.BORROW);
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
